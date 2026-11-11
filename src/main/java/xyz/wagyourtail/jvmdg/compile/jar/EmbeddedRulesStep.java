package xyz.wagyourtail.jvmdg.compile.jar;

import xyz.wagyourtail.jvmdg.compile.shade.IndexScanner;
import xyz.wagyourtail.jvmdg.config.JvmdgConfig;
import xyz.wagyourtail.jvmdg.config.ManifestConfigReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads {@code META-INF/jvmdg/*.mf} rule files embedded in an input jar and
 * applies them sandboxed to that jar's own packages
 * ({@link JvmdgConfig#applyEmbedded}). Runs first in the step chain —
 * {@link #prepareFused} fires before the entry walk, so the scoped rules
 * exist before any class transform consults them. Entries are stripped from
 * the output by default: {@code Keep-Embedded-Rules} preserves them (a
 * downgraded library re-exporting its rules) and
 * {@code Ignore-Embedded-Rules} passes them through untouched (true
 * "pretend they aren't there" semantics).
 */
public class EmbeddedRulesStep extends JarStep {

    private static final String RULES_DIR = "META-INF/jvmdg";

    private static boolean isRuleFile(String name) {
        return name.startsWith(JvmdgConfig.EMBEDDED_PREFIX) && name.endsWith(".mf") &&
            name.indexOf('/', JvmdgConfig.EMBEDDED_PREFIX.length()) == -1;
    }

    @Override
    public void prepareFused(Path inputRoot, StepContext ctx) throws IOException {
        if (ctx.flags.ignoreEmbeddedRules) return;
        List<Path> ruleFiles = new ArrayList<>();
        Set<String> packages = new HashSet<>();
        try (Stream<Path> walk = Files.walk(inputRoot)) {
            walk.filter(Files::isRegularFile).forEach(p -> {
                String rel = IndexScanner.internalPath(inputRoot.relativize(p));
                if (isRuleFile(rel)) {
                    ruleFiles.add(p);
                    return;
                }
                if (rel.endsWith(".class")) {
                    packages.add(packageDir(rel));
                }
            });
        }
        ruleFiles.sort(null);
        for (Path mf : ruleFiles) {
            try (Reader r = new InputStreamReader(Files.newInputStream(mf), StandardCharsets.UTF_8)) {
                JvmdgConfig.applyEmbedded(
                    ManifestConfigReader.read(r, mf.toUri().toString()), ctx.flags, packages, ctx.logger);
            }
        }
    }

    @Override
    public JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException {
        if (!ctx.flags.ignoreEmbeddedRules && !ctx.flags.keepEmbeddedRules && isRuleFile(entry.name)) {
            return null; // strip — rules steer this transform, they don't leak downstream
        }
        return entry;
    }

    @Override
    public boolean visitDirectory(JarEntry entry, StepContext ctx) throws IOException {
        if (!ctx.flags.ignoreEmbeddedRules && !ctx.flags.keepEmbeddedRules &&
            entry.name.equals(RULES_DIR)) {
            return false; // empty META-INF/jvmdg dir follows its files out
        }
        return true;
    }

    /** The package dir of a class entry in internal form, version-prefix stripped, trailing slash, {@code ""} for the default package. */
    private static String packageDir(String rel) {
        if (rel.startsWith("META-INF/versions/")) {
            int slash = rel.indexOf('/', "META-INF/versions/".length());
            if (slash != -1) rel = rel.substring(slash + 1);
        }
        int last = rel.lastIndexOf('/');
        return last == -1 ? "" : rel.substring(0, last + 1);
    }
}
