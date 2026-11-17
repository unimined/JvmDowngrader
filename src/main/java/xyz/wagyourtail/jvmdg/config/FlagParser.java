package xyz.wagyourtail.jvmdg.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

/**
 * Parses raw option text into field values and merges contributions into a
 * field's existing value. One parser instance per {@link Flags} field type —
 * the parse+merge pair is what lets sysprop init, CLI application, and
 * config-file application share one code path per field.
 */
public abstract class FlagParser {

    /** Parse one scalar token into a field-typed value (or element for collections). */
    public abstract Object parseScalar(String raw);

    /**
     * Parse a token that may carry several collection elements (list fields
     * split on their native separator). Scalars delegate to
     * {@link #parseScalar}.
     */
    public Object parseMulti(String raw) {
        return parseScalar(raw);
    }

    /**
     * Merge a parsed contribution into the running value. {@code current} is
     * null when this contribution starts a fresh value (non-accumulating
     * application — config file wins wholesale).
     */
    public Object merge(Object current, Object parsed) {
        return parsed;
    }

    /** Seed value for a fresh accumulation (empty collection for list fields). */
    public Object fresh() {
        return null;
    }

    // ---- scalar parsers ----

    public static final FlagParser BOOLEAN = new FlagParser() {
        @Override
        public Object parseScalar(String raw) {
            if ("true".equalsIgnoreCase(raw)) return Boolean.TRUE;
            if ("false".equalsIgnoreCase(raw)) return Boolean.FALSE;
            throw new IllegalArgumentException("expected true|false, got '" + raw + "'");
        }
    };

    public static final FlagParser INT = new FlagParser() {
        @Override
        public Object parseScalar(String raw) {
            return Integer.parseInt(raw.trim());
        }
    };

    /** Java major version ({@code 8}) → class-file version ({@code 52}). */
    public static final FlagParser JAVA_VERSION = new FlagParser() {
        @Override
        public Object parseScalar(String raw) {
            return Utils.majorVersionToClassVersion(Integer.parseInt(raw.trim()));
        }
    };

    public static final FlagParser STRING = new FlagParser() {
        @Override
        public Object parseScalar(String raw) {
            return raw;
        }
    };

    public static final FlagParser LOG_LEVEL = new FlagParser() {
        @Override
        public Object parseScalar(String raw) {
            return Logger.Level.valueOf(raw.trim().toUpperCase());
        }
    };

    /** {@link StubPolicy} — {@code full|noBehavior|mechanical} (case-insensitive). */
    public static final FlagParser STUB_POLICY = new FlagParser() {
        @Override
        public Object parseScalar(String raw) {
            for (StubPolicy p : StubPolicy.values()) {
                if (p.name().replace("_", "").equalsIgnoreCase(raw.trim().replace("_", ""))) {
                    return p;
                }
            }
            throw new IllegalArgumentException("expected full|noBehavior|mechanical, got '" + raw + "'");
        }
    };

    /** {@link OnMissingApi} — {@code error|warn} (case-insensitive). */
    public static final FlagParser ON_MISSING_API = new FlagParser() {
        @Override
        public Object parseScalar(String raw) {
            return OnMissingApi.valueOf(raw.trim().toUpperCase());
        }
    };

    /** {@link ShadeReflection} — {@code none|usedOwners|all} (case-insensitive). */
    public static final FlagParser SHADE_REFLECTION = new FlagParser() {
        @Override
        public Object parseScalar(String raw) {
            for (ShadeReflection s : ShadeReflection.values()) {
                if (s.name().replace("_", "").equalsIgnoreCase(raw.trim().replace("_", ""))) {
                    return s;
                }
            }
            throw new IllegalArgumentException("expected none|usedOwners|all, got '" + raw + "'");
        }
    };

    // ---- collection parsers ----

    /** Base for {@code Set}-typed fields: elements add into a LinkedHashSet. */
    private static abstract class SetParser extends FlagParser {
        @Override
        public Object parseMulti(String raw) {
            List<Object> out = new ArrayList<>();
            for (String token : raw.split("[\\s,|]+")) {
                if (!token.isEmpty()) {
                    out.add(parseScalar(token));
                }
            }
            return out;
        }

        @Override
        public Object merge(Object current, Object parsed) {
            LinkedHashSet<Object> set = new LinkedHashSet<>();
            if (current instanceof Collection) {
                //noinspection unchecked
                set.addAll((Collection<Object>) current);
            }
            //noinspection unchecked
            set.addAll((Collection<Object>) parsed);
            return set;
        }

        @Override
        public Object fresh() {
            return new LinkedHashSet<>();
        }
    }

    /** Class-version ints (e.g. {@code debugSkipStubs}, multiReleaseVersions). */
    public static final FlagParser CLASS_VERSION_SET = new SetParser() {
        @Override
        public Object parseScalar(String raw) {
            return Integer.parseInt(raw.trim());
        }
    };

    /** Java-major versions in config files → class-version set. */
    public static final FlagParser JAVA_VERSION_SET = new SetParser() {
        @Override
        public Object parseScalar(String raw) {
            return Utils.majorVersionToClassVersion(Integer.parseInt(raw.trim()));
        }
    };

    /** JEP numbers — plain ints. */
    public static final FlagParser JEP_SET = CLASS_VERSION_SET;

    public static final FlagParser FQMND_SET = new SetParser() {
        @Override
        public Object parseScalar(String raw) {
            return FullyQualifiedMemberNameAndDesc.of(raw);
        }
    };

    /** {@code List<File>} — path-separator separated. */
    public static final FlagParser FILE_LIST = new FlagParser() {
        @Override
        public Object parseScalar(String raw) {
            return new File(raw);
        }

        @Override
        public Object parseMulti(String raw) {
            List<Object> out = new ArrayList<>();
            for (String token : raw.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                if (!token.isEmpty()) {
                    out.add(new File(token));
                }
            }
            return out;
        }

        @Override
        public Object merge(Object current, Object parsed) {
            List<Object> list = current == null
                ? new ArrayList<>() : (List<Object>) current;
            //noinspection unchecked
            list.addAll((Collection<Object>) parsed);
            return list;
        }

        @Override
        public Object fresh() {
            return new ArrayList<>();
        }
    };

    /**
     * {@code TreeMap<String, WildcardType>} for {@code ignoreWarningsIn} —
     * tokens are {@code path}, {@code path/*}, or {@code path/**}.
     */
    public static final FlagParser WILDCARD_MAP = new FlagParser() {
        @Override
        public Object parseScalar(String raw) {
            return raw;
        }

        @Override
        public Object parseMulti(String raw) {
            Map<String, Flags.WildcardType> map = new TreeMap<>();
            for (String token : raw.split("[\\s,]+")) {
                if (token.isEmpty()) continue;
                if (token.endsWith("**")) {
                    map.put(token.substring(0, token.length() - 2), Flags.WildcardType.DOUBLE);
                } else if (token.endsWith("*")) {
                    map.put(token.substring(0, token.length() - 1), Flags.WildcardType.SINGLE);
                } else {
                    map.put(token, Flags.WildcardType.NONE);
                }
            }
            return map;
        }

        @Override
        public Object merge(Object current, Object parsed) {
            TreeMap<String, Flags.WildcardType> map = current == null
                ? new TreeMap<>() : (TreeMap<String, Flags.WildcardType>) current;
            //noinspection unchecked
            map.putAll((Map<String, Flags.WildcardType>) parsed);
            return map;
        }

        @Override
        public Object fresh() {
            return new TreeMap<>();
        }
    };

    private FlagParser() {
    }
}
