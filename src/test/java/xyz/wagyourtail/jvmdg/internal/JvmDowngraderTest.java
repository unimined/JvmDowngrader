package xyz.wagyourtail.jvmdg.internal;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.compile.ZipDowngrader;
import xyz.wagyourtail.jvmdg.test.JavaRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class JvmDowngraderTest {
    private static final Path javaApi = Path.of("./java-api/build/libs/jvmdowngrader-java-api-0.0.1.jar");

    static {
        System.setProperty("jvmdg.java-api", javaApi.toString());
    }

    private final JavaRunner.JavaVersion target = JavaRunner.JavaVersion.V1_8;

    private final Path original = Path.of("./downgradetest/build/libs/downgradetest-1.0.0.jar");

    private final Path downgraded = getDowngradedPath(original, "-downgraded-" + target.getMajorVersion() + ".jar");

    private final Path downgradedJavaApi = getDowngradedPath(javaApi, "-downgraded-" + target.getMajorVersion() + ".jar");

    public JvmDowngraderTest() throws Exception {
    }

    @BeforeAll
    public static void before() throws Exception {
        new JvmDowngraderTest().cleanup();
    }

    public void cleanup() throws Exception {
        if (Files.exists(downgraded)) {
            Files.delete(downgraded);
        }
        if (Files.exists(downgradedJavaApi)) {
            Files.delete(downgradedJavaApi);
        }
    }

    private Path getDowngradedPath(Path originalPath, String suffix) throws Exception {
        Path path = originalPath.getParent().resolve(originalPath.getFileName().toString().replace(".jar", suffix));
        if (!Files.exists(path)) {
            ZipDowngrader.downgradeZip(target.toOpcode(), originalPath, new HashSet<>(), path);
        }
        return path;
    }

    private void testDowngrade(String mainClass) throws Exception {
        System.out.println();
        System.out.println("Original: ");

        StringBuilder originalLog = new StringBuilder();

        Integer ret2 = JavaRunner.runJarInSubprocess(
            original,
            new String[] {},
            mainClass,
            Set.of(),
            Path.of("."),
            Map.of(),
            true,
            List.of(),
            VersionProvider.getCurrentClassVersion(),
            (String it) -> {
                originalLog.append(it).append("\n");
                System.out.println(it);
            },
            (String it) -> {
                originalLog.append(it).append("\n");
                System.out.println(it);
            }
        );

        System.out.println();
        System.out.println("Downgraded: ");

        StringBuilder downgradedLog = new StringBuilder();
        Integer ret = JavaRunner.runJarInSubprocess(
            downgraded,
            new String[] {},
            mainClass,
            Set.of(downgradedJavaApi),
            Path.of("."),
            Map.of(),
            true,
            List.of(),
            target.toOpcode(),
            (String it) -> {
                downgradedLog.append(it).append("\n");
                System.out.println(it);
            },
            (String it) -> {
                downgradedLog.append(it).append("\n");
                System.out.println(it);
            }
        );

        Set<Path> classpathJars = new HashSet<>(Stream.of(System.getProperty("java.class.path").split(":"))
            .map(Path::of)
            .toList());

        StringBuilder runtimeDowngradeLog = new StringBuilder();

        Integer ret3 = JavaRunner.runJarInSubprocess(
            null,
            new String[] {downgraded.toString(), mainClass},
            "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            classpathJars,
            Path.of("."),
            Map.of(),
            true,
            List.of(/*"-Djvmdg.debug=true", */"-Djvmdg.java-api=" + javaApi, "-Djvmdg.log=false"),
            target.toOpcode(),
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
        if (ret3 != 0) {
            throw new Exception("Runtime Downgraded jar did not return 0");
        }

        assertEquals(originalLog.toString(), downgradedLog.toString());
        assertEquals(originalLog.toString(), runtimeDowngradeLog.toString());
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
        testDowngrade("xyz.wagyourtail.downgradetest.TestInterface");
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
}