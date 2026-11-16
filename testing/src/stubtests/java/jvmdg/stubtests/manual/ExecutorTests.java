package jvmdg.stubtests.manual;

import jvmdg.stubtests.StubTest;
import jvmdg.stubtests.Support;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Parity tests for the {@code Executors} per-task executor factories — the
 * generator denies them (returned identity is ambient and the executor races
 * any synthesized read-back), so each probe drives the executor to a
 * deterministic outcome: a submitted task's return value, plus the
 * thread-shape fact ({@code Thread.currentThread().isVirtual()} inside the
 * task) that distinguishes the virtual from the platform variant.
 */
public final class ExecutorTests {

    private ExecutorTests() {
    }

    /** The virtual variant must run the task on a virtual thread — true on
     * native j21+, true under the FakeVirtualThread-backed stub. */
    @StubTest(member = @Ref(value = "java/util/concurrent/Executors",
        member = "newVirtualThreadPerTaskExecutor",
        desc = "()Ljava/util/concurrent/ExecutorService;"))
    public static void newVirtualThreadPerTaskExecutor(String id) throws Exception {
        ExecutorService es = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        boolean virtual = es.submit(() -> Thread.currentThread().isVirtual()).get();
        es.shutdown();
        Support.print(id, virtual && es.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS));
    }

    /** The platform variant must run the task on a factory-built thread —
     * naming the factory makes the read-back deterministic. */
    @StubTest(member = @Ref(value = "java/util/concurrent/Executors",
        member = "newThreadPerTaskExecutor",
        desc = "(Ljava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ExecutorService;"))
    public static void newThreadPerTaskExecutor(String id) throws Exception {
        ThreadFactory factory = r -> new Thread(r, "per-task-probe");
        ExecutorService es = java.util.concurrent.Executors.newThreadPerTaskExecutor(factory);
        String name = es.submit(() -> Thread.currentThread().getName()).get();
        es.shutdown();
        Support.print(id, name + ":" + es.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS));
    }

}
