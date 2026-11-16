package xyz.wagyourtail.jvmdg.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.wagyourtail.jvmdg.test.JavaRunner;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end {@code -javaagent} scenario: an application compiled for Java 11
 * that calls a Java 11 API ({@code String.isBlank}) must run on a Java 8 JVM
 * once the JvmDowngrader agent is attached.
 *
 * <p>The agent jar is located via the {@code jvmdg.test.agentJar} system
 * property, falling back to the {@code *-all.jar} in {@code build/libs}.</p>
 */
public class TestAgentSubprocess {

    private static Path findAgentJar() throws IOException {
        String prop = System.getProperty("jvmdg.test.agentJar");
        if (prop != null) return Paths.get(prop);
        Path libs = Paths.get("build", "libs");
        if (Files.isDirectory(libs)) {
            List<Path> candidates = new ArrayList<>();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(libs, "*-all.jar")) {
                for (Path p : ds) candidates.add(p);
            }
            if (!candidates.isEmpty()) return candidates.get(0);
        }
        return null;
    }

    private static Path java8Binary() {
        String prop = System.getProperty("jvmdg.test.java8");
        if (prop != null) return Paths.get(prop);
        // gradle toolchain cache layout
        Path jdks = Paths.get(System.getProperty("user.home"), ".gradle", "jdks");
        if (Files.isDirectory(jdks)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(jdks, "*-8-amd64-linux*")) {
                for (Path p : ds) {
                    Path bin = p.resolve("bin/java");
                    if (Files.exists(bin)) return bin;
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static Path java11Compiler() {
        String prop = System.getProperty("jvmdg.test.java11javac");
        if (prop != null) return Paths.get(prop);
        Path jdks = Paths.get(System.getProperty("user.home"), ".gradle", "jdks");
        if (Files.isDirectory(jdks)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(jdks, "*-11-amd64-linux*")) {
                for (Path p : ds) {
                    Path bin = p.resolve("bin/javac");
                    if (Files.exists(bin)) return bin;
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    @Test
    public void javaAgentDowngradesJava11AppOnJdk8(@TempDir Path dir) throws Exception {
        Path agentJar = findAgentJar();
        Path java8 = java8Binary();
        Path javac11 = java11Compiler();
        if (agentJar == null) {
            System.out.println("SKIP: no -all agent jar found (set -Djvmdg.test.agentJar)");
            return;
        }
        if (java8 == null || javac11 == null) {
            System.out.println("SKIP: required JDKs not installed (set jvmdg.test.java8 / jvmdg.test.java11javac)");
            return;
        }

        // app compiled for Java 11 using a Java 11 API
        Path src = dir.resolve("src");
        Files.createDirectories(src);
        Files.write(src.resolve("AgentApp.java"),
            ("public class AgentApp {\n" +
             "    public static void main(String[] args) {\n" +
             "        System.out.println(\"RESULT:\" + \"  \".isBlank());\n" +
             "        for (String a : args) System.out.println(\"ARG:\" + a);\n" +
             "    }\n" +
             "}\n").getBytes("UTF-8"));
        Process compile = new ProcessBuilder(
            javac11.toString(), "-d", dir.resolve("app").toString(),
            src.resolve("AgentApp.java").toString())
            .redirectErrorStream(true).start();
        String compileOut = new String(
            xyz.wagyourtail.jvmdg.util.Utils.readAllBytes(compile.getInputStream()), "UTF-8");
        assertEquals(0, compile.waitFor(), "app compile failed: " + compileOut);

        // jvmdg classes resolve via the agent jar itself; ASM must come from
        // the application classpath
        java.util.Set<Path> appCp = new java.util.HashSet<>();
        appCp.add(dir.resolve("app"));
        for (String e : System.getProperty("java.class.path").split(
                java.io.File.pathSeparator)) {
            if (e.contains("asm")) appCp.add(Paths.get(e));
        }

        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        Integer code = JavaRunner.runJarInSubprocess(
            null,
            new String[0],
            "AgentApp",
            appCp,
            dir,
            new HashMap<String, String>(),
            true,
            Arrays.asList("-javaagent:" + agentJar.toAbsolutePath(), "-Djvmdg.quiet=false"),
            java8,
            out::append, err::append);

        assertNotNull(code);
        assertEquals(0, code.intValue(),
            "agent subprocess failed\nstdout: " + out + "\nstderr: " + err);
        assertTrue(out.toString().contains("RESULT:true"),
            "expected Java-11 API to be stubbed; output was: " + out);
    }

    @Test
    public void bootstrapMainPreservesArgCase(@TempDir Path dir) throws Exception {
        Path agentJar = findAgentJar();
        Path java8 = java8Binary();
        Path javac11 = java11Compiler();
        if (agentJar == null || java8 == null || javac11 == null) {
            System.out.println("SKIP: agent jar or required JDKs missing");
            return;
        }

        Path src = dir.resolve("src");
        Files.createDirectories(src);
        Files.write(src.resolve("AgentApp.java"),
            ("public class AgentApp {\n" +
             "    public static void main(String[] args) {\n" +
             "        for (String a : args) System.out.println(\"ARG:\" + a);\n" +
             "    }\n" +
             "}\n").getBytes("UTF-8"));
        Process compile = new ProcessBuilder(
            javac11.toString(), "-d", dir.resolve("app").toString(),
            src.resolve("AgentApp.java").toString())
            .redirectErrorStream(true).start();
        assertEquals(0, compile.waitFor(), "app compile failed");

        // Bootstrap runs the app through the downgrading classloader; jvmdg
        // + bundled ASM come from the agent jar on the classpath
        java.util.Set<Path> cp = new java.util.HashSet<>();
        cp.add(agentJar.toAbsolutePath());
        for (String e : System.getProperty("java.class.path").split(
                java.io.File.pathSeparator)) {
            if (e.contains("annotations")) cp.add(Paths.get(e));
        }

        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        Integer code = JavaRunner.runJarInSubprocess(
            null,
            new String[]{
                dir.resolve("app").toAbsolutePath().toString(),
                "AgentApp",
                "MixedCase_Arg1",
                "UPPER",
            },
            "xyz.wagyourtail.jvmdg.runtime.Bootstrap",
            cp,
            dir,
            new HashMap<String, String>(),
            true,
            Arrays.asList("-Djvmdg.quiet=true"),
            java8,
            out::append, err::append);

        assertNotNull(code);
        assertEquals(0, code.intValue(),
            "bootstrap subprocess failed\nstdout: " + out + "\nstderr: " + err);
        assertTrue(out.toString().contains("ARG:MixedCase_Arg1"),
            "mixed-case arg must reach main unmodified; output: " + out);
        assertTrue(out.toString().contains("ARG:UPPER"),
            "uppercase arg must reach main unmodified; output: " + out);
    }
}
