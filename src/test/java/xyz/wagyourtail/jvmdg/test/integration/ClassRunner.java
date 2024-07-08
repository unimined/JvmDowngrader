package xyz.wagyourtail.jvmdg.test.integration;

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
import xyz.wagyourtail.jvmdg.test.JavaRunner;
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

public class ClassRunner {
    private static final String JVMDG_VERSION_KEY = "jvmdg.test.version";
    private static final String ORIGINAL_VERSION_KEY = "jvmdg.test.originalVersion";
    private static final String JAVA_VERSION_KEY = "jvmdg.test.javaVersion";
    private static final String LAUNCHER_KEY = "jvmdg.test.launcher";
    private static final String DOWNGRADE_CLASSPATH = "jvmdg.test.downgradeClasspath";


    private static final Path original = Path.of("./downgradetest/build/libs/downgradetest-1.0.0.jar");
    private static final Path javaApi = Path.of("./java-api/build/libs/jvmdowngrader-java-api-" + System.getProperty(JVMDG_VERSION_KEY) + ".jar");
    private static final Path sharedClasses = Path.of("./build/classes/java/shared");

    private static final List<Path> downgradeClasspath = Arrays.stream(System.getProperty(DOWNGRADE_CLASSPATH).split(File.pathSeparator)).map(Path::of).toList();

    private static final Flags flags = new Flags();

    private static final JavaRunner.JavaVersion originalVersion = JavaRunner.JavaVersion.fromMajor(Integer.parseInt(System.getProperty(ORIGINAL_VERSION_KEY)));

    private static final List<JavaRunner.JavaVersion> javaVersions = Arrays.stream(System.getProperty(JAVA_VERSION_KEY).split(File.pathSeparator)).map(e -> JavaRunner.JavaVersion.fromMajor(Integer.parseInt(e))).toList();
    private static final List<Path> launchers = Arrays.stream(System.getProperty(LAUNCHER_KEY).split(File.pathSeparator)).map(Path::of).toList();

