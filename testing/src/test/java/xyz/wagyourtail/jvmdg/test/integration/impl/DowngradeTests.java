package xyz.wagyourtail.jvmdg.test.integration.impl;

import org.apache.commons.io.function.IOStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.test.JavaRunner;
import xyz.wagyourtail.jvmdg.test.integration.BaseIntegrationTests;
import xyz.wagyourtail.jvmdg.test.integration.FlagsAndRunner;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DowngradeTests extends BaseIntegrationTests {
    private static final String DOWNGRADE_PATH = "jvmdg.test.downgradePath";
    private static final Path original = Path.of(System.getProperty(DOWNGRADE_PATH));
    private static final String BATCH_MAIN = "jvmdg.featuretests.BatchMain";
    private static final Map<String, Map.Entry<Integer, String>> originalResults = new ConcurrentHashMap<>();
    private static final String[] args = new String[]{
        "arg1",
        "testArg2",
        "arg3"
    };
    private static final Map<Flags, Path> downgradedPaths = new ConcurrentHashMap<>();
    private static final Map<Flags, Path> shadedPaths = new ConcurrentHashMap<>();
    // one subprocess per flags-config runs ALL mains (BatchMain frames) instead
    // of one JVM per (main x config) — ~650 launches down to ~16
    private static final Map<FlagsAndRunner, Map<String, Map.Entry<Integer, String>>> downgradedResults = new ConcurrentHashMap<>();
    private static final Map<FlagsAndRunner, Map<String, Map.Entry<Integer, String>>> shadedResults = new ConcurrentHashMap<>();
    private static final Map<FlagsAndRunner, Map<String, Map.Entry<Integer, String>>> runtimeResults = new ConcurrentHashMap<>();
    private static volatile List<String> mainsCache;

    private static Stream<FlagsAndRunner> flags() {
        Flags flags = BaseIntegrationTests.flags.copy();
        flags.logLevel = Logger.Level.FATAL;

        return Stream.of(
            // test minimum version
//            new FlagsAndRunner(JavaRunner.JavaVersion.V1_8, flags.copy(e -> {
//                e.classVersion = JavaRunner.JavaVersion.V1_5.toOpcode();
//                e.shadeInlining = true;
//            })),
            // test LTS versions
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
            })),
            new FlagsAndRunner(JavaRunner.JavaVersion.V25, flags.copy(e -> {
                e.classVersion = JavaRunner.JavaVersion.V25.toOpcode();
                e.shadeInlining = true;
            })),
            // check that running downgraded on newer version works
            new FlagsAndRunner(JavaRunner.JavaVersion.V21, flags.copy(e -> {
                e.classVersion = JavaRunner.JavaVersion.V1_8.toOpcode();
                e.shadeInlining = true;
            }))
        ).filter(e -> launchersByVersion.containsKey(e.targetVersion()));
    }

    private static List<String> mainClasses() throws IOException {
        if (mainsCache != null) return mainsCache;
        try (FileSystem fs = Utils.openZipFileSystem(original, false)) {
            Path root = fs.getPath("/");
            List<String> mains = IOStream.adapt(Files.walk(root))
                .filter(e -> Files.isRegularFile(e) && e.getFileName().toString().endsWith(".class"))
                .map(e -> root.relativize(e).toString().replaceAll("\\.class$", "").replace("/", "."))
                .filter(e -> {
                    ClassReader cr = new ClassReader(Files.readAllBytes(fs.getPath(e.replace(".", "/") + ".class")));
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, ClassReader.SKIP_CODE);
                    if (cn.invisibleAnnotations != null) {
                        if (cn.invisibleAnnotations.stream().anyMatch(a -> a.desc.equals("Lxyz/wagyourtail/TestMain;"))) {
                            return true;
                        }
                    }
                    return cn.methods.stream().anyMatch(m -> m.name.equals("main"));
                })
                // the batch driver itself is not a test case
                .filter(e -> !e.equals(BATCH_MAIN))
                .unwrap().toList();
            mainsCache = mains;
            return mains;
        }
    }

    public static Stream<Arguments> arguments() throws IOException {
        return IOStream.adapt(flags())
            .flatMap(e ->
                IOStream.adapt(mainClasses().stream()).map(m -> Arguments.of(m, e))
            )
            .unwrap();
    }

    @BeforeAll
    public static void runOriginal() throws IOException, InterruptedException {
        originalResults.putAll(runBatch(
            original,
            batchArgs(mainClasses()),
            BATCH_MAIN,
            Set.of(),
            originalVersion
        ));
    }

    // mains that observe JVM-global state (e.g. the process child list) must
    // run on the driver thread before the parallel phase — see "++" in
    // BatchMain and --seqmain in cli.Main
    private static final Set<String> SEQUENTIAL_MAINS = Set.of(
        "jvmdg.featuretests.manual.ProcessTests"
    );

    private static String[] batchArgs(List<String> mains) {
        List<String> all = new ArrayList<>();
        for (String main : mains) {
            if (SEQUENTIAL_MAINS.contains(main)) {
                all.add(main);
            }
        }
        if (!all.isEmpty()) {
            all.add("++");
        }
        for (String main : mains) {
            if (!SEQUENTIAL_MAINS.contains(main)) {
                all.add(main);
            }
        }
        all.add("--");
        Collections.addAll(all, args);
        return all.toArray(new String[0]);
    }

    private static String[] batchArgsFor(FlagsAndRunner javaVersion) throws IOException {
        List<String> mains = new ArrayList<>();
        for (String main : mainClasses()) {
            mains.add(renamedMain(main, javaVersion));
        }
        return batchArgs(mains);
    }

    private static Map<String, Map.Entry<Integer, String>> runBatch(
            Path jar, String[] cmdArgs, String mainClass, Set<Path> classPath,
            JavaRunner.JavaVersion jv) throws IOException, InterruptedException {
        StringBuilder log = new StringBuilder();
        Integer ret = JavaRunner.runJarInSubprocess(
            jar,
            cmdArgs,
            mainClass,
            classPath,
            Path.of("."),
            Map.of(),
            true,
            List.of(),
            launchersByVersion.get(jv),
            (String it) -> log.append(it).append("\n"),
            (String it) -> log.append(it).append("\n")
        );
        Map<String, Map.Entry<Integer, String>> parsed = parseFrames(log.toString(), ret);
        if (parsed.isEmpty()) {
            throw new IllegalStateException("no batch frames parsed from " + jar + " output:\n" + log);
        }
        return parsed;
    }

    // frames: ===JVMDG-BEGIN <name> / ===JVMDG-EXIT <code> — see BatchMain and
    // cli.Main --batch
    private static Map<String, Map.Entry<Integer, String>> parseFrames(String log, Integer processExit) {
        Map<String, Map.Entry<Integer, String>> out = new LinkedHashMap<>();
        String current = null;
        StringBuilder cur = new StringBuilder();
        for (String line : log.split("\n")) {
            if (line.startsWith("===JVMDG-BEGIN ")) {
                current = line.substring("===JVMDG-BEGIN ".length());
                cur = new StringBuilder();
            } else if (line.startsWith("===JVMDG-EXIT ") && current != null) {
                out.put(current, Map.entry(Integer.parseInt(line.substring("===JVMDG-EXIT ".length()).trim()), cur.toString()));
                current = null;
            } else if (current != null) {
                cur.append(line).append("\n");
            }
        }
        if (current != null) {
            // driver died mid-main — attribute the JVM's own exit code
            out.put(current, Map.entry(processExit, cur.toString()));
        }
        return out;
    }

    // interfaces get their main moved to <name>$jvmdg$StaticDefaults below v52
    private static String renamedMain(String mainClass, FlagsAndRunner javaVersion) throws IOException {
        if (javaVersion.flags().classVersion > 51) {
            return mainClass;
        }
        try (ZipFile zf = new ZipFile(DowngradeTests.original.toFile())) {
            ZipEntry entry = zf.getEntry(mainClass.replace(".", "/") + ".class");
            try (InputStream is = zf.getInputStream(entry)) {
                ClassReader cr = new ClassReader(is);
                if ((cr.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                    return mainClass + "$jvmdg$StaticDefaults";
                }
            }
        }
        return mainClass;
    }

    private static Map<String, Map.Entry<Integer, String>> batchResults(
            Map<FlagsAndRunner, Map<String, Map.Entry<Integer, String>>> cache,
            FlagsAndRunner javaVersion,
            IOSupplier<Map<String, Map.Entry<Integer, String>>> runner) {
        return cache.computeIfAbsent(javaVersion, k -> {
            try {
                return runner.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @FunctionalInterface
    private interface IOSupplier<T> {
        T get() throws IOException, InterruptedException;
    }

    private static Path getDowngradedPath(FlagsAndRunner flags) {
        String fName = original.getFileName().toString();
        String withoutExt = fName.substring(0, fName.lastIndexOf('.'));
        String ext = fName.substring(fName.lastIndexOf('.'));
        return original.resolveSibling(withoutExt + "-downgrade-" + flags.readableSlug() + ext);
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
        String fName = original.getFileName().toString();
        String withoutExt = fName.substring(0, fName.lastIndexOf('.'));
        String ext = fName.substring(fName.lastIndexOf('.'));
        return original.resolveSibling(withoutExt + "-shade-" + flags.readableSlug() + ext);
    }

    private static synchronized Path getShadedJar(FlagsAndRunner flags) {
        return shadedPaths.computeIfAbsent(flags.flags(), e -> {
            try {
                Path target = getShadedPath(flags);
                ApiShader.shadeApis(
                    flags.flags(),
                    "featuretests",
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
    public static void clearPrevious() throws IOException {
        for (FlagsAndRunner flag : flags().toList()) {
            Files.deleteIfExists(getDowngradedPath(flag));
            Files.deleteIfExists(getApiPath(flag));
            Files.deleteIfExists(getShadedPath(flag));
        }
        for (FlagsAndRunner flag : flags().toList()) {
            getApiJar(flag);
            getDowngradedJar(flag);
            getShadedJar(flag);
        }
    }

    public static void compareResults(String mainClass, FlagsAndRunner javaVersion, Map.Entry<Integer, String> originalResult, Map.Entry<Integer, String> downgradedResult) {
        // remove hsperf error if has one on first line of downgraded
        if (downgradedResult.getValue().matches("\\[\\d+.\\d+s\\]\\[warning\\]\\[perf")) {
            downgradedResult.setValue(downgradedResult.getValue().substring(downgradedResult.getValue().indexOf('\n')));
        }
        assertEquals(originalResult.getValue(), downgradedResult.getValue(), "Output mismatch for " + mainClass + " on " + javaVersion.readableSlug());
        assertEquals(originalResult.getKey(), downgradedResult.getKey(), "Exit code mismatch for " + mainClass + " on " + javaVersion.readableSlug());
    }

    @ParameterizedTest
    @MethodSource({"arguments"})
    @Execution(ExecutionMode.CONCURRENT)
    public void testDowngrade(String mainClass, FlagsAndRunner javaVersion) throws IOException, InterruptedException {
        System.out.println("TEST_DOWNGRADE: " + mainClass + " on " + javaVersion.readableSlug());
        Map<String, Map.Entry<Integer, String>> results = batchResults(downgradedResults, javaVersion, () ->
            runBatch(
                getDowngradedJar(javaVersion),
                batchArgsFor(javaVersion),
                BATCH_MAIN,
                Stream.concat(Stream.of(getApiJar(javaVersion)), downgradeClasspath.stream()).collect(Collectors.toSet()),
                javaVersion.targetVersion()
            )
        );
        Map.Entry<Integer, String> downgraded = results.get(renamedMain(mainClass, javaVersion));
        assertNotNull(downgraded, "no batch result for " + mainClass + " on " + javaVersion.readableSlug());
        compareResults(
            mainClass,
            javaVersion,
            originalResults.get(mainClass),
            downgraded
        );
    }

    @ParameterizedTest
    @MethodSource({"arguments"})
    @Execution(ExecutionMode.CONCURRENT)
    public void testShade(String mainClass, FlagsAndRunner javaVersion) throws IOException, InterruptedException {
        System.out.println("TEST_SHADE: " + mainClass + " on " + javaVersion.readableSlug());
        Map<String, Map.Entry<Integer, String>> results = batchResults(shadedResults, javaVersion, () ->
            runBatch(
                getShadedJar(javaVersion),
                batchArgsFor(javaVersion),
                BATCH_MAIN,
                Set.of(),
                javaVersion.targetVersion()
            )
        );
        Map.Entry<Integer, String> shaded = results.get(renamedMain(mainClass, javaVersion));
        assertNotNull(shaded, "no batch result for " + mainClass + " on " + javaVersion.readableSlug());
        compareResults(
            mainClass,
            javaVersion,
            originalResults.get(mainClass),
            shaded
        );
    }

    // the runtime batch only needs jvmdg's own build outputs + real deps — the
    // full test classpath (~800KB of junit/gson/test classes) makes
    // /proc/self/cmdline huge enough that the JDK's native
    // ProcessHandle.Info.arguments() fails to parse it (page-sized buffer),
    // breaking parity with the small-classpath baseline run
    private static Set<Path> runtimeBatchClasspath() {
        return Stream.of(System.getProperty("java.class.path").split(File.pathSeparator))
            .map(Path::of)
            .filter(p -> {
                String s = p.toString();
                String n = p.getFileName() == null ? "" : p.getFileName().toString();
                if (n.isEmpty()) return false;
                if (s.contains("/testing/")) return false;
                return s.contains("jvmdowngrader") || s.contains("JvmDowngrader")
                    || n.startsWith("asm-") || n.startsWith("gson-")
                    || n.startsWith("commons-io") || n.startsWith("commons-compress")
                    || n.startsWith("commons-lang3") || n.startsWith("annotations-")
                    || n.startsWith("kotlin-stdlib") || n.startsWith("java-diff-utils");
            })
            .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @MethodSource({"arguments"})
    @Execution(ExecutionMode.CONCURRENT)
    public void testRuntime(String mainClass, FlagsAndRunner javaVersion) throws IOException, InterruptedException {
        System.out.println("TEST_RUNTIME: " + mainClass + " on " + javaVersion.readableSlug());
        Set<Path> classpathJars = runtimeBatchClasspath();
        Map<String, Map.Entry<Integer, String>> results = batchResults(runtimeResults, javaVersion, () -> {
            List<String> rargs = new ArrayList<>(List.of(
                "-a",
                javaApi.toString(),
                "--quiet",
                "bootstrap",
                "--batch",
                "--classpath",
                DowngradeTests.original.toAbsolutePath().toString()
            ));
            for (String m : mainClasses()) {
                if (SEQUENTIAL_MAINS.contains(m)) {
                    rargs.add("--seqmain");
                } else {
                    rargs.add("-m");
                }
                rargs.add(m);
            }
            rargs.addAll(Arrays.asList(DowngradeTests.args));
            return runBatch(
                null,
                rargs.toArray(new String[0]),
                "xyz.wagyourtail.jvmdg.cli.Main",
                classpathJars,
                javaVersion.targetVersion()
            );
        });
        Map.Entry<Integer, String> runtime = results.get(mainClass);
        assertNotNull(runtime, "no batch result for " + mainClass + " on " + javaVersion.readableSlug());
        compareResults(
            mainClass,
            javaVersion,
            originalResults.get(mainClass),
            runtime
        );
    }

}
