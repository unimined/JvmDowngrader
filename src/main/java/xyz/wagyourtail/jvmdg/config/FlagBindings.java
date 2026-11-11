package xyz.wagyourtail.jvmdg.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import xyz.wagyourtail.jvmdg.cli.Flags;

/**
 * The single index of {@link Flags}'s option surface, built by reflecting over
 * {@link BoundFlag}-annotated fields. Everything that used to be hand-mirrored
 * per flag — sysprop initialization, CLI flag declaration/application, config
 * keys, {@code copy}/{@code equals}/{@code hashCode}/{@code toString}, the
 * {@code --print} dump — is driven from here, so adding a field is annotating
 * it, nothing else.
 *
 * <p>Application precedence (low → high): field default &lt; embedded rules
 * &lt; config file(s) &lt; system property &lt; explicit CLI / Gradle DSL. The
 * sysprop rule is mechanical: a binding whose {@link BoundFlag#sysprop()} is
 * set in the JVM skips config application entirely.
 */
public final class FlagBindings {

    /** Option-source tag recorded for {@code --print} output. */
    public enum Source {
        DEFAULT, EMBEDDED, CONFIG, SYSPROP, CLI, GRADLE
    }

    /** One {@link Flags} field's bindings (primary + constant-flag extras). */
    public static final class Binding {
        public final Field field;
        public final BoundFlag[] metas;
        private BoundFlag primary;

        Binding(Field field, BoundFlag[] metas) {
            this.field = field;
            this.metas = metas;
        }

        /** The value binding — the meta carrying the manifest key. */
        public BoundFlag primary() {
            if (primary == null) {
                for (BoundFlag m : metas) {
                    if (!m.key().isEmpty()) {
                        primary = m;
                        return primary;
                    }
                }
                primary = metas[0];
            }
            return primary;
        }

