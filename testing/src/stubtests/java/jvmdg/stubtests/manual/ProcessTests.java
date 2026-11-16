package jvmdg.stubtests.manual;

import jvmdg.stubtests.StubTest;
import jvmdg.stubtests.Support;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Parity tests over real subprocesses. The generator denies the whole
 * {@code java.lang.Process}/{@code ProcessBuilder} owners because synthesized
 * receivers can't exist — each test here spawns a controlled child ({@code sh}
 * / {@code cmd} shell scripts: short-lived {@code echo}, interactive
 * {@code cat}/{@code more} for pipe tests) and prints only shape-stable
 * results (booleans, deterministic output, marker strings) — never pids,
 * commands, or platform-specific error text.
 */
public final class ProcessTests {

    private ProcessTests() {
    }

    private static ProcessBuilder shell(String script) {
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        return new ProcessBuilder(win ? "cmd" : "sh", win ? "/c" : "-c", script);
    }

    private static ProcessBuilder passthrough() {
        return shell(System.getProperty("os.name", "").toLowerCase().contains("win")
            ? "more" : "cat");
    }

    private static void kill(Process p) {
        try {
            p.destroyForcibly();
            p.waitFor();
        } catch (Throwable ignored) {
        }
    }

    /** Process.pid() reports a per-process id — printed as a shape check. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "pid", desc = "()J"))
    public static void pid(String id) throws Exception {
        Process p = passthrough().start();
        try {
            Support.print(id, p.pid() > 0);
        } finally {
            kill(p);
        }
    }

    /** Process.isAlive() — deterministic on a live child. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "isAlive", desc = "()Z"))
    public static void isAlive(String id) throws Exception {
        Process p = passthrough().start();
        try {
            Support.print(id, p.isAlive());
        } finally {
            kill(p);
        }
    }

    /** Process.children() — live-process stream; only the non-negative count is stable. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "children",
        desc = "()Ljava/util/stream/Stream;"))
    public static void children(String id) throws Exception {
        Process p = passthrough().start();
        try {
            Support.print(id, p.children().count() >= 0);
        } finally {
            kill(p);
        }
    }

    /** Process.descendants() — same live-process reasoning as children(). */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "descendants",
        desc = "()Ljava/util/stream/Stream;"))
    public static void descendants(String id) throws Exception {
        Process p = passthrough().start();
        try {
            Support.print(id, p.descendants().count() >= 0);
        } finally {
            kill(p);
        }
    }

    /** Process.info() — presence is the stable property across providers. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "info",
        desc = "()Ljava/lang/ProcessHandle$Info;"))
    public static void info(String id) throws Exception {
        Process p = passthrough().start();
        try {
            Support.print(id, p.info() != null);
        } finally {
            kill(p);
        }
    }

    /** Process.toHandle() — the handle's pid relation is the stable property. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "toHandle",
        desc = "()Ljava/lang/ProcessHandle;"))
    public static void toHandle(String id) throws Exception {
        Process p = passthrough().start();
        try {
            Support.print(id, p.toHandle().pid() == p.pid());
        } finally {
            kill(p);
        }
    }

    /** Process.inputReader() — reads the child's deterministic stdout. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "inputReader",
        desc = "()Ljava/io/BufferedReader;"))
    public static void inputReader(String id) throws Exception {
        Process p = shell("echo reader-line").start();
        try {
            Support.print(id, p.inputReader().readLine());
        } finally {
            kill(p);
        }
    }

    /** Process.inputReader(Charset) — charset-decoding variant. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "inputReader",
        desc = "(Ljava/nio/charset/Charset;)Ljava/io/BufferedReader;"))
    public static void inputReaderCharset(String id) throws Exception {
        Process p = shell("echo reader-cs").start();
        try {
            Support.print(id, p.inputReader(StandardCharsets.UTF_8).readLine());
        } finally {
            kill(p);
        }
    }

    /** Process.errorReader() — a guaranteed-stderr script keeps it deterministic. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "errorReader",
        desc = "()Ljava/io/BufferedReader;"))
    public static void errorReader(String id) throws Exception {
        Process p = shell("echo err-line 1>&2").start();
        try {
            Support.print(id, p.errorReader().readLine());
        } finally {
            kill(p);
        }
    }

    /** Process.errorReader(Charset) — charset-decoding variant. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "errorReader",
        desc = "(Ljava/nio/charset/Charset;)Ljava/io/BufferedReader;"))
    public static void errorReaderCharset(String id) throws Exception {
        Process p = shell("echo err-cs 1>&2").start();
        try {
            Support.print(id, p.errorReader(StandardCharsets.UTF_8).readLine());
        } finally {
            kill(p);
        }
    }

    /** Process.outputWriter() — stdin write round-trips through cat's echo. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "outputWriter",
        desc = "()Ljava/io/BufferedWriter;"))
    public static void outputWriter(String id) throws Exception {
        Process p = passthrough().start();
        try {
            java.io.BufferedWriter w = p.outputWriter();
            w.write("out-line\n");
            w.flush();
            Support.print(id, p.inputReader().readLine());
        } finally {
            kill(p);
        }
    }

    /** Process.outputWriter(Charset) — charset-encoding variant. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "outputWriter",
        desc = "(Ljava/nio/charset/Charset;)Ljava/io/BufferedWriter;"))
    public static void outputWriterCharset(String id) throws Exception {
        Process p = passthrough().start();
        try {
            java.io.BufferedWriter w = p.outputWriter(StandardCharsets.UTF_8);
            w.write("out-cs\n");
            w.flush();
            Support.print(id, p.inputReader().readLine());
        } finally {
            kill(p);
        }
    }

    /** Process.waitFor(long, TimeUnit) — a short-lived child completes. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "waitFor",
        desc = "(JLjava/util/concurrent/TimeUnit;)Z"))
    public static void waitForTimeout(String id) throws Exception {
        Process p = shell("echo done").start();
        try {
            Support.print(id, p.waitFor(30, TimeUnit.SECONDS));
        } finally {
            kill(p);
        }
    }

    /** Process.waitFor(Duration) — duration variant of the same. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "waitFor",
        desc = "(Ljava/time/Duration;)Z"))
    public static void waitForDuration(String id) throws Exception {
        Process p = shell("echo done").start();
        try {
            Support.print(id, p.waitFor(java.time.Duration.ofSeconds(30)));
        } finally {
            kill(p);
        }
    }

    /** Process.destroyForcibly() — liveness flips deterministically. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "destroyForcibly",
        desc = "()Ljava/lang/Process;"))
    public static void destroyForcibly(String id) throws Exception {
        Process p = passthrough().start();
        p.destroyForcibly();
        p.waitFor();
        Support.print(id, p.isAlive());
    }

    /** Process.close() — releases the child's pipes. */
    @StubTest(member = @Ref(value = "java/lang/Process", member = "close", desc = "()V"))
    public static void close(String id) throws Exception {
        Process p = passthrough().start();
        try {
            p.close();
            Support.print(id, "closed");
        } finally {
            kill(p);
        }
    }

    /** ProcessBuilder.startPipeline — a two-stage echo|cat pipe has stable output. */
    @StubTest(member = @Ref(value = "java/lang/ProcessBuilder", member = "startPipeline",
        desc = "(Ljava/util/List;)Ljava/util/List;"))
    public static void startPipeline(String id) throws Exception {
        java.util.List<Process> ps = ProcessBuilder.startPipeline(java.util.Arrays.asList(
            shell("echo piped"), passthrough()));
        try {
            Support.print(id, ps.get(ps.size() - 1).inputReader().readLine());
        } finally {
            for (Process p : ps) {
                kill(p);
            }
        }
    }

    /** Runtime.version() — the stub emulates from the live runtime so
     * feature() agrees with the native call on the same JVM. */
    @StubTest(member = @Ref(value = "java/lang/Runtime", member = "version",
        desc = "()Ljava/lang/Runtime$Version;"))
    public static void runtimeVersion(String id) {
        Support.print(id, Runtime.version().feature() > 0);
    }

    /** RuntimeMXBean.getPid() — per-JVM value, printed as a shape check. */
    @StubTest(member = @Ref(value = "java/lang/management/RuntimeMXBean",
        member = "getPid", desc = "()J"))
    public static void mxBeanPid(String id) {
        Support.print(id, java.lang.management.ManagementFactory.getRuntimeMXBean().getPid() > 0);
    }
}
