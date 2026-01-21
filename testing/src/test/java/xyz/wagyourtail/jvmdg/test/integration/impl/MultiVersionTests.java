package xyz.wagyourtail.jvmdg.test.integration.impl;

import org.apache.commons.io.function.IOStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader;
import xyz.wagyourtail.jvmdg.test.JavaRunner;
import xyz.wagyourtail.jvmdg.test.integration.BaseIntegrationTests;
import xyz.wagyourtail.jvmdg.test.integration.FlagsAndRunner;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class MultiVersionTests extends BaseIntegrationTests {
    private static final String MULTIVERSION_PATH = "jvmdg.test.multiVersionPath";
    private static final Path original = Path.of(System.getProperty(MULTIVERSION_PATH));

    private static final FlagsAndRunner flagsAndRunner = new FlagsAndRunner(JavaRunner.JavaVersion.V1_8, flags.copy(f -> {
        f.multiReleaseOriginal = true;
        f.multiReleaseVersions = Set.of(JavaRunner.JavaVersion.V11.toOpcode(), JavaRunner.JavaVersion.V17.toOpcode());
    }));
    private static final Map<String, Map.Entry<Integer, String>> originalResults = new ConcurrentHashMap<>();
    private static final Map<String, Map.Entry<Integer, String>> fullDowngradeResults = new ConcurrentHashMap<>();

    private static List<String> mainClasses() throws IOException {
        try (FileSystem fs = Utils.openZipFileSystem(original, false)) {
            Path root = fs.getPath("/");
            return IOStream.adapt(Files.walk(root))
                .filter(e -> Files.isRegularFile(e) && e.getFileName().toString().endsWith(".class"))
                .map(e -> root.relativize(e).toString().replaceAll("\\.class$", "").replace("/", "."))
                .filter(e -> {
                    ClassReader cr = new ClassReader(Files.readAllBytes(fs.getPath(e.replace(".", "/") + ".class")));
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, ClassReader.SKIP_CODE);
                    return cn.methods.stream().anyMatch(m -> m.name.equals("main") && m.desc.equals("([Ljava/lang/String;)V"));
                })
                .unwrap().toList();
        }
    }

    private static synchronized Path getDowngradedJar() throws IOException {
        Path target = original.resolveSibling("multi-version-downgraded-" + flagsAndRunner.readableSlug() + ".jar");
        if (!Files.exists(target)) {
            ZipDowngrader.downgradeZip(
                ClassDowngrader.downgradeTo(flagsAndRunner.flags()),
                original,
                Set.of(),
                target
            );
        }
        return target;
    }

    @BeforeAll
    public static void deleteExisting() throws IOException {
        Files.deleteIfExists(original.resolveSibling("downgraded-" + flagsAndRunner.readableSlug()));
    }

    @BeforeAll
    public static void runOriginal() throws IOException, InterruptedException {
        for (String main : mainClasses()) {
            StringBuilder originalLog = new StringBuilder();
            Integer exitCode = JavaRunner.runJarInSubprocess(
                original,
                new String[0],
                main,
                Set.of(),
                Path.of("."),
                Map.of(),
                true,
                List.of(),
                launchersByVersion.get(originalVersion),
                (String it) -> originalLog.append(it).append("\n"),
                (String it) -> originalLog.append(it).append("\n")
            );
            assert exitCode != null;
            originalResults.put(main, Map.entry(exitCode, originalLog.toString()));
        }
    }

    @BeforeAll
    public static void runOnTestTarget() throws IOException, InterruptedException {

        FlagsAndRunner downgraded = new FlagsAndRunner(JavaRunner.JavaVersion.V1_8, flags.copy(f -> f.quiet = true));

        for (String main : mainClasses()) {
            StringBuilder fullDowngradeLog = new StringBuilder();
            Integer exitCode = JavaRunner.runJarInSubprocess(
                getDowngradedJar(),
                new String[0],
                main,
                Stream.concat(Stream.of(getApiJar(downgraded)), downgradeClasspath.stream()).collect(Collectors.toSet()),
                Path.of("."),
                Map.of(),
                true,
                List.of(),
                launchersByVersion.get(JavaRunner.JavaVersion.V1_8),
                (String it) -> fullDowngradeLog.append(it).append("\n"),
                (String it) -> fullDowngradeLog.append(it).append("\n")
            );
            assert exitCode != null;
            fullDowngradeResults.put(main, Map.entry(exitCode, fullDowngradeLog.toString()));
        }
    }

    public static Stream<Arguments> arguments() throws IOException {
        return mainClasses().stream().map(Arguments::of);
    }

    public static JavaRunner.JavaVersion getVersionOf(String main) {
        Matcher m = Pattern.compile("J(\\d+)$").matcher(main);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid main class name: " + main);
        }
        return JavaRunner.JavaVersion.fromMajor(Integer.parseInt(m.group(1)));
    }

    @ParameterizedTest
    @MethodSource({"arguments"})
    @Execution(ExecutionMode.CONCURRENT)
    public void testMultiVersion(String main) throws IOException, InterruptedException {
        JavaRunner.JavaVersion version = getVersionOf(main);
        JavaRunner.JavaVersion aboveTarget = launchersByVersion.keySet().stream().filter(e -> e.getMajorVersion() >= version.getMajorVersion()).findFirst().orElseThrow();

        Map.Entry<Integer, String> originalResult = originalResults.get(main);
        Map.Entry<Integer, String> fullDowngradeResult = fullDowngradeResults.get(main);

        FlagsAndRunner downgraded = new FlagsAndRunner(JavaRunner.JavaVersion.V1_8, flags.copy(f -> f.quiet = true));

        assertNotEquals(originalResult, fullDowngradeResult);

        StringBuilder aboveTargetLog = new StringBuilder();
        Integer aboveTargetExitCode = JavaRunner.runJarInSubprocess(
            getDowngradedJar(),
            new String[0],
            main,
            Stream.concat(Stream.of(getApiJar(downgraded)), downgradeClasspath.stream()).collect(Collectors.toSet()),
            Path.of("."),
            Map.of(),
            true,
            List.of(),
            launchersByVersion.get(aboveTarget),
            (String it) -> aboveTargetLog.append(it).append("\n"),
            (String it) -> aboveTargetLog.append(it).append("\n")
        );

        assert aboveTargetExitCode != null;
        assertEquals(originalResult, Map.entry(aboveTargetExitCode, aboveTargetLog.toString()));
    }

}
