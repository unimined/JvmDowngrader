package xyz.wagyourtail.jvmdg.internal;

import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
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
    private static Properties props = new Properties();
    static {
        try (InputStream is = Files.newInputStream(Path.of("gradle.properties"))) {
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static final Path javaApi = Path.of("./java-api/build/libs/jvmdowngrader-java-api-" + props.getProperty("version") + ".jar");

    static {
        System.setProperty("jvmdg.java-api", javaApi.toString());
    }

    private final JavaRunner.JavaVersion target = JavaRunner.JavaVersion.fromMajor(Integer.parseInt(props.getProperty("testTargetVersion")));

    private final Path mainClasses = Path.of("./build/classes/java/main");

    private final Path original = Path.of("./downgradetest/build/libs/downgradetest-1.0.0.jar");

    private final Path downgraded = getDowngradedPath(original, "-downgraded-" + target.getMajorVersion() + ".jar");
    private final Path downgradedJavaApi = getDowngradedJavaApi(javaApi, "-downgraded-" + target.getMajorVersion() + ".jar");

    private final Path shaded = getShadedPath(downgraded, downgradedJavaApi, "-shaded.jar");

    public JvmDowngraderTest() throws Exception {
    }

    private Path getDowngradedPath(Path originalPath, String suffix) throws Exception {
        Path path = originalPath.getParent().resolve(originalPath.getFileName().toString().replace(".jar", suffix));
        Files.deleteIfExists(path);
        ZipDowngrader.downgradeZip(target.toOpcode(), originalPath, new HashSet<>(), path);
        return path;
    }

    private Path getDowngradedJavaApi(Path javaApi, String suffix) throws IOException {
        // resolve temp file in build
        Path output = Path.of("./build/tmp/test/" + javaApi.getFileName().toString().replace(".jar", suffix));
        Files.deleteIfExists(output);
        ApiShader.downgradedApi(target.toOpcode(), javaApi, output);
        return output;
    }

    private Path getShadedPath(Path originalPath, Path downgradedJavaApi, String suffix) throws IOException {
        Path path = originalPath.getParent().resolve(originalPath.getFileName().toString().replace(".jar", suffix));
        Files.deleteIfExists(path);
        ApiShader.shadeApis("jvmdg/shade/", originalPath, path, downgradedJavaApi);
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
            Utils.getCurrentClassVersion(),
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

        StringBuilder downgradedLog = new StringBuilder();
        Integer ret = JavaRunner.runJarInSubprocess(
            downgraded,
            new String[]{},
            mainClass,
            Set.of(downgradedJavaApi, mainClasses),
            Path.of("."),
            Map.of(),
            true,
            List.of(),
            JavaRunner.JavaVersion.V1_8.toOpcode(),
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
            JavaRunner.JavaVersion.V1_8.toOpcode(),
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
            new String[]{downgraded.toString(), mainClass},
            "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            classpathJars,
            Path.of("."),
            Map.of(),
            true,
            List.of(/*"-Djvmdg.debug=true", */"-Djvmdg.java-api=" + javaApi, "-Djvmdg.log=false", "-Djvmdg.quiet=true"),
            JavaRunner.JavaVersion.V1_8.toOpcode(),
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

}