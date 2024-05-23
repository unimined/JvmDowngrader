package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.j8.intl.collections.SynchronizedBackingSet;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BiConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BiFunction;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Function;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Supplier;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

@Adapter("java/util/concurrent/CompletableFuture")
public class J_U_C_CompletableFuture<T> implements Future<T>, J_U_C_CompletionStage<T> {
    private static final boolean USE_COMMON_POOL = J_U_C_ForkJoinPool.getCommonPoolParallelism() > 1;
    private static final Executor ASYNC_POOL = USE_COMMON_POOL ? J_U_C_ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();
    private final CompletableFutureTask<T> wrapped;
    private final Executor executor;

    J_U_C_CompletableFuture(CompletableFutureTask<T> wrapped, Executor executor) {
        this.wrapped = wrapped;
        this.executor = executor;
        // indicates synchronous completion
        if (executor == null) {
            wrapped.run();
        } else {
            executor.execute(wrapped);
        }
    }

    public static <U> J_U_C_CompletableFuture<U> supplyAsync(J_U_F_Supplier<U> supplier) {
        return supplyAsync(supplier, ASYNC_POOL);
    }

    public static <U> J_U_C_CompletableFuture<U> supplyAsync(final J_U_F_Supplier<U> supplier, Executor executor) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<>(new Callable<U>() {

            @Override
            public U call() throws Exception {
                return supplier.get();
            }

        }), executor);
    }

    public static J_U_C_CompletableFuture<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, ASYNC_POOL);
    }

    public static J_U_C_CompletableFuture<Void> runAsync(final Runnable runnable, Executor executor) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<Void>(runnable, null), executor);
    }

    public static <U> J_U_C_CompletableFuture<U> completedFuture(U value) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<>(new Runnable() {
            @Override
            public void run() {

            }
        }, value), null);
    }


    static <U> J_U_C_CompletableFuture<U> failedFuture(final Exception ex) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<>(new Callable<U>() {
            @Override
            public U call() throws Exception {
                throw ex;
            }
        }), null);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return wrapped.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return wrapped.isCancelled();
    }

    @Override
    public boolean isDone() {
        return wrapped.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return wrapped.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return wrapped.get(timeout, unit);
    }

    public T join() {
        try {
            return wrapped.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new J_U_C_CompletionException(e.getCause());
        }
    }

    public T getNow(T valueIfAbsent) {
        try {
            return wrapped.get(0, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new J_U_C_CompletionException(e.getCause());
        } catch (TimeoutException e) {
            return valueIfAbsent;
        }
    }


    public boolean complete(T value) {
        return wrapped.complete(value);
    }

    public boolean completeExceptionally(Throwable ex) {
        return wrapped.completeExceptionally(ex);
    }

    @Override
    public <U> J_U_C_CompletionStage<U> thenApply(final J_U_F_Function<? super T, ? extends U> fn) {
        final Semaphore semaphore = new Semaphore(0);
        wrapped.after(new Runnable() {
            @Override
            public void run() {
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
            return new J_U_C_CompletableFuture<>(new CompletableFutureTask<>(new Callable<U>() {
                @Override
                public U call() throws Exception {
                    return fn.apply(wrapped.get());
                }
            }), null);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <U> J_U_C_CompletionStage<U> thenApplyAsync(J_U_F_Function<? super T, ? extends U> fn) {
        return thenApplyAsync(fn, ASYNC_POOL);
    }

    @Override
    public <U> J_U_C_CompletionStage<U> thenApplyAsync(final J_U_F_Function<? super T, ? extends U> fn, final Executor executor) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<>(new Callable<U>() {
            @Override
            public U call() throws Exception {
                return fn.apply(wrapped.get());
            }
        }), new AfterCompleteDelegatingExecutor(executor));
    }

    @Override
    public J_U_C_CompletionStage<Void> thenAccept(final J_U_F_Function<? super T, Void> action) {
        final Semaphore semaphore = new Semaphore(0);
        wrapped.after(new Runnable() {
            @Override
            public void run() {
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
            return new J_U_C_CompletableFuture<>(new CompletableFutureTask<>((Callable<Void>) new Callable() {
                @Override
                public Object call() throws Exception {
                    action.apply(wrapped.get());
                    return null;
                }
            }), null);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J_U_C_CompletionStage<Void> thenAcceptAsync(J_U_F_Function<? super T, Void> action) {
        return thenAcceptAsync(action, ASYNC_POOL);
    }

    @Override
    public J_U_C_CompletionStage<Void> thenAcceptAsync(final J_U_F_Function<? super T, Void> action, Executor executor) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<>((Callable<Void>) new Callable() {
            @Override
            public Object call() throws Exception {
                action.apply(wrapped.get());
                return null;
            }
        }), new AfterCompleteDelegatingExecutor(executor));
    }

    @Override
    public J_U_C_CompletionStage<Void> thenRun(Runnable action) {
        final Semaphore semaphore = new Semaphore(0);
        wrapped.after(new Runnable() {
            @Override
            public void run() {
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
            return new J_U_C_CompletableFuture<>(new CompletableFutureTask<Void>(action, null), null);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J_U_C_CompletionStage<Void> thenRunAsync(Runnable action) {
        return thenRunAsync(action, ASYNC_POOL);
    }

    @Override
    public J_U_C_CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<Void>(action, null), new AfterCompleteDelegatingExecutor(executor));
    }

    @Override
    public <U, V> J_U_C_CompletionStage<V> thenCombine(final J_U_C_CompletionStage<? extends U> other, final J_U_F_BiFunction<? super T, ? super U, ? extends V> fn) {
        final Semaphore semaphore = new Semaphore(0);
        wrapped.after(new Runnable() {
            @Override
            public void run() {
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
            return other.thenApply(new J_U_F_Function.FunctionAdapter<U, V>() {
                @Override
                public V apply(U u) {
                    try {
                        return fn.apply(wrapped.get(), u);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        throw new J_U_C_CompletionException(e.getCause());
                    }
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <U, V> J_U_C_CompletionStage<V> thenCombineAsync(J_U_C_CompletionStage<? extends U> other, J_U_F_BiFunction<? super T, ? super U, ? extends V> fn) {
        return thenCombineAsync(other, fn, ASYNC_POOL);
    }

    @Override
    public <U, V> J_U_C_CompletionStage<V> thenCombineAsync(final J_U_C_CompletionStage<? extends U> other, final J_U_F_BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<>(new Callable<V>() {
            @Override
            public V call() throws Exception {
                return fn.apply(wrapped.get(), other.toCompletableFuture().get());
            }
        }), new AfterAllDelegatingExecutor(new HashSet<>(Arrays.asList(other.toCompletableFuture().wrapped, wrapped)), executor));
    }

    @Override
    public <U> J_U_C_CompletionStage<Void> thenAcceptBoth(final J_U_C_CompletionStage<? extends U> other, final J_U_F_BiFunction<? super T, ? super U, Void> action) {
        final Semaphore semaphore = new Semaphore(0);
        wrapped.after(new Runnable() {
            @Override
            public void run() {
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
            return other.thenAccept((J_U_F_Function) new J_U_F_Function.FunctionAdapter<U, Object>() {
                @Override
                public Object apply(U u) {
                    try {
                        action.apply(wrapped.get(), u);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <U> J_U_C_CompletionStage<Void> thenAcceptBothAsync(J_U_C_CompletionStage<? extends U> other, J_U_F_BiFunction<? super T, ? super U, Void> action) {
        return thenAcceptBothAsync(other, action, ASYNC_POOL);
    }

    @Override
    public <U> J_U_C_CompletionStage<Void> thenAcceptBothAsync(final J_U_C_CompletionStage<? extends U> other, final J_U_F_BiFunction<? super T, ? super U, Void> action, Executor executor) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                action.apply(wrapped.get(), other.toCompletableFuture().get());
                return null;
            }
        }), new AfterAllDelegatingExecutor(new HashSet<>(Arrays.asList(other.toCompletableFuture().wrapped, wrapped)), executor));
    }

    @Override
    public J_U_C_CompletionStage<Void> runAfterBoth(J_U_C_CompletionStage<?> other, Runnable action) {
        final Semaphore semaphore = new Semaphore(0);
        wrapped.after(new Runnable() {
            @Override
            public void run() {
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
            return other.thenRun(action);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J_U_C_CompletionStage<Void> runAfterBothAsync(J_U_C_CompletionStage<?> other, Runnable action) {
        return runAfterBothAsync(other, action, ASYNC_POOL);
    }

    @Override
    public J_U_C_CompletionStage<Void> runAfterBothAsync(J_U_C_CompletionStage<?> other, final Runnable action, Executor executor) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<Void>(action, null), new AfterAllDelegatingExecutor(new HashSet<>(Arrays.asList(other.toCompletableFuture().wrapped, wrapped)), executor));
    }

    @Override
    public <U> J_U_C_CompletionStage<U> applyToEither(final J_U_C_CompletionStage<? extends T> other, final J_U_F_Function<? super T, U> fn) {
        final Semaphore semaphore = new Semaphore(0);
        final boolean[] exceptionally = new boolean[1];
        wrapped.after(new Runnable() {
            @Override
            public void run() {
                try {
                    wrapped.get();
                } catch (InterruptedException | ExecutionException e) {
                    exceptionally[0] = true;
                }
                semaphore.release();
            }
        });
        other.toCompletableFuture().wrapped.after(new Runnable() {
            @Override
            public void run() {
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
            if (exceptionally[0] || exceptionally[1]) {
                semaphore.acquire();
            }
            if (exceptionally[0] && exceptionally[1]) {
                return failedFuture(new ExecutionException("Both futures completed exceptionally", null));
            }
            // get the done one
            if (exceptionally[0]) {
                return other.thenApply(fn);
            } else if (exceptionally[1]) {
                return thenApply(fn);
            } else {
                // neither failed, so we can't know which one is done
                T result = null;
                try {
                    // get first
                    result = wrapped.get(0, TimeUnit.NANOSECONDS);
                } catch (J_U_C_CompletionException | ExecutionException | TimeoutException e) {
                    try {
                        result = other.toCompletableFuture().get(0, TimeUnit.NANOSECONDS);
                    } catch (ExecutionException e1) {
                        return failedFuture((Exception) e1.getCause());
                    } catch (TimeoutException e2) {
                        throw new RuntimeException(e2);
                    }
                }
                return completedFuture(fn.apply(result));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <U> J_U_C_CompletionStage<U> applyToEitherAsync(J_U_C_CompletionStage<? extends T> other, J_U_F_Function<? super T, U> fn) {
        return applyToEitherAsync(other, fn, ASYNC_POOL);
    }

    @Override
    public <U> J_U_C_CompletionStage<U> applyToEitherAsync(final J_U_C_CompletionStage<? extends T> other, final J_U_F_Function<? super T, U> fn, Executor executor) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<>(new Callable<U>() {
            @Override
            public U call() throws Exception {
                try {
                    return fn.apply(wrapped.get(0, TimeUnit.NANOSECONDS));
                } catch (InterruptedException | ExecutionException e) {
                    return fn.apply(other.toCompletableFuture().get(0, TimeUnit.NANOSECONDS));
                }
            }
        }), new AfterFirstExecutor((Set) new HashSet<>(Arrays.asList(other.toCompletableFuture().wrapped, wrapped)), executor));
    }

    @Override
    public J_U_C_CompletionStage<Void> acceptEither(final J_U_C_CompletionStage<? extends T> other, final J_U_F_Function<? super T, Void> action) {
        return applyToEither(other, (J_U_F_Function) new J_U_F_Function.FunctionAdapter<T, Object>() {

            @Override
            public Object apply(T t) {
                action.apply(t);
                return null;
            }
        });
    }

    @Override
    public J_U_C_CompletionStage<Void> acceptEitherAsync(J_U_C_CompletionStage<? extends T> other, J_U_F_Function<? super T, Void> action) {
        return acceptEitherAsync(other, action, ASYNC_POOL);
    }

    @Override
    public J_U_C_CompletionStage<Void> acceptEitherAsync(J_U_C_CompletionStage<? extends T> other, final J_U_F_Function<? super T, Void> action, Executor executor) {
        return applyToEitherAsync(other, new J_U_F_Function.FunctionAdapter<T, Void>() {
            @Override
            public Void apply(T t) {
                action.apply(t);
                return null;
            }
        }, executor);
    }

    @Override
    public J_U_C_CompletionStage<Void> runAfterEither(J_U_C_CompletionStage<?> other, Runnable action) {
        final Semaphore semaphore = new Semaphore(0);
        final boolean[] exceptionally = new boolean[1];
        wrapped.after(new Runnable() {
            @Override
            public void run() {
                try {
                    wrapped.get();
                } catch (InterruptedException | ExecutionException e) {
                    exceptionally[0] = true;
                }
                semaphore.release();
            }
        });
        other.toCompletableFuture().wrapped.after(new Runnable() {
            @Override
            public void run() {
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
            if (exceptionally[0] || exceptionally[1]) {
                semaphore.acquire();
            }
            if (exceptionally[0] && exceptionally[1]) {
                return failedFuture(new ExecutionException("Both futures completed exceptionally", null));
            }
            // get the done one
            if (exceptionally[0]) {
                return other.thenRun(action);
            } else if (exceptionally[1]) {
                return thenRun(action);
            } else {
                return new J_U_C_CompletableFuture<>(new CompletableFutureTask<Void>(action, null), new AfterFirstExecutor((Set) new HashSet<>(Arrays.asList(other.toCompletableFuture().wrapped, wrapped)), ASYNC_POOL));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public J_U_C_CompletionStage<Void> runAfterEitherAsync(J_U_C_CompletionStage<?> other, Runnable action) {
        return runAfterEitherAsync(other, action, ASYNC_POOL);
    }

    @Override
    public J_U_C_CompletionStage<Void> runAfterEitherAsync(J_U_C_CompletionStage<?> other, Runnable action, Executor executor) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<Void>(action, null), new AfterFirstExecutor((Set) new HashSet<>(Arrays.asList(other.toCompletableFuture().wrapped, wrapped)), executor));
    }

    @Override
    public <U> J_U_C_CompletionStage<U> thenCompose(J_U_F_Function<? super T, ? extends J_U_C_CompletionStage<U>> fn) {
        final Semaphore semaphore = new Semaphore(0);
        final boolean[] exceptionally = new boolean[1];
        wrapped.after(new Runnable() {
            @Override
            public void run() {
                try {
                    wrapped.get();
                } catch (InterruptedException | ExecutionException e) {
                    exceptionally[0] = true;
                }
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
            if (exceptionally[0]) {
                return failedFuture(new ExecutionException("Future completed exceptionally", null));
            }
            return fn.apply(wrapped.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <U> J_U_C_CompletionStage<U> thenComposeAsync(J_U_F_Function<? super T, ? extends J_U_C_CompletionStage<U>> fn) {
        return thenComposeAsync(fn, ASYNC_POOL);
    }

    @Override
    public <U> J_U_C_CompletionStage<U> thenComposeAsync(J_U_F_Function<? super T, ? extends J_U_C_CompletionStage<U>> fn, Executor executor) {
        return thenApplyAsync(fn, executor).thenApplyAsync(new J_U_F_Function.FunctionAdapter<J_U_C_CompletionStage<U>, U>() {
            @Override
            public U apply(J_U_C_CompletionStage<U> ujUCCompletionStage) {
                return ujUCCompletionStage.toCompletableFuture().join();
            }
        }, executor);
    }

    @Override
    public <U> J_U_C_CompletionStage<U> handle(final J_U_F_BiFunction<? super T, Throwable, ? extends U> fn) {
        final Semaphore semaphore = new Semaphore(0);
        wrapped.after(new Runnable() {
            @Override
            public void run() {
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
            return new J_U_C_CompletableFuture<>(new CompletableFutureTask<U>(new Callable<U>() {
                @Override
                public U call() throws Exception {
                    try {
                        return fn.apply(get(0, TimeUnit.NANOSECONDS), null);
                    } catch (J_U_C_CompletionException e) {
                        return fn.apply(null, e.getCause());
                    } catch (TimeoutException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }), executor);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <U> J_U_C_CompletionStage<U> handleAsync(J_U_F_BiFunction<? super T, Throwable, ? extends U> fn) {
        return handleAsync(fn, ASYNC_POOL);
    }

    @Override
    public <U> J_U_C_CompletionStage<U> handleAsync(final J_U_F_BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return new J_U_C_CompletableFuture<>(new CompletableFutureTask<U>(new Callable<U>() {
            @Override
            public U call() throws Exception {
                try {
                    return fn.apply(get(0, TimeUnit.NANOSECONDS), null);
                } catch (J_U_C_CompletionException e) {
                    return fn.apply(null, e.getCause());
                } catch (TimeoutException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }), executor);
    }

    @Override
    public J_U_C_CompletionStage<T> whenComplete(final J_U_F_BiConsumer<? super T, ? super Throwable> action) {
        return handle(new J_U_F_BiFunction.BiFunctionAdapter<T, Throwable, T>() {
            @Override
            public T apply(T t, Throwable throwable) {
                action.accept(t, throwable);
                return t;
            }
        });
    }

    @Override
    public J_U_C_CompletionStage<T> whenCompleteAsync(J_U_F_BiConsumer<? super T, ? super Throwable> action) {
        return whenCompleteAsync(action, ASYNC_POOL);
    }

    @Override
    public J_U_C_CompletionStage<T> whenCompleteAsync(final J_U_F_BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return handleAsync(new J_U_F_BiFunction.BiFunctionAdapter<T, Throwable, T>() {
            @Override
            public T apply(T t, Throwable throwable) {
                action.accept(t, throwable);
                return t;
            }
        }, executor);
    }

    @Override
    public J_U_C_CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    @Adapter("java/util/concurrent/CompletableFuture$AsynchronousCompletionTask")
    public interface AsynchronousCompletionTask {
    }

    //TODO: switch to ForkJoinTask
    private static class CompletableFutureTask<T> extends FutureTask<T> {
        Runnable after;

        CompletableFutureTask(Callable<T> runnable) {
            super(runnable);
        }

        CompletableFutureTask(Runnable runnable, T result) {
            super(runnable, result);
        }

        @Override
        public void run() {
            super.run();
            synchronized (this) {
                if (after != null) {
                    after.run();
                    after = null;
                }
            }
        }

        public void after(final Runnable runnable) {
            if (isDone()) {
                runnable.run();
            } else {
                synchronized (this) {
                    if (isDone()) {
                        runnable.run();
                    } else {
                        final Runnable prev = after;
                        after = new Runnable() {
                            @Override
                            public void run() {
                                if (prev != null) {
                                    prev.run();
                                }
                                runnable.run();
                            }
                        };
                    }
                }
            }
        }

        public boolean complete(T value) {
            set(value);
            synchronized (this) {
                if (after != null) {
                    after.run();
                    after = null;
                }
            }
            try {
                return get() == value;
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        }

        public boolean completeExceptionally(Throwable ex) {
            setException(ex);
            synchronized (this) {
                if (after != null) {
                    after.run();
                    after = null;
                }
            }
            try {
                get();
                return false;
            } catch (ExecutionException e) {
                return e.getCause() == ex;
            } catch (InterruptedException e) {
                return false;
            }
        }
    }

    private static class AsyncRunnableFuture<T> extends CompletableFutureTask<T> implements AsynchronousCompletionTask {
        AsyncRunnableFuture(Callable<T> runnable) {
            super(runnable);
        }

        AsyncRunnableFuture(Runnable runnable, T result) {
            super(runnable, result);
        }

        @Override
        public void run() {
            super.run();
        }
    }

    private static class ThreadPerTaskExecutor implements Executor {

        @Override
        public void execute(Runnable command) {
            new Thread(command).start();
        }
    }

    private static class RunOnCurrentThreadExecutor implements Executor {

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private static class AfterAllDelegatingExecutor implements Executor {
        private final Set<CompletableFutureTask<?>> tasks;
        private final Deque<Runnable> runnables = new ConcurrentLinkedDeque<>();
        private final Executor executor;

        public AfterAllDelegatingExecutor(final Set<CompletableFutureTask<?>> tasks, final Executor executor) {
            this.tasks = new SynchronizedBackingSet<>(tasks);
            this.executor = executor;
            for (final CompletableFutureTask<?> task : tasks) {
                task.after(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (tasks) {
                            tasks.remove(task);
                        }
                        if (tasks.isEmpty()) {
                            Runnable runnable;
                            while ((runnable = runnables.poll()) != null) {
                                executor.execute(runnable);
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void execute(final Runnable command) {
            if (tasks.isEmpty()) {
                executor.execute(command);
            } else {
                synchronized (tasks) {
                    if (!tasks.isEmpty()) {
                        runnables.add(command);
                    }
                }
                executor.execute(command);
            }
        }
    }

    private static class AfterFirstExecutor implements Executor {
        private final Set<CompletableFutureTask<?>> tasks;
        private final Deque<Runnable> runnables = new ConcurrentLinkedDeque<>();
        private final Executor executor;
        private boolean done;

        public AfterFirstExecutor(final Set<CompletableFutureTask<?>> tasks, final Executor executor) {
            this.tasks = new SynchronizedBackingSet<>(tasks);
            this.executor = executor;
            for (final CompletableFutureTask<?> task : tasks) {
                task.after(new Runnable() {
                    @Override
                    public void run() {
                        boolean wasDone;
                        synchronized (tasks) {
                            tasks.remove(task);
                            wasDone = done;
                            done = true;
                        }
                        if (wasDone == false) {
                            Runnable runnable;
                            while ((runnable = runnables.poll()) != null) {
                                executor.execute(runnable);
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void execute(final Runnable command) {
            if (done) {
                executor.execute(command);
            } else {
                synchronized (tasks) {
                    if (!done) {
                        runnables.add(command);
                    }
                }
                executor.execute(command);
            }
        }
    }

    private class AfterCompleteDelegatingExecutor implements Executor {
        private final Executor executor;

        public AfterCompleteDelegatingExecutor(Executor executor) {
            this.executor = executor;
        }

        @Override
        public void execute(final Runnable command) {
            wrapped.after(new Runnable() {
                @Override
                public void run() {
                    executor.execute(command);
                }
            });
        }
    }
}
