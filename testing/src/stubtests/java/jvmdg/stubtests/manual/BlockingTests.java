package jvmdg.stubtests.manual;

import jvmdg.stubtests.StubTest;
import jvmdg.stubtests.Support;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Parity tests for blocking/lifecycle members the generator denies because
 * synthesized receivers park forever or race an executor. Each test drives
 * the member to a deterministic outcome — a completed task for the join
 * family, an inline executor or {@code .get()} for async stages, a far-future
 * timeout for the never-fires shape — and only prints the stable result.
 */
public final class BlockingTests {

    private BlockingTests() {
    }

    /** threadId is denied (ambient value); the >0 shape is stable. */
    @StubTest(member = @Ref(value = "java/lang/Thread", member = "threadId",
        desc = "()J"))
    public static void threadId(String id) {
        Support.print(id, new Thread().threadId() > 0);
    }

    /** sleep is denied (blocking); a zero duration returns immediately. */
    @StubTest(member = @Ref(value = "java/lang/Thread", member = "sleep",
        desc = "(Ljava/time/Duration;)V"))
    public static void sleepDuration(String id) throws Exception {
        Thread.sleep(java.time.Duration.ZERO);
        Support.print(id, "slept");
    }

    /** join is denied (blocking); a finished thread returns immediately. */
    @StubTest(member = @Ref(value = "java/lang/Thread", member = "join",
        desc = "(Ljava/time/Duration;)Z"))
    public static void joinDuration(String id) throws Exception {
        Thread t = new Thread(() -> {
        });
        t.start();
        t.join();
        t.join(java.time.Duration.ofDays(1));
        Support.print(id, "joined");
    }

    /** quietlyJoin parks on an unforked task; a completed task returns true. */
    @StubTest(member = @Ref(value = "java/util/concurrent/ForkJoinTask",
        member = "quietlyJoin", desc = "(JLjava/util/concurrent/TimeUnit;)Z"))
    public static void quietlyJoin(String id) throws Exception {
        java.util.concurrent.ForkJoinTask<?> t =
            java.util.concurrent.ForkJoinTask.adapt(() -> {
            });
        t.invoke();
        Support.print(id, t.quietlyJoin(1, TimeUnit.DAYS));
    }

    /** quietlyJoinUninterruptibly — same completed-task fixture. */
    @StubTest(member = @Ref(value = "java/util/concurrent/ForkJoinTask",
        member = "quietlyJoinUninterruptibly",
        desc = "(JLjava/util/concurrent/TimeUnit;)Z"))
    public static void quietlyJoinUninterruptibly(String id) {
        java.util.concurrent.ForkJoinTask<?> t =
            java.util.concurrent.ForkJoinTask.adapt(() -> {
            });
        t.invoke();
        Support.print(id, t.quietlyJoinUninterruptibly(1, TimeUnit.DAYS));
    }

    /** completeAsync races the executor; .get() makes the result deterministic. */
    @StubTest(member = @Ref(value = "java/util/concurrent/CompletableFuture",
        member = "completeAsync",
        desc = "(Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;"))
    public static void completeAsync(String id) throws Exception {
        CompletableFuture<String> cf = new CompletableFuture<>();
        cf.completeAsync(() -> "v");
        Support.print(id, cf.get());
    }

    /** completeAsync(supplier, executor) — inline executor is deterministic. */
    @StubTest(member = @Ref(value = "java/util/concurrent/CompletableFuture",
        member = "completeAsync",
        desc = "(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)"
            + "Ljava/util/concurrent/CompletableFuture;"))
    public static void completeAsyncExecutor(String id) throws Exception {
        CompletableFuture<String> cf = new CompletableFuture<>();
        cf.completeAsync(() -> "v", (Executor) Runnable::run);
        Support.print(id, cf.get());
    }

    /** completeOnTimeout races the delayer; a far-future timeout never fires. */
    @StubTest(member = @Ref(value = "java/util/concurrent/CompletableFuture",
        member = "completeOnTimeout",
        desc = "(Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)"
            + "Ljava/util/concurrent/CompletableFuture;"))
    public static void completeOnTimeout(String id) {
        CompletableFuture<String> cf = new CompletableFuture<>();
        cf.completeOnTimeout("v", 1, TimeUnit.DAYS);
        Support.print(id, cf.isDone());
    }

    /** orTimeout — same far-future never-fires shape. */
    @StubTest(member = @Ref(value = "java/util/concurrent/CompletableFuture",
        member = "orTimeout",
        desc = "(JLjava/util/concurrent/TimeUnit;)"
            + "Ljava/util/concurrent/CompletableFuture;"))
    public static void orTimeout(String id) {
        CompletableFuture<String> cf = new CompletableFuture<>();
        cf.orTimeout(1, TimeUnit.DAYS);
        Support.print(id, cf.isDone());
    }

    /** exceptionallyAsync on a pre-completed stage: .get() pins the result. */
    @StubTest(member = @Ref(value = "java/util/concurrent/CompletionStage",
        member = "exceptionallyAsync",
        desc = "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletionStage;"))
    public static void exceptionallyAsync(String id) throws Exception {
        CompletionStage<String> cs = CompletableFuture.completedFuture("ok");
        Support.print(id, cs.exceptionallyAsync(t -> "alt").toCompletableFuture().get());
    }

