package xyz.wagyourtail.jvmdg.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.config.ManifestConfigReader.ConfigException;
import xyz.wagyourtail.jvmdg.config.ManifestConfigReader.ConfigFile;
import xyz.wagyourtail.jvmdg.config.ManifestConfigReader.Section;

/**
 * Applies parsed {@code jvmdg.mf} content to {@link Flags} through
 * {@link FlagBindings} — the layer that enforces the config file's own rules:
 * the {@code Jvmdg-Config-Version} header, unknown-key rejection, main-only
 * keys leaking into named sections, and the embedded-jar sandbox.
 *
 * <p>Application order is the caller's job: defaults &lt; embedded rules &lt;
 * config files in the given order &lt; sysprops (skipped mechanically when a
 * binding's sysprop is set) &lt; explicit CLI/Gradle values applied after.
 */
public final class JvmdgConfig {

    /** Required format marker — value must be {@code 1}. */
    public static final String VERSION_KEY = "Jvmdg-Config-Version";
    /** Resource prefix embedded rule files live under in input jars. */
    public static final String EMBEDDED_PREFIX = "META-INF/jvmdg/";

    private JvmdgConfig() {
    }

    /**
     * Apply a user config file: main-section keys write bound fields,
     * {@code Name:} sections become unconstrained {@link ScopedRule}s.
     * Unknown keys and global keys in named sections are hard errors — a typo
     * in the user's own file must fail fast, not silently no-op.
     */
    public static void apply(ConfigFile file, Flags flags,
                             Map<String, FlagBindings.Source> sources) throws IOException {
        checkVersion(file);

        for (Map.Entry<String, String> e : file.main.attributes.entrySet()) {
            if (ManifestConfigReader.Section.canonical(VERSION_KEY).equals(e.getKey())) continue;
            FlagBindings.Binding b = FlagBindings.byKey(e.getKey());
            if (b == null) {
                throw new ConfigException(file.source, file.main.line,
                    "unknown config key '" + file.main.keySpelling(e.getKey()) + "'");
            }
            FlagBindings.applyConfig(b, flags, e.getValue(), sources, FlagBindings.Source.CONFIG);
        }

        for (Section s : file.named) {
            for (String k : s.attributes.keySet()) {
                validateScopedKey(k, file.source, s);
            }
            flags.scopedRules.add(new ScopedRule(
                stripWildcard(s.name), wildcardOf(s.name), file.source, null, s.line,
                new LinkedHashMap<>(s.attributes)));
        }
    }

    /**
     * Apply an embedded {@code META-INF/jvmdg/*.mf} file from an input jar —
     * the whole file is sandboxed: its main section becomes a rule scoped to
     * the jar's own packages (per {@code jarScope}, the set of package dirs
     * the jar contains), and named sections are intersected with the same
     * set. Global-only and unknown keys are dropped with a warning rather
     * than failing the build — a library's rules must not brick consumers,
     * including keys a newer jvmdg may have introduced. Returns nothing
     * through flag fields; embedded rules only ever contribute scoped rules.
     */
    public static void applyEmbedded(ConfigFile file, Flags flags, java.util.Set<String> jarScope,
                                     xyz.wagyourtail.jvmdg.logging.Logger logger) throws IOException {
        checkVersion(file);
        Map<String, String> attrs = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : file.main.attributes.entrySet()) {
            if (ManifestConfigReader.Section.canonical(VERSION_KEY).equals(e.getKey())) continue;
            if (scopedKey(e.getKey())) {
                attrs.put(e.getKey(), e.getValue());
            } else {
                warnEmbedded(logger, file, file.main.line, e.getKey());
            }
        }
        // the jar's packages constrain even the "main" section — an embedded
        // file can only ever steer classes that came from its own jar
        flags.scopedRules.add(new ScopedRule("", Flags.WildcardType.DOUBLE, file.source, jarScope, 1, attrs));
        for (Section s : file.named) {
            Map<String, String> kept = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : s.attributes.entrySet()) {
                if (scopedKey(e.getKey())) {
                    kept.put(e.getKey(), e.getValue());
                } else {
                    warnEmbedded(logger, file, s.line, e.getKey());
                }
            }
            flags.scopedRules.add(new ScopedRule(
                stripWildcard(s.name), wildcardOf(s.name), file.source, jarScope, s.line, kept));
        }
    }

    /** Parse + apply config files in order — later files win for scalar keys. */
    public static void applyFiles(List<Path> files, Flags flags, Map<String, FlagBindings.Source> sources) throws IOException {
        for (Path file : files) {
            apply(ManifestConfigReader.read(file), flags, sources);
        }
    }

    private static void checkVersion(ConfigFile file) throws ConfigException {
        String ver = file.main.get(VERSION_KEY);
        if (ver == null) {
            throw new ConfigException(file.source, 1, "missing required '" + VERSION_KEY + ": 1' header");
        }
        if (!"1".equals(ver.trim())) {
            throw new ConfigException(file.source, 1, "unsupported " + VERSION_KEY + " '" + ver + "' (expected 1)");
        }
    }

    /** Whether {@code key} may appear in a scoped section. */
    private static boolean scopedKey(String key) {
        if ("ignore-warnings".equals(key)) return true;
        FlagBindings.Binding b = FlagBindings.byKey(key);
        return b != null && b.primary().scoped();
    }

    /**
     * The scoped-section keys honored today. {@code Stub-Policy},
     * {@code On-Missing-Api}, {@code Keep-Reflection}, and
     * {@code Include-Stubs} arrive with the stubPolicy phase — they are
     * unknown keys until their semantics exist (typo protection beats a
     * silently-ignored reserved word).
     */
    private static void validateScopedKey(String key, String source, Section s) throws ConfigException {
        if ("ignore-warnings".equals(key)) return;
        FlagBindings.Binding b = FlagBindings.byKey(key);
        if (b == null) {
            throw new ConfigException(source, s.line,
                "unknown config key '" + s.keySpelling(key) + "' in section '" + s.name + "'");
        }
        if (!b.primary().scoped()) {
            throw new ConfigException(source, s.line,
                "'" + s.keySpelling(key) + "' is a global-only key — not valid in scoped section '" + s.name + "'");
        }
    }

    private static void warnEmbedded(xyz.wagyourtail.jvmdg.logging.Logger logger,
                                     ConfigFile file, int line, String key) {
        String msg = file.source + ":" + line + ": embedded rule key '" + key + "' is not a scoped key — ignored";
        if (logger != null) {
            logger.warn(msg);
        } else {
            System.err.println("[jvmdg] WARN " + msg);
        }
    }

    /** Strip the wildcard suffix off a {@code Name:} pattern (addIgnore rules). */
    static Flags.WildcardType wildcardOf(String pattern) {
        if (pattern.endsWith("**")) return Flags.WildcardType.DOUBLE;
        if (pattern.endsWith("*")) return Flags.WildcardType.SINGLE;
        return Flags.WildcardType.NONE;
    }

    /** The pattern with its wildcard suffix removed — the stored prefix. */
    static String stripWildcard(String pattern) {
        if (pattern.endsWith("**")) return pattern.substring(0, pattern.length() - 2);
        if (pattern.endsWith("*")) return pattern.substring(0, pattern.length() - 1);
        return pattern;
    }
}
