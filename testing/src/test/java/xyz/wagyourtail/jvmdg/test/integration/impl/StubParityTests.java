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
 * {@code testing/stubparity-known-diffs.txt} ({@code <slug> <id>} per line) —
 * a ratchet: new diffs fail the test, and diffs that start matching also fail
 * so the baseline self-heals.</p>
 */
public class StubParityTests extends BaseIntegrationTests {
    private static final String STUB_PATH = "jvmdg.test.stubPath";
    private static final Path original = Path.of(System.getProperty(STUB_PATH));
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

    private static Set<String> knownDiffs(String slug) throws IOException {
        Set<String> out = new java.util.HashSet<>();
        if (Files.exists(KNOWN_DIFFS)) {
            for (String line : Files.readAllLines(KNOWN_DIFFS)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith(slug + " ")) {
                    out.add(line.substring(slug.length() + 1));
                }
            }
        }
        return out;
    }

    private void compareResults(String mode, FlagsAndRunner javaVersion,
                                Map.Entry<Integer, String> original,
                                Map.Entry<Integer, String> actual) throws IOException {
        String slug = javaVersion.readableSlug() + "-" + mode;
        assertEquals(original.getKey(), actual.getKey(),
            "Exit code mismatch on " + slug);
        Map<String, String> expected = parse(original.getValue());
        Map<String, String> got = parse(actual.getValue());
        Set<String> known = knownDiffs(slug);

        List<String> newDiffs = new ArrayList<>();
        for (Map.Entry<String, String> e : expected.entrySet()) {
            String gotVal = got.get(e.getKey());
            if (e.getValue().equals(gotVal)) continue;
            String entry = e.getKey() + " expected=" + e.getValue() + " actual=" + gotVal;
            if (!known.remove(e.getKey())) {
                newDiffs.add(entry);
            }
        }
        for (String id : got.keySet()) {
            if (!expected.containsKey(id)) {
                newDiffs.add(id + " missing-from-original actual=" + got.get(id));
            }
        }
        assertTrue(newDiffs.isEmpty(),
            "stub parity diffs on " + slug + " (add to " + KNOWN_DIFFS + " if accepted):\n  "
                + String.join("\n  ", newDiffs));
        assertTrue(known.isEmpty(),
            "members in " + KNOWN_DIFFS + " now match on " + slug + " — remove their lines:\n  "
                + String.join("\n  ", known));
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
