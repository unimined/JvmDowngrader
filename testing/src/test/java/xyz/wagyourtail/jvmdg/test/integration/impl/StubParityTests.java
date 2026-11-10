package xyz.wagyourtail.jvmdg.test.integration.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.test.JavaRunner;
import xyz.wagyourtail.jvmdg.test.integration.BaseIntegrationTests;
import xyz.wagyourtail.jvmdg.test.integration.FlagsAndRunner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Behavioral parity for generated stub call sites. The
 * {@code generateStubTests}/{@code stubtestsJar} tasks emit and compile
 * {@code jvmdg.stubtests.Index} — per-owner classes with up to three
 * profile-ladder call sites per registered stub member plus reflected
 * {@code @StubTest} manual tests, each printing {@code <id> RESULT:...} or
 * {@code <id> EXC:...}. Ids are {@code m####} (optionally {@code b}/{@code c}
 * suffixed for extra profiles) and {@code x####} for manual tests.
 *
 * <p>This test runs the jar natively on the original test JDK, then runs the
 * downgraded and downgraded+shaded jars on each target launcher, and diffs the
 * per-member verdicts. Divergences must be registered in
 * {@code testing/stubparity-known-diffs.txt} — one entry per line:</p>
 *
 * <pre>{@code
 * # rationale (required: a comment line must precede each contiguous entry block)
 * <slug|all> <stable-ref> ==> <native-glob> ==> <downgraded-glob>
 * }</pre>
 *
 * <p>{@code slug} is {@code <classVersion>-downgrade|shade} (e.g.
 * {@code 52-downgrade}) or {@code all} for diffs present on every config.
 * {@code stable-ref} is the index.txt identity — the canonical
 * {@code Lowner;name;desc} member key plus {@code @<profile>} for generated
 * sites (e.g. {@code Ljava/lang/Class;getNestMembers;()[Ljava/lang/Class;@meaningful})
 * or {@code <class>.<method>} for manual tests — so entries survive
 * {@code m####} renumbering. The two globs pin the actual diff shape:
 * {@code *} is the only wildcard, {@code \xNN} and backslash-uXXXX escapes
 * decode to control chars, everything else is literal. A matched id whose
 * values stop matching either pattern fails, as does a resolved entry whose
 * values start matching.</p>
 */
public class StubParityTests extends BaseIntegrationTests {
    private static final String STUB_PATH = "jvmdg.test.stubPath";
    private static final String STUB_REPORTS = "jvmdg.test.stubReports";
    private static final Path original = Path.of(System.getProperty(STUB_PATH));
    private static final Path REPORTS = Path.of(
        System.getProperty(STUB_REPORTS, "build/reports/stubtests"));
    private static final String MAIN = "jvmdg.stubtests.Index";
    // the file lives at <repo>/testing/stubparity-known-diffs.txt; the test
    // workdir is the testing module dir, so also try the bare name
    private static final List<Path> KNOWN_DIFFS_CANDIDATES = List.of(
        Paths.get("stubparity-known-diffs.txt"),
        Paths.get("testing", "stubparity-known-diffs.txt"));
    private static final Path KNOWN_DIFFS = KNOWN_DIFFS_CANDIDATES.stream()
        .filter(Files::exists).findFirst()
        .orElse(KNOWN_DIFFS_CANDIDATES.get(0));

    private static final Map.Entry<Integer, String>[] originalResult = new Map.Entry[1];
    private static final Map<Flags, Path> downgradedPaths = new ConcurrentHashMap<>();
    private static final Map<Flags, Path> shadedPaths = new ConcurrentHashMap<>();

    private static Stream<FlagsAndRunner> flags() {
        Flags flags = BaseIntegrationTests.flags.copy();
        flags.logLevel = Logger.Level.FATAL;

        return Stream.of(
            new FlagsAndRunner(JavaRunner.JavaVersion.V1_8, flags.copy(e -> {
                e.classVersion = JavaRunner.JavaVersion.V1_8.toOpcode();
                e.shadeInlining = true;
            })),
            new FlagsAndRunner(JavaRunner.JavaVersion.V11, flags.copy(e -> {
                e.classVersion = JavaRunner.JavaVersion.V11.toOpcode();
                e.shadeInlining = true;
            })),
            new FlagsAndRunner(JavaRunner.JavaVersion.V17, flags.copy(e -> {
                e.classVersion = JavaRunner.JavaVersion.V17.toOpcode();
                e.shadeInlining = true;
            })),
            new FlagsAndRunner(JavaRunner.JavaVersion.V21, flags.copy(e -> {
                e.classVersion = JavaRunner.JavaVersion.V21.toOpcode();
                e.shadeInlining = true;
            }))
        ).filter(e -> launchersByVersion.containsKey(e.targetVersion()));
    }

    private static Path getDowngradedPath(FlagsAndRunner flags) {
        return original.resolveSibling("stubtests-downgrade-" + flags.readableSlug() + ".jar");
    }

    private static synchronized Path getDowngradedJar(FlagsAndRunner flags) {
        return downgradedPaths.computeIfAbsent(flags.flags(), e -> {
            try {
                Path target = getDowngradedPath(flags);
                ZipDowngrader.downgradeZip(
                    ClassDowngrader.downgradeTo(flags.flags()),
                    original,
                    Set.of(),
                    target
                );
                return target;
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

    private static Path getShadedPath(FlagsAndRunner flags) {
        return original.resolveSibling("stubtests-shade-" + flags.readableSlug() + ".jar");
    }

    private static synchronized Path getShadedJar(FlagsAndRunner flags) {
        return shadedPaths.computeIfAbsent(flags.flags(), e -> {
            try {
                Path target = getShadedPath(flags);
                ApiShader.shadeApis(
                    flags.flags(),
                    "stubparity",
                    getDowngradedJar(flags).toFile(),
                    target.toFile(),
                    Set.of(getApiJar(flags).toFile())
                );
                return target;
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

    @BeforeAll
    public static void setup() throws IOException, InterruptedException {
        for (FlagsAndRunner flag : flags().toList()) {
            Files.deleteIfExists(getDowngradedPath(flag));
            Files.deleteIfExists(getApiPath(flag));
            Files.deleteIfExists(getShadedPath(flag));
        }
        StringBuilder originalLog = new StringBuilder();
        Integer exitCode = JavaRunner.runJarInSubprocess(
            original,
            new String[0],
            MAIN,
            Set.of(),
            Path.of("."),
            Map.of(),
            true,
            List.of(),
            launchersByVersion.get(originalVersion),
            (String it) -> originalLog.append(it).append("\n"),
            (String it) -> originalLog.append(it).append("\n")
        );
        assertNotNull(exitCode);
        originalResult[0] = Map.entry(exitCode, originalLog.toString());
        for (FlagsAndRunner flag : flags().toList()) {
            getApiJar(flag);
            getDowngradedJar(flag);
            getShadedJar(flag);
        }
    }

    private static Map<String, String> parse(String output) {
        Map<String, String> results = new LinkedHashMap<>();
        for (String line : output.split("\n")) {
            int sp = line.indexOf(' ');
            if (sp > 0 && (line.charAt(0) == 'm' || line.charAt(0) == 'x')) {
                String id = line.substring(0, sp);
                if (id.matches("[mx]\\d{4}[a-z]?")) {
                    results.put(id, line.substring(sp + 1));
                }
            }
        }
        return results;
    }

    /** One parsed {@code stubparity-known-diffs.txt} entry. */
    private static final class KnownDiff {
        final int lineNo;
        final String slug;           // "<flags-slug>-<mode>" or "all"
        final String ref;            // "Lowner;name;desc@profile" or manual "pkg.Class.method"
        final String nativeGlob;
        final String downgradedGlob;
        String id;                   // resolved corpus id for the active run
        boolean consumed;            // the id actually diverged and this entry was evaluated

        KnownDiff(int lineNo, String slug, String ref, String nativeGlob, String downgradedGlob) {
            this.lineNo = lineNo;
            this.slug = slug;
            this.ref = ref;
            this.nativeGlob = nativeGlob;
            this.downgradedGlob = downgradedGlob;
        }
    }

    /**
     * {@code index.txt} stable-ref → current corpus id. Generated sites key on
     * {@code Lowner;name;desc@profile}; manual tests on the emitted
     * {@code pkg.Class.method} token.
     */
    private static Map<String, String> stableRefs() throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        Path index = REPORTS.resolve("index.txt");
        if (!Files.exists(index)) {
            return out;
        }
        for (String line : Files.readAllLines(index)) {
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            int sp = line.indexOf(' ');
            if (sp <= 0) continue;
            String id = line.substring(0, sp);
            String rest = line.substring(sp + 1);
            int manual = rest.indexOf(" [manual]");
            if (manual >= 0) {
                out.put(rest.substring(0, manual), id);
                continue;
            }
            int kindAt = rest.indexOf(" [");
            int profileAt = rest.indexOf(" profile=");
            if (kindAt < 0 || profileAt < 0) continue;
            out.put(rest.substring(0, kindAt) + "@" + rest.substring(profileAt + 9), id);
        }
        return out;
    }

    /**
     * Parses the whole known-diffs file, collecting entries into {@code out}
     * and returning file-level format errors. Every entry must sit in a
     * contiguous block containing a {@code #} rationale comment above it.
     */
    private static List<String> loadKnownDiffs(List<KnownDiff> out) throws IOException {
        List<String> errors = new ArrayList<>();
        if (!Files.exists(KNOWN_DIFFS)) {
            return errors;
        }
        List<String> lines = Files.readAllLines(KNOWN_DIFFS);
        boolean commentInBlock = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                commentInBlock = false;
                continue;
            }
            if (line.startsWith("#")) {
                commentInBlock = true;
                continue;
            }
            String[] sides = line.split(" ==> ", -1);
            int head = sides[0].indexOf(' ');
            if (sides.length != 3 || head <= 0) {
                errors.add(KNOWN_DIFFS + ":" + (i + 1)
                    + ": malformed entry — expected '<slug> <stable-ref> ==> <native-glob> ==> <downgraded-glob>'");
                continue;
            }
            String ref = sides[0].substring(head + 1).trim();
            if (ref.isEmpty() || sides[1].trim().isEmpty() || sides[2].trim().isEmpty()) {
                errors.add(KNOWN_DIFFS + ":" + (i + 1) + ": empty ref or glob");
                continue;
            }
            if (!commentInBlock) {
                errors.add(KNOWN_DIFFS + ":" + (i + 1)
                    + ": missing rationale comment above entry");
            }
            out.add(new KnownDiff(i + 1, sides[0].substring(0, head), ref,
                sides[1].trim(), sides[2].trim()));
        }
        return errors;
    }

    /** Slugs produced by the active {@link #flags()} matrix, plus {@code all}. */
    private static Set<String> validSlugs() {
        Set<String> out = new java.util.HashSet<>();
        out.add("all");
        flags().forEach(f -> {
            out.add(f.readableSlug() + "-downgrade");
            out.add(f.readableSlug() + "-shade");
        });
        return out;
    }

    /**
     * {@code *}-wildcard full-value match; {@code null} never matches.
     * Literal text is matched verbatim (no regex — values may contain
     * {@code \Q}/{@code \E} and friends); backslash-xNN and
     * backslash-uXXXX escapes in the glob decode to control chars, which
     * the corpus can legitimately print (e.g. a {@code \0} boundary arg).
     */
    private static boolean globMatch(String glob, String value) {
        if (value == null) return false;
        String literal = unescape(glob);
        String[] parts = literal.split("\\*", -1);
        int pos = 0;
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            int idx;
            if (i == 0) {
                idx = value.startsWith(p) ? 0 : -1;
            } else if (i == parts.length - 1 && !literal.endsWith("*")) {
                // anchored at the end: last literal must be a suffix
                idx = value.endsWith(p) && value.length() - p.length() >= pos
                    ? value.length() - p.length() : -1;
            } else {
                idx = value.indexOf(p, pos);
            }
            if (idx < 0) return false;
            pos = idx + p.length();
        }
        return true;
    }

    /** Decodes backslash-xNN and backslash-uXXXX escapes; other backslashes pass through. */
    private static String unescape(String s) {
        if (s.indexOf('\\') < 0) return s;
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == 'x' && i + 3 < s.length()) {
                    int hex = hex(s, i + 2, 2);
                    if (hex >= 0) {
                        out.append((char) hex);
                        i += 3;
                        continue;
                    }
                } else if (n == 'u' && i + 5 < s.length()) {
                    int hex = hex(s, i + 2, 4);
                    if (hex >= 0) {
                        out.append((char) hex);
                        i += 5;
                        continue;
                    }
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static int hex(String s, int off, int len) {
        int v = 0;
        for (int i = off; i < off + len; i++) {
            int d = Character.digit(s.charAt(i), 16);
            if (d < 0) return -1;
            v = (v << 4) | d;
        }
        return v;
    }

    private void compareResults(String mode, FlagsAndRunner javaVersion,
                                Map.Entry<Integer, String> original,
                                Map.Entry<Integer, String> actual) throws IOException {
        String slug = javaVersion.readableSlug() + "-" + mode;
        assertEquals(original.getKey(), actual.getKey(),
            "Exit code mismatch on " + slug);
        Map<String, String> expected = parse(original.getValue());
        Map<String, String> got = parse(actual.getValue());

        List<KnownDiff> entries = new ArrayList<>();
        List<String> failures = new ArrayList<>(loadKnownDiffs(entries));

        Set<String> validSlugs = validSlugs();
        for (KnownDiff kd : entries) {
            if (!validSlugs.contains(kd.slug)) {
                failures.add(KNOWN_DIFFS + ":" + kd.lineNo + ": unknown slug '" + kd.slug
                    + "' — not produced by the active test matrix");
            }
        }

        Map<String, String> refs = stableRefs();
        Map<String, String> refById = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : refs.entrySet()) {
            refById.put(e.getValue(), e.getKey());
        }
        Map<String, KnownDiff> byId = new LinkedHashMap<>();
        for (KnownDiff kd : entries) {
            if (!kd.slug.equals(slug) && !kd.slug.equals("all")) continue;
            kd.id = refs.get(kd.ref);
            if (kd.id == null) {
                failures.add(KNOWN_DIFFS + ":" + kd.lineNo + ": stable ref " + kd.ref
                    + " does not resolve to a corpus id (stale or mistyped)");
                continue;
            }
            if (byId.put(kd.id, kd) != null) {
                failures.add(KNOWN_DIFFS + ":" + kd.lineNo + ": duplicate entry for "
                    + kd.ref + " on " + slug);
            }
        }

        List<String> newDiffs = new ArrayList<>();
        for (Map.Entry<String, String> e : expected.entrySet()) {
            String id = e.getKey();
            String gotVal = got.get(id);
            if (e.getValue().equals(gotVal)) continue;
            String ref = refById.getOrDefault(id, "?");
            KnownDiff kd = byId.get(id);
            if (kd == null) {
                newDiffs.add(id + " " + ref + "\n      native  = " + e.getValue()
                    + "\n      actual  = " + gotVal);
            } else {
                kd.consumed = true;
                if (!globMatch(kd.nativeGlob, e.getValue())
                        || !globMatch(kd.downgradedGlob, gotVal)) {
                    failures.add("known-diff shape changed on " + slug + " for " + kd.ref
                        + " (" + id + ", " + KNOWN_DIFFS + ":" + kd.lineNo + ")"
                        + "\n      native    = " + e.getValue()
                        + "\n      expected ~= " + kd.nativeGlob
                        + "\n      actual    = " + gotVal
                        + "\n      expected ~= " + kd.downgradedGlob);
                }
            }
        }
        for (String id : got.keySet()) {
            if (!expected.containsKey(id)) {
                newDiffs.add(id + " " + refById.getOrDefault(id, "?")
                    + " missing-from-original actual=" + got.get(id));
            }
        }
        for (KnownDiff kd : entries) {
            if ((kd.slug.equals(slug) || kd.slug.equals("all"))
                    && kd.id != null && !kd.consumed) {
                failures.add("known-diff no longer differs on " + slug + ": " + kd.ref
                    + " (" + kd.id + ", " + KNOWN_DIFFS + ":" + kd.lineNo
                    + ") — remove the entry");
            }
        }
        if (!newDiffs.isEmpty()) {
            failures.add("stub parity diffs on " + slug + " (add to " + KNOWN_DIFFS
                + " if accepted):\n  " + String.join("\n  ", newDiffs));
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
        if (expected.isEmpty()) {
            fail("no member results parsed from " + slug + " output");
        }
    }

    @ParameterizedTest
    @MethodSource({"flags"})
    @Execution(ExecutionMode.CONCURRENT)
    public void testDowngrade(FlagsAndRunner javaVersion) throws IOException, InterruptedException {
        Map.Entry<Integer, String> original = originalResult[0];
        StringBuilder downgradedLog = new StringBuilder();
        Integer ret = JavaRunner.runJarInSubprocess(
            getDowngradedJar(javaVersion),
            new String[0],
            MAIN,
            Stream.concat(Stream.of(getApiJar(javaVersion)), downgradeClasspath.stream()).collect(Collectors.toSet()),
            Path.of("."),
            Map.of(),
            true,
            List.of(),
            launchersByVersion.get(javaVersion.targetVersion()),
            (String it) -> downgradedLog.append(it).append("\n"),
            (String it) -> downgradedLog.append(it).append("\n")
        );
        assertNotNull(ret);
        compareResults("downgrade", javaVersion, original, Map.entry(ret, downgradedLog.toString()));
    }

    @ParameterizedTest
    @MethodSource({"flags"})
    @Execution(ExecutionMode.CONCURRENT)
    public void testShade(FlagsAndRunner javaVersion) throws IOException, InterruptedException {
        Map.Entry<Integer, String> original = originalResult[0];
        StringBuilder shadedLog = new StringBuilder();
        Integer ret = JavaRunner.runJarInSubprocess(
            getShadedJar(javaVersion),
            new String[0],
            MAIN,
            Set.of(),
            Path.of("."),
            Map.of(),
            true,
            List.of(),
            launchersByVersion.get(javaVersion.targetVersion()),
            (String it) -> shadedLog.append(it).append("\n"),
            (String it) -> shadedLog.append(it).append("\n")
        );
        assertNotNull(ret);
        compareResults("shade", javaVersion, original, Map.entry(ret, shadedLog.toString()));
    }
}