    /** exceptionallyAsync(fn, executor) — inline-executor variant. */
    @StubTest(member = @Ref(value = "java/util/concurrent/CompletionStage",
        member = "exceptionallyAsync",
        desc = "(Ljava/util/function/Function;Ljava/util/concurrent/Executor;)"
            + "Ljava/util/concurrent/CompletionStage;"))
    public static void exceptionallyAsyncExecutor(String id) throws Exception {
        CompletionStage<String> cs = CompletableFuture.completedFuture("ok");
        Support.print(id, cs.exceptionallyAsync(t -> "alt",
            (Executor) Runnable::run).toCompletableFuture().get());
    }

    /** exceptionallyComposeAsync — composed-stage variant. */
    @StubTest(member = @Ref(value = "java/util/concurrent/CompletionStage",
        member = "exceptionallyComposeAsync",
        desc = "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletionStage;"))
    public static void exceptionallyComposeAsync(String id) throws Exception {
        CompletionStage<String> cs = CompletableFuture.completedFuture("ok");
        Support.print(id, cs.exceptionallyComposeAsync(
            t -> CompletableFuture.completedFuture("alt")).toCompletableFuture().get());
    }

    /** exceptionallyComposeAsync(fn, executor) — inline-executor variant. */
    @StubTest(member = @Ref(value = "java/util/concurrent/CompletionStage",
        member = "exceptionallyComposeAsync",
        desc = "(Ljava/util/function/Function;Ljava/util/concurrent/Executor;)"
            + "Ljava/util/concurrent/CompletionStage;"))
    public static void exceptionallyComposeAsyncExecutor(String id) throws Exception {
        CompletionStage<String> cs = CompletableFuture.completedFuture("ok");
        Support.print(id, cs.exceptionallyComposeAsync(
            t -> CompletableFuture.completedFuture("alt"),
            (Executor) Runnable::run).toCompletableFuture().get());
    }

    /** select(Consumer) is denied (blocking); a ready pipe key returns 1. */
    @StubTest(member = @Ref(value = "java/nio/channels/Selector", member = "select",
        desc = "(Ljava/util/function/Consumer;)I"))
    public static void selectConsumer(String id) throws Exception {
        java.nio.channels.Selector sel = java.nio.channels.Selector.open();
        java.nio.channels.Pipe p = java.nio.channels.Pipe.open();
        try {
            p.source().configureBlocking(false);
            p.source().register(sel, java.nio.channels.SelectionKey.OP_READ);
            p.sink().write(java.nio.ByteBuffer.wrap(new byte[]{1}));
            Support.print(id, sel.select(k -> {
            }));
        } finally {
            p.sink().close();
            p.source().close();
            sel.close();
        }
    }

    /** select(Consumer, long) — timeout variant of the ready-key fixture. */
    @StubTest(member = @Ref(value = "java/nio/channels/Selector", member = "select",
        desc = "(Ljava/util/function/Consumer;J)I"))
    public static void selectConsumerTimeout(String id) throws Exception {
        java.nio.channels.Selector sel = java.nio.channels.Selector.open();
        java.nio.channels.Pipe p = java.nio.channels.Pipe.open();
        try {
            p.source().configureBlocking(false);
            p.source().register(sel, java.nio.channels.SelectionKey.OP_READ);
            p.sink().write(java.nio.ByteBuffer.wrap(new byte[]{1}));
            Support.print(id, sel.select(k -> {
            }, 1000));
        } finally {
            p.sink().close();
            p.source().close();
            sel.close();
        }
    }

    /** open(ProtocolFamily) is denied (real socket); open-without-bind is stable. */
    @StubTest(member = @Ref(value = "java/nio/channels/SocketChannel", member = "open",
        desc = "(Ljava/net/ProtocolFamily;)Ljava/nio/channels/SocketChannel;"))
    public static void socketChannelOpen(String id) throws Exception {
        try (java.nio.channels.SocketChannel ch =
                 java.nio.channels.SocketChannel.open(
                     java.net.StandardProtocolFamily.INET)) {
            Support.print(id, ch.isOpen());
        }
    }

    /** ServerSocketChannel.open(ProtocolFamily) — same open-without-bind shape. */
    @StubTest(member = @Ref(value = "java/nio/channels/ServerSocketChannel",
        member = "open",
        desc = "(Ljava/net/ProtocolFamily;)Ljava/nio/channels/ServerSocketChannel;"))
    public static void serverSocketChannelOpen(String id) throws Exception {
        try (java.nio.channels.ServerSocketChannel ch =
                 java.nio.channels.ServerSocketChannel.open(
                     java.net.StandardProtocolFamily.INET)) {
            Support.print(id, ch.isOpen());
        }
    }

    /** shutdown is denied (lifecycle); on a fresh client it's a fixed marker. */
    @StubTest(member = @Ref(value = "java/net/http/HttpClient", member = "shutdown",
        desc = "()V"))
    public static void httpShutdown(String id) {
        java.net.http.HttpClient c = java.net.http.HttpClient.newHttpClient();
        c.shutdown();
        Support.print(id, "down");
    }

    /** shutdownNow — same fresh-client lifecycle marker. */
    @StubTest(member = @Ref(value = "java/net/http/HttpClient", member = "shutdownNow",
        desc = "()V"))
    public static void httpShutdownNow(String id) {
        java.net.http.HttpClient c = java.net.http.HttpClient.newHttpClient();
        c.shutdownNow();
        Support.print(id, "down");
    }
}
