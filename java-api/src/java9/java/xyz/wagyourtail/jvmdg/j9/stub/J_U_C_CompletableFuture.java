package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class J_U_C_CompletableFuture {

    @Stub(opcVers = Opcodes.V9)
    public static <U> CompletableFuture<U> newIncompleteFuture(CompletableFuture<U> future) {
        return new CompletableFuture<>();
    }

    @Stub(opcVers = Opcodes.V9)
    public static Executor defaultExecutor(CompletableFuture<?> future) {
        return ForkJoinPool.commonPool();
    }

    @Stub(opcVers = Opcodes.V9)
    public static <T> CompletableFuture<T> copy(CompletableFuture<T> future) {
        return future.thenApply(Function.identity());
    }

    @Stub(opcVers = Opcodes.V9)
    public static <T> CompletionStage<T> minimalCompletionStage(CompletableFuture<T> future) {
        return future.thenApply(Function.identity());
    }

    @Stub(opcVers = Opcodes.V9, include = Completer.class)
    public static <T> CompletableFuture<T> completeAsync(CompletableFuture<T> future, Supplier<? extends T> supplier, Executor executor) {
        if (supplier == null || executor == null) {
            throw new NullPointerException();
        }
        new Completer<>(future, supplier).completeAsync(executor);
        return future;
    }

    @Stub(opcVers = Opcodes.V9, include = Completer.class)
    public static <T> CompletableFuture<T> completeAsync(CompletableFuture<T> future, Supplier<? extends T> supplier) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        new Completer<>(future, supplier).completeAsync(ForkJoinPool.commonPool());
        return future;
    }

    @Stub(opcVers = Opcodes.V9, include = Completer.class)
    public static <T> CompletableFuture<T> orTimeout(CompletableFuture<T> future, long timeout, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }
        if (future.isDone()) {
            return future;
        }
        new Completer<>(future, null).orTimeout(timeout, unit);
        return future;
    }

    @Stub(opcVers = Opcodes.V9, include = Completer.class)
    public static <T> CompletableFuture<T> completeOnTimeout(CompletableFuture<T> future, T value, long timeout, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }
        if (future.isDone()) {
            return future;
        }
        new Completer<>(future, null).completeOnTimeout(value, timeout, unit);
        return future;
    }

    @Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/util/concurrent/CompletableFuture;"), include = DelayedExecutor.class)
    public static Executor delayedExecutor(long delay, TimeUnit unit, Executor executor) {
        return new DelayedExecutor(executor, delay, unit);
    }

    @Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/util/concurrent/CompletableFuture;"), include = DelayedExecutor.class)
    public static Executor delayedExecutor(long delay, TimeUnit unit) {
        return new DelayedExecutor(ForkJoinPool.commonPool(), delay, unit);
    }

    @Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/util/concurrent/CompletableFuture;"))
    public static <U> CompletionStage<U> completedStage(U value) {
        return CompletableFuture.completedFuture(value);
    }

    @Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/util/concurrent/CompletableFuture;"))
    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }
        CompletableFuture<U> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

    @Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/util/concurrent/CompletableFuture;"))
    public static <U> CompletionStage<U> failedStage(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }
        CompletableFuture<U> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

    public static class Completer<T> {

        private final CompletableFuture<T> future;
        private final Supplier<? extends T> supplier;

        public Completer(CompletableFuture<T> future, Supplier<? extends T> supplier) {
            this.future = future;
            this.supplier = supplier;
        }

        public void completeAsync(Executor executor) {
            if (future.isDone()) {
                return;
            }
            executor.execute(this::complete);
        }

        public void complete() {
            if (future.isDone()) {
                return;
            }
            future.complete(supplier.get());
        }

        public void completeOnTimeout(T value, long timeout, TimeUnit unit) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(unit.toMillis(timeout));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (future.isDone()) {
                    return;
                }
                future.complete(value);
            });
        }

        public void orTimeout(long timeout, TimeUnit unit) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(unit.toMillis(timeout));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (future.isDone()) {
                    return;
                }
                future.completeExceptionally(new TimeoutException());
            });
        }

    }

    public static class DelayedExecutor implements Executor {
        private final Executor executor;
        private final long delay;
        private final TimeUnit unit;

        public DelayedExecutor(Executor executor, long delay, TimeUnit unit) {
            this.executor = executor;
            this.delay = delay;
            this.unit = unit;
        }

        @Override
        public void execute(Runnable command) {
            executor.execute(() -> {
                try {
                    Thread.sleep(unit.toMillis(delay));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                command.run();
            });
        }

    }

}