        /** Field has no option surface at all (pure internal state). */
        public boolean internal() {
            for (BoundFlag m : metas) {
                if (!m.key().isEmpty() || !m.cli().isEmpty() || !m.sysprop().isEmpty() || !m.gradle().isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        public String key() {
            return primary().key();
        }

        /** Parser for config-file values ({@link BoundFlag#form()}). */
        public FlagParser parser() {
            return parser(primary().form());
        }

        /** Parser for sysprop/CLI values ({@link BoundFlag#legacyForm()}) — legacy forms differ for version fields. */
        public FlagParser legacyParser() {
            BoundFlag m = primary();
            return parser(m.legacyForm() == BoundFlag.Parser.AUTO ? m.form() : m.legacyForm());
        }

        private FlagParser parser(BoundFlag.Parser p) {
            return p == BoundFlag.Parser.AUTO ? autoParser(field.getType()) : p.instance;
        }

        private static FlagParser autoParser(Class<?> type) {
            if (type == boolean.class) return FlagParser.BOOLEAN;
            if (type == int.class) return FlagParser.INT;
            if (type == String.class) return FlagParser.STRING;
            if (type.isEnum()) return FlagParser.LOG_LEVEL;
            if (java.util.Set.class.isAssignableFrom(type) || java.util.List.class.isAssignableFrom(type)) {
                throw new IllegalStateException("collection-typed flag field '" +
                    "' requires an explicit form= — element type is ambiguous");
            }
            if (Map.class.isAssignableFrom(type)) return FlagParser.WILDCARD_MAP;
            throw new IllegalStateException("no default parser for flag field type " + type);
        }

        /** The {@link BoundFlag} declaring {@code cli() == name} (or its alias). */
        public BoundFlag metaForCli(String name) {
            String lower = name.toLowerCase(Locale.ROOT);
            for (BoundFlag m : metas) {
                if (m.cli().toLowerCase(Locale.ROOT).equals(lower)) return m;
                for (String alias : m.aliases()) {
                    if (alias.equals(lower)) return m;
                }
            }
            return metas[0];
        }

        /** Apply one raw value. {@code legacy} selects the sysprop/CLI parser. */
        public void apply(Flags flags, String raw, boolean legacy, boolean accumulate) {
            try {
                FlagParser p = legacy ? legacyParser() : parser();
                Object cur = accumulate ? field.get(flags) : p.fresh();
                field.set(flags, p.merge(cur, p.parseMulti(raw)));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** All bound fields, sorted by manifest key for deterministic output. */
    public static final List<Binding> ALL;

    private static final Map<String, Binding> BY_KEY = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final Map<String, Binding> BY_CLI = new LinkedHashMap<>();

    static {
        List<Binding> all = new ArrayList<>();
        for (Field f : Flags.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            BoundFlag[] metas = f.getAnnotationsByType(BoundFlag.class);
            if (metas.length == 0) continue;
            all.add(new Binding(f, metas));
        }
        all.sort(Comparator.comparing(b -> b.key().isEmpty() ? b.field.getName() : b.key(), String.CASE_INSENSITIVE_ORDER));
        ALL = Collections.unmodifiableList(all);
        for (Binding b : ALL) {
            for (BoundFlag m : b.metas) {
                if (!m.key().isEmpty()) {
                    if (BY_KEY.put(m.key(), b) != null) {
                        throw new IllegalStateException("duplicate manifest key " + m.key());
                    }
                }
                if (!m.cli().isEmpty()) {
                    BY_CLI.put(m.cli(), b);
                    for (String alias : m.aliases()) {
                        BY_CLI.put(alias, b);
                    }
                }
            }
        }
        // every public instance field must be annotated — this is the
        // completeness gate that makes hand-mirroring impossible
        java.util.Set<String> boundNames = new java.util.HashSet<>();
        for (Binding b : ALL) {
            boundNames.add(b.field.getName());
        }
        for (Field f : Flags.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (!boundNames.contains(f.getName())) {
                throw new IllegalStateException(
                    "Flags." + f.getName() + " is missing @BoundFlag — every option field must be bound");
            }
        }
    }

    private FlagBindings() {
    }

    public static Binding byKey(String manifestKey) {
        return BY_KEY.get(manifestKey);
    }

    public static Binding byCliFlag(String flag) {
        return BY_CLI.get(flag);
    }

    public static List<Binding> cliBindings() {
        List<Binding> out = new ArrayList<>();
        for (Binding b : ALL) {
            if (!b.internal()) out.add(b);
        }
        return out;
    }

    /** Initialize all fields from their declared system properties. */
    public static void applySysprops(Flags flags) {
        applySysprops(flags, null);
    }

    public static void applySysprops(Flags flags, Map<String, Source> sources) {
        for (Binding b : ALL) {
            for (BoundFlag m : b.metas) {
                if (m.sysprop().isEmpty()) continue;
                String raw = System.getProperty(m.sysprop());
                if (raw == null) continue;
                b.apply(flags, raw, true, true);
                if (sources != null) sources.put(b.field.getName(), Source.SYSPROP);
            }
        }
    }

    /** True when any of the binding's sysprops is set — config must not override it. */
    public static boolean syspropSet(Binding b) {
        for (BoundFlag m : b.metas) {
            if (!m.sysprop().isEmpty() && System.getProperty(m.sysprop()) != null) {
                return true;
            }
        }
        return false;
    }

    /** Apply a config-file value (full replace unless the field accumulates). */
    public static void applyConfig(Binding b, Flags flags, String raw, Map<String, Source> sources, Source src) {
        if (syspropSet(b)) return;
        b.apply(flags, raw, false, false);
        if (sources != null) sources.put(b.field.getName(), src);
    }

    /** Apply CLI occurrences of a value flag (each occurrence's tokens). */
    public static void applyCli(Binding b, BoundFlag m, Flags flags, List<String[]> occurrences, Map<String, Source> sources) {
        for (String[] occ : occurrences) {
            if (m.cliConst().isEmpty()) {
                for (String token : occ) {
                    b.apply(flags, token, true, true);
                }
            } else {
                b.apply(flags, m.cliConst(), true, true);
            }
        }
        if (sources != null) sources.put(b.field.getName(), Source.CLI);
    }

    /** Copy every bound field — generated, so a new field can't be dropped. */
    public static void copyInto(Flags from, Flags to) {
        for (Binding b : ALL) {
            try {
                b.field.set(to, b.field.get(from));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        deepCopyInto(from, to);
    }

    /**
     * Collection fields are shared by {@link #copyInto}'s shallow set — callers
     * mutating the copy's collections would corrupt the original, so clone the
     * known mutable field types.
     */
    private static void deepCopyInto(Flags from, Flags to) {
        for (Binding b : ALL) {
            try {
                Object v = b.field.get(from);
                if (v instanceof java.util.List) {
                    b.field.set(to, new ArrayList<>((java.util.List<?>) v));
                } else if (v instanceof java.util.Set) {
                    b.field.set(to, new java.util.LinkedHashSet<>((java.util.Set<?>) v));
                } else if (v instanceof TreeMap) {
                    b.field.set(to, new TreeMap<>((TreeMap<?, ?>) v));
                } else if (v instanceof Map) {
                    b.field.set(to, new LinkedHashMap<>((Map<?, ?>) v));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static boolean flagsEqual(Flags a, Flags b) {
        for (Binding bb : ALL) {
            try {
                if (!Objects.equals(bb.field.get(a), bb.field.get(b))) return false;
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return true;
    }

    public static int flagsHash(Flags a) {
        try {
            List<Object> vals = new ArrayList<>(ALL.size());
            for (Binding b : ALL) {
                vals.add(b.field.get(a));
            }
            return Objects.hash(vals.toArray());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String describe(Flags a) {
        StringBuilder sb = new StringBuilder("Flags{");
        boolean first = true;
        for (Binding b : ALL) {
            if (!first) sb.append(", ");
            first = false;
            try {
                sb.append(b.field.getName()).append('=').append(b.field.get(a));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return sb.append('}').toString();
    }

    /**
     * Effective-config dump for {@code --print} — one {@code Key = value
     * (source)} line per non-internal binding.
     */
    public static String printConfig(Flags flags, Map<String, Source> sources) {
        StringBuilder sb = new StringBuilder();
        for (Binding b : ALL) {
            if (b.internal()) continue;
            String key = b.key().isEmpty() ? "(cli-only) " + b.metas[0].cli() : b.key();
            Source src = sources == null ? Source.DEFAULT : sources.getOrDefault(b.field.getName(), Source.DEFAULT);
            try {
                sb.append(String.format(Locale.ROOT, "%-24s = %s (%s)%n", key, b.field.get(flags), src.name().toLowerCase(Locale.ROOT)));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        if (!flags.scopedRules.isEmpty()) {
            sb.append("\nScoped rules (most specific match wins):\n");
            for (ScopedRule r : flags.scopedRules) {
                sb.append(String.format(Locale.ROOT, "  Name: %s%s%s%n",
                    r.pattern, wildcardSuffix(r.wildcard),
                    r.jarScope == null ? "" : "  [jar-scoped]")).append("    ")
                    .append(r.attributes).append("  <- ").append(r.source).append(':').append(r.line).append('\n');
            }
        }
        return sb.toString();
    }

    private static String wildcardSuffix(Flags.WildcardType w) {
        switch (w) {
            case SINGLE: return "*";
            case DOUBLE: return "**";
            default: return "";
        }
    }
}
