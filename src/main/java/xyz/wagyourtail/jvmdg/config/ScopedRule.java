package xyz.wagyourtail.jvmdg.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import xyz.wagyourtail.jvmdg.cli.Flags;

/**
 * A {@code Name:}-scoped section of a config file: the class-name pattern plus
 * the raw attributes declared under it. Resolution (which rule applies to a
 * given class) is order-independent — the most specific matching pattern
 * wins — so this is stored verbatim and interpreted at use sites.
 */
public final class ScopedRule {

    /** The {@code Name:} pattern, wildcard suffix stripped (as in {@code ignoreWarningsIn}). */
    public final String pattern;

    /** Wildcard strength of {@link #pattern}. */
    public final Flags.WildcardType wildcard;

    /** Where this rule came from — a config-file path or {@code jar:entry}. */
    public final String source;

    /**
     * When the rule came embedded in an input jar, the set of package dirs
     * the jar actually contains ({@code ""} = default package). The rule can
     * only fire for classes in one of those packages — a library's embedded
     * rules are sandboxed to its own code. {@code null} for user config
     * files, which are unconstrained.
     */
    public final Set<String> jarScope;

    /** 1-based line of the {@code Name:} header in {@link #source}. */
    public final int line;

    /** Canonical (lower-case) manifest keys → raw values, file order. */
    public final Map<String, String> attributes;

    public ScopedRule(String pattern, Flags.WildcardType wildcard, String source,
                      Set<String> jarScope, int line, Map<String, String> attributes) {
        this.pattern = pattern;
        this.wildcard = wildcard;
        this.source = source;
        this.jarScope = jarScope == null ? null : Collections.unmodifiableSet(new java.util.LinkedHashSet<>(jarScope));
        this.line = line;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    /**
     * Specificity ordering for most-specific-wins resolution: longer literal
     * prefix, then narrower wildcard ({@code NONE} &gt; {@code SINGLE} &gt;
     * {@code DOUBLE}). Two different patterns can share a specificity — that
     * is the equal-specificity conflict the config layer must reject.
     */
    public int specificity() {
        return pattern.length() * 4 - wildcard.ordinal();
    }

    /**
     * Whether {@code className} (internal form) falls under this rule's
     * pattern — same semantics as {@code Flags.checkInIgnoreWarnings}: the
     * literal prefix must match first, then {@code SINGLE} confines to one
     * package level and {@code DOUBLE} covers everything below.
     */
    public boolean matches(String className) {
        if (className.isEmpty() || !className.startsWith(pattern)) return false;
        if (jarScope != null &&
            !jarScope.contains(className.substring(0, className.lastIndexOf('/') + 1))) {
            return false;
        }
        switch (wildcard) {
            case NONE:
                return className.equals(pattern);
            case SINGLE:
                int lastSlash = className.lastIndexOf('/');
                if (lastSlash == -1) return true;
                String parent = className.substring(0, lastSlash + 1);
                String value = pattern.endsWith("/") ? pattern : pattern.substring(0, pattern.lastIndexOf('/') + 1);
                return parent.equals(value);
            case DOUBLE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Order-independent scope resolution: the most specific matching pattern
     * wins. Two rules matching the same class with equal specificity are a
     * config error — silently picking one would make file order semantically
     * load-bearing.
     */
    /** {@code Ignore-Warnings: true} suppresses transform warnings for classes in scope. */
    public boolean ignoreWarnings() {
        return Boolean.parseBoolean(attributes.get("Ignore-Warnings".toLowerCase(java.util.Locale.ROOT)));
    }

    /**
     * Members of {@code Exclude-Stubs} — callsite references in scope keep their
     * native form (the stub is not emitted for the class). Parsed lazily; an
     * invalid member ref surfaces as a {@link ManifestConfigReader.ConfigException}
     * pointing at the declaring rule.
     */
    public java.util.Set<xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc> excludeStubs()
        throws ManifestConfigReader.ConfigException {
        return parseSet("exclude-stubs", FlagParser.FQMND_SET);
    }

    /**
     * JEP ids of {@code Exclude-Jeps} — stub entries whose {@code @JEP} set
     * intersects are not emitted for classes in scope.
     */
    public java.util.Set<Integer> excludeJeps() throws ManifestConfigReader.ConfigException {
        return parseSet("exclude-jeps", FlagParser.JEP_SET);
    }

    /**
     * Members of {@code Include-Stubs} — re-included for classes in scope even
     * when {@code Exclude-Stubs}/{@code Exclude-Jeps} or {@code Stub-Policy}
     * would drop them. An explicit member-level statement is the most specific
     * one possible, so it wins over every veto.
     */
    public java.util.Set<xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc> includeStubs()
        throws ManifestConfigReader.ConfigException {
        return parseSet("include-stubs", FlagParser.FQMND_SET);
    }

    /** {@code Stub-Policy} for classes in scope — null defers to the global flag. */
    public StubPolicy stubPolicy() throws ManifestConfigReader.ConfigException {
        return (StubPolicy) parseScalar("stub-policy", FlagParser.STUB_POLICY);
    }

    /** {@code On-Missing-Api} for classes in scope — null defers to the global flag. */
    public OnMissingApi onMissingApi() throws ManifestConfigReader.ConfigException {
        return (OnMissingApi) parseScalar("on-missing-api", FlagParser.ON_MISSING_API);
    }

    private Object parseScalar(String key, FlagParser parser) throws ManifestConfigReader.ConfigException {
        String raw = attributes.get(key);
        if (raw == null) {
            return null;
        }
        try {
            return parser.parseScalar(raw);
        } catch (RuntimeException e) {
            throw new ManifestConfigReader.ConfigException(source, line,
                "invalid '" + key + "' value: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> java.util.Set<T> parseSet(String key, FlagParser parser) throws ManifestConfigReader.ConfigException {
        String raw = attributes.get(key);
        if (raw == null) {
            return java.util.Collections.emptySet();
        }
        try {
            Object parsed = parser.parseMulti(raw);
            return parsed instanceof java.util.Set
                ? (java.util.Set<T>) parsed
                : new java.util.LinkedHashSet<>((java.util.Collection<T>) parsed);
        } catch (RuntimeException e) {
            throw new ManifestConfigReader.ConfigException(source, line,
                "invalid '" + key + "' value: " + e.getMessage());
        }
    }

    public static ScopedRule resolve(Iterable<ScopedRule> rules, String className)
        throws ManifestConfigReader.ConfigException {
        ScopedRule best = null;
        for (ScopedRule r : rules) {
            if (!r.matches(className)) continue;
            if (best == null || r.specificity() > best.specificity()) {
                best = r;
            } else if (r.specificity() == best.specificity() && r != best) {
                throw new ManifestConfigReader.ConfigException(r.source, r.line,
                    "equally specific scopes '" + best.pattern + "' (" + best.source + ":" + best.line +
                        ") and '" + r.pattern + "' both match " + className);
            }
        }
        return best;
    }

    @Override
    public String toString() {
        return "ScopedRule{" + pattern + "/" + wildcard + " <- " + source + ":" + line + "}";
    }
}
