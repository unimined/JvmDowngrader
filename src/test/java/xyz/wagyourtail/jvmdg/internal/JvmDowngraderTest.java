package xyz.wagyourtail.jvmdg.internal;

import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader;
import xyz.wagyourtail.jvmdg.test.JavaRunner;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class JvmDowngraderTest {
    private static final Path javaApi = Path.of("./java-api/build/libs/jvmdowngrader-java-api-" + System.getProperty("jvmdg.test.version") + ".jar");

    private final Flags flags = new Flags();

    private final JavaRunner.JavaVersion target = JavaRunner.JavaVersion.fromMajor(Integer.parseInt(System.getProperty("jvmdg.test.javaVersion")));

    private final Path mainClasses = Path.of("./build/classes/java/main");

    private final Path original = Path.of("./downgradetest/build/libs/downgradetest-1.0.0.jar");
    private final Path downgraded;
    private final Path downgradedJavaApi;

    private final Path shaded;

//        System.setProperty("jvmdg.java-api", javaApi.toString());


    public JvmDowngraderTest() throws Exception {
        flags.api = javaApi.toFile();
        flags.classVersion = target.toOpcode();
        downgraded = getDowngradedPath(original, "-downgraded-" + target.getMajorVersion() + ".jar");
        downgradedJavaApi = getDowngradedJavaApi(javaApi, "-downgraded-" + target.getMajorVersion() + ".jar");
        shaded = getShadedPath(downgraded, downgradedJavaApi, "-shaded.jar");
    }

    private Path getDowngradedPath(Path originalPath, String suffix) throws Exception {
        Path path = originalPath.getParent().resolve(originalPath.getFileName().toString().replace(".jar", suffix));
        Files.deleteIfExists(path);
        ZipDowngrader.downgradeZip(ClassDowngrader.downgradeTo(flags), originalPath, new HashSet<>(), path);
        return path;
    }

    private Path getDowngradedJavaApi(Path javaApi, String suffix) throws IOException {
        // resolve temp file in build
        Path output = Path.of("./build/tmp/test/" + javaApi.getFileName().toString().replace(".jar", suffix));
        Files.deleteIfExists(output);
        ApiShader.downgradedApi(flags, javaApi, output);
        return output;
    }

    private Path getShadedPath(Path originalPath, Path downgradedJavaApi, String suffix) throws IOException {
        Path path = originalPath.getParent().resolve(originalPath.getFileName().toString().replace(".jar", suffix));
        Files.deleteIfExists(path);
        ApiShader.shadeApis(null, "jvmdg/shade/", originalPath.toFile(), path.toFile(), downgradedJavaApi.toFile());
        return path;
    }

    private void testDowngrade(String mainClass) throws Exception {
        testDowngrade(mainClass, true);
    }

    private void testDowngrade(String mainClass, boolean eq) throws Exception {
        System.out.println();
        System.out.println("Original: ");

        StringBuilder originalLog = new StringBuilder();

        Integer ret2 = JavaRunner.runJarInSubprocess(
                original,
                new String[]{},
                mainClass.replace("$jvmdg$StaticDefaults", ""),
                Set.of(),
                Path.of("."),
                Map.of(),
                true,
                List.of(),
                Path.of(System.getProperty("jvmdg.test.jvm")),
                (String it) -> {
                    originalLog.append(it).append("\n");
                    System.out.println(it);
                },
                (String it) -> {
                    originalLog.append(it).append("\n");
                    System.out.println(it);
                }
        );

        // only java <= 7 moves interface statics
        if (target.ordinal() > JavaRunner.JavaVersion.V1_7.ordinal()) {
            mainClass = mainClass.replace("$jvmdg$StaticDefaults", "");
        }

        System.out.println();
        System.out.println("Downgraded: ");

        Set<Path> classpath = new HashSet<>(Stream.of(System.getProperty("java.class.path").split(":"))
                .map(Path::of)
                .toList());
        classpath.add(downgradedJavaApi);
        classpath.add(mainClasses);

        StringBuilder downgradedLog = new StringBuilder();
        Integer ret = JavaRunner.runJarInSubprocess(
                downgraded,
                new String[]{},
                mainClass,
                classpath,
                Path.of("."),
                Map.of(),
                true,
                List.of(),
                Path.of(System.getProperty("jvmdg.test.targetJvm")),
                (String it) -> {
                    downgradedLog.append(it).append("\n");
                    System.out.println(it);
                },
                (String it) -> {
                    downgradedLog.append(it).append("\n");
                    System.out.println(it);
                }
        );

        System.out.println();
        System.out.println("Shaded: ");

        StringBuilder shadedLog = new StringBuilder();
        Integer ret4 = JavaRunner.runJarInSubprocess(
                shaded,
                new String[]{},
                mainClass,
                Set.of(),
                Path.of("."),
                Map.of(),
                true,
                List.of(),
                Path.of(System.getProperty("jvmdg.test.targetJvm")),
                (String it) -> {
                    shadedLog.append(it).append("\n");
                    System.out.println(it);
                },
                (String it) -> {
                    shadedLog.append(it).append("\n");
                    System.out.println(it);
                }
        );

        Set<Path> classpathJars = new HashSet<>(Stream.of(System.getProperty("java.class.path").split(":"))
                .map(Path::of)
                .toList());

        System.out.println();
        System.out.println("Runtime Downgraded: ");

        StringBuilder runtimeDowngradeLog = new StringBuilder();

        Integer ret3 = JavaRunner.runJarInSubprocess(
                null,
                new String[]{
                        "-a",
                        javaApi.toString(),
                        "--quiet",
                        "bootstrap",
                        "--classpath",
                        original.toString(),
                        "-m",
                        mainClass
                },
                "xyz.wagyourtail.jvmdg.cli.Main",
                classpathJars,
                Path.of("."),
                Map.of(),
                true,
                List.of(/*"-Djvmdg.debug=true", "-Djvmdg.java-api=" + javaApi, "-Djvmdg.log=false", "-Djvmdg.quiet=true" */),
                Path.of(System.getProperty("jvmdg.test.targetJvm")),
                (String it) -> {
                    runtimeDowngradeLog.append(it).append("\n");
                    System.out.println(it);
                },
                (String it) -> {
                    runtimeDowngradeLog.append(it).append("\n");
                    System.out.println(it);
                }
        );

        if (ret2 != 0) {
            throw new Exception("Original jar did not return 0");
        }
        if (ret != 0) {
            throw new Exception("Downgraded jar did not return 0");
        }
        if (ret4 != 0) {
            throw new Exception("Shaded jar did not return 0");
        }
        if (ret3 != 0) {
            throw new Exception("Runtime Downgraded jar did not return 0");
        }

        if (eq) {
            assertEquals(originalLog.toString(), downgradedLog.toString());
            assertEquals(originalLog.toString(), runtimeDowngradeLog.toString());
            assertEquals(originalLog.toString(), shadedLog.toString());
        } else {
            assertNotEquals(originalLog.toString(), downgradedLog.toString());
            assertNotEquals(originalLog.toString(), runtimeDowngradeLog.toString());
            assertNotEquals(originalLog.toString(), shadedLog.toString());
        }
    }

    @Test
    public void testBuffer() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestBuffer");
    }

    @Test
    public void testDowngradeRecord() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestRecord");
    }

    @Test
    public void testDowngradeString() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestString");
    }

    @Test
    public void testDowngradeInterface() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestInterface$jvmdg$StaticDefaults");
    }

    @Test
    public void testSeal() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestSeal");
    }

    @Test
    public void testFilter() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestFilter");
    }

    @Test
    public void testException() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestException");
    }

    @Test
    public void testStream() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestStream");
    }

    @Test
    public void testClass() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestClass");
    }

    @Test
    public void testFuture() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestFuture");
    }

    @Test
    public void testLambda() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestLambda");
    }

    @Test
    public void testNests() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestNests");
    }

    @Test
    public void testStackWalker() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestStackWalker");
    }

    @Test
    public void testVersion() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestVersion", false);
    }

    @Test
    public void testWasAbstract() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestWasAbstract");
    }

    @Test
    public void testSwitch() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestSwitch");
    }

    @Test
    public void testRandom() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestRandom");
    }

    @Test
    public void testFile() throws Exception {
        testDowngrade("xyz.wagyourtail.downgradetest.TestFile");
    }

}