    private static final Map<JavaRunner.JavaVersion, Path> launchersByVersion = zip(javaVersions.stream(), launchers.stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    private static <T, U> Stream<Map.Entry<T, U>> zip(Stream<T> a, Stream<U> b) {
        Iterator<T> aIt = a.iterator();
        Iterator<U> bIt = b.iterator();
        List<Map.Entry<T, U>> out = new ArrayList<>();
        while (aIt.hasNext() && bIt.hasNext()) {
            out.add(Map.entry(aIt.next(), bIt.next()));
        }
        return out.stream();
    }

    private static Stream<FlagsAndRunner> flags() {
        Flags flags = ClassRunner.flags.copy();
        flags.quiet = true;
        flags.api = javaApi.toFile();

        return Stream.of(
            new FlagsAndRunner(flags.copy(e -> e.classVersion = JavaRunner.JavaVersion.V1_8.toOpcode()), JavaRunner.JavaVersion.V1_8)
//            new FlagsAndRunner(flags.copy(e -> {
//                e.classVersion = JavaRunner.JavaVersion.V1_7.toOpcode();
//                e.debugSkipStubs = Set.of(JavaRunner.JavaVersion.V1_8.toOpcode());
//            }), JavaRunner.JavaVersion.V1_8),
//            new FlagsAndRunner(flags.copy(e -> e.classVersion = JavaRunner.JavaVersion.V1_7.toOpcode()), JavaRunner.JavaVersion.V1_7)
        ).filter(e -> javaVersions.contains(e.targetVersion));
    }

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


    public static Stream<Arguments> arguments() throws IOException {
        return IOStream.adapt(flags())
            .flatMap(e ->
                IOStream.adapt(mainClasses().stream()).map(m -> Arguments.of(m, e))
            )
            .unwrap();
    }

    private static final Map<String, Map.Entry<Integer, String>> originalResults = new ConcurrentHashMap<>();

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

    private static final Map<FlagsAndRunner, Path> downgradedPaths = new ConcurrentHashMap<>();

    private static Path getDowngradedPath(FlagsAndRunner flags) {
        String fName = original.getFileName().toString();
        String withoutExt = fName.substring(0, fName.lastIndexOf('.'));
        String ext = fName.substring(fName.lastIndexOf('.'));
        return original.resolveSibling(withoutExt + "-downgrade-" + flags.readableSlug() + ext);
    }

    private static final Map<FlagsAndRunner, Path> shadedPaths = new ConcurrentHashMap<>();

    private static Path getShadedPath(FlagsAndRunner flags) {
        String fName = original.getFileName().toString();
        String withoutExt = fName.substring(0, fName.lastIndexOf('.'));
        String ext = fName.substring(fName.lastIndexOf('.'));
        return original.resolveSibling(withoutExt + "-shade-" + flags.readableSlug() + ext);
    }

    private static synchronized Path getDowngradedJar(FlagsAndRunner flags) {
        return shadedPaths.computeIfAbsent(flags, e -> {
            try {
                Path target = getDowngradedPath(flags);
                ZipDowngrader.downgradeZip(
                    ClassDowngrader.downgradeTo(flags.flags),
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

    private static final Map<FlagsAndRunner, Path> apiPaths = new ConcurrentHashMap<>();

    private static synchronized Path getShadedJar(FlagsAndRunner flags) {
        return apiPaths.computeIfAbsent(flags, e -> {
            try {
                Path target = getShadedPath(flags);
                ApiShader.shadeApis(
                    flags.flags,
                    "downgradetest",
                    getDowngradedJar(flags).toFile(),
                    target.toFile(),
                    getApiJar(flags).toFile()
                );
                return target;
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

    private static Path getApiPath(FlagsAndRunner flags) {
        String fName = javaApi.getFileName().toString();
        String withoutExt = fName.substring(0, fName.lastIndexOf('.'));
        String ext = fName.substring(fName.lastIndexOf('.'));
        return Path.of("./build/tmp/test/" + withoutExt + "-downgrade-" + flags.readableSlug() + ext);
    }

    private static synchronized Path getApiJar(FlagsAndRunner flags) {
        return downgradedPaths.computeIfAbsent(flags, e -> {
            try {
                Path target = getApiPath(flags);
                ZipDowngrader.downgradeZip(
                    ClassDowngrader.downgradeTo(flags.flags),
                    flags.flags.api.toPath(),
                    Set.of(),
                    target
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
    }

    @ParameterizedTest
    @MethodSource({"xyz.wagyourtail.jvmdg.test.ClassRunner#arguments"})
    @Execution(ExecutionMode.CONCURRENT)
    public void testDowngrade(String mainClass, FlagsAndRunner javaVersion) throws IOException, InterruptedException {
        System.out.println("TEST_DOWNGRADE: Running " + mainClass + " on " + javaVersion.readableSlug());
        Map.Entry<Integer, String> original = originalResults.get(mainClass);
        System.out.println("Original: ");
        System.out.println(original.getValue());
        System.out.println("Exit code: " + original.getKey());

        if (javaVersion.flags.classVersion <= 51) {
            try (ZipFile zf = new ZipFile(ClassRunner.original.toFile())) {
                ZipEntry entry = zf.getEntry(mainClass.replace(".", "/") + ".class");
                try (InputStream is = zf.getInputStream(entry)) {
                    ClassReader cr = new ClassReader(is);
                    if ((cr.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                        mainClass = mainClass + "$jvmdg$StaticDefaults";
                    }
                }
            }
        }

        System.out.println("\n\n");
        System.out.println("Downgraded: ");
        StringBuilder downgradedLog = new StringBuilder();
        Integer ret = JavaRunner.runJarInSubprocess(
            getDowngradedJar(javaVersion),
            new String[]{},
            mainClass,
            Stream.concat(Stream.of(getApiJar(javaVersion), sharedClasses), downgradeClasspath.stream()).collect(Collectors.toSet()),
            Path.of("."),
            Map.of(),
            true,
            List.of(),
            launchersByVersion.get(javaVersion.targetVersion),
            (String it) -> {
                downgradedLog.append(it).append("\n");
                System.out.println(it);
            },
            (String it) -> {
                downgradedLog.append(it).append("\n");
                System.out.println(it);
            }
        );
        System.out.println("\n");
        System.out.println("Exit code: " + ret);

        assert ret != null;
        compareResults(
            mainClass,
            javaVersion,
            original,
            Map.entry(ret, downgradedLog.toString())
        );
    }

    @ParameterizedTest
    @MethodSource({"arguments"})
    @Execution(ExecutionMode.CONCURRENT)
    public void testShade(String mainClass, FlagsAndRunner javaVersion) throws IOException, InterruptedException {
        System.out.println("TEST_SHADE: Running " + mainClass + " on " + javaVersion.readableSlug());
        Map.Entry<Integer, String> original = originalResults.get(mainClass);
        System.out.println("Original: ");
        System.out.println(original.getValue());
        System.out.println("Exit code: " + original.getKey());

        if (javaVersion.flags.classVersion <= 51) {
            try (ZipFile zf = new ZipFile(ClassRunner.original.toFile())) {
                ZipEntry entry = zf.getEntry(mainClass.replace(".", "/") + ".class");
                try (InputStream is = zf.getInputStream(entry)) {
                    ClassReader cr = new ClassReader(is);
                    if ((cr.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                        mainClass = mainClass + "$jvmdg$StaticDefaults";
                    }
                }
            }
        }

        System.out.println("\n\n");
        System.out.println("Shade: ");
        StringBuilder shadedLog = new StringBuilder();
        Integer ret = JavaRunner.runJarInSubprocess(
            getShadedJar(javaVersion),
            new String[]{},
            mainClass,
            Set.of(),
            Path.of("."),
            Map.of(),
            true,
            List.of(),
            launchersByVersion.get(javaVersion.targetVersion),
            (String it) -> {
                shadedLog.append(it).append("\n");
                System.out.println(it);
            },
            (String it) -> {
                shadedLog.append(it).append("\n");
                System.out.println(it);
            }
        );

        System.out.println("\n");
        System.out.println("Exit code: " + ret);

        assert ret != null;
        compareResults(
            mainClass,
            javaVersion,
            original,
            Map.entry(ret, shadedLog.toString())
        );
    }

    @ParameterizedTest
    @MethodSource({"arguments"})
    @Execution(ExecutionMode.CONCURRENT)
    public void testRuntime(String mainClass, FlagsAndRunner javaVersion) throws IOException, InterruptedException {
        System.out.println("TEST_RUNTIME: Running " + mainClass + " on " + javaVersion.readableSlug());
        Map.Entry<Integer, String> original = originalResults.get(mainClass);
        System.out.println("Original: ");
        System.out.println(original.getValue());
        System.out.println("Exit code: " + original.getKey());

        Set<Path> classpathJars = new HashSet<>(Stream.of(System.getProperty("java.class.path").split(":"))
            .map(Path::of)
            .toList());

        System.out.println("\n\n");
        System.out.println("Runtime: ");
        StringBuilder runtimeLog = new StringBuilder();
        Integer ret = JavaRunner.runJarInSubprocess(
            null,
            new String[]{
                "-a",
                javaApi.toString(),
                "--quiet",
                "bootstrap",
                "--classpath",
                ClassRunner.original.toAbsolutePath().toString(),
                "-m",
                mainClass
            },
            "xyz.wagyourtail.jvmdg.cli.Main",
            classpathJars,
            Path.of("."),
            Map.of(),
            true,
            List.of(),
            launchersByVersion.get(javaVersion.targetVersion),
            (String it) -> {
                runtimeLog.append(it).append("\n");
                System.out.println(it);
            },
            (String it) -> {
                runtimeLog.append(it).append("\n");
                System.out.println(it);
            }
        );

        System.out.println("\n");
        System.out.println("Exit code: " + ret);

        assert ret != null;
        compareResults(
            mainClass,
            javaVersion,
            original,
            Map.entry(ret, runtimeLog.toString())
        );
    }

    public static void compareResults(String mainClass, FlagsAndRunner javaVersion, Map.Entry<Integer, String> originalResult, Map.Entry<Integer, String> downgradedResult) {
        assertEquals(originalResult.getKey(), downgradedResult.getKey(), "Exit code mismatch for " + mainClass + " on " + javaVersion.readableSlug());
        assertEquals(originalResult.getValue(), downgradedResult.getValue(), "Output mismatch for " + mainClass + " on " + javaVersion.readableSlug());
    }

    public record FlagsAndRunner(Flags flags, JavaRunner.JavaVersion targetVersion) {

        public String readableSlug() {
            if (flags.debugSkipStubs.isEmpty()) {
                return Integer.toString(targetVersion.getMajorVersion());
            } else {
                return targetVersion.getMajorVersion() + "-fake-" + flags.debugSkipStubs.stream().map(e -> Integer.toString(JavaRunner.JavaVersion.fromOpcode(e).getMajorVersion())).collect(Collectors.joining("-"));
            }
        }

    }

}
