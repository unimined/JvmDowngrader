package xyz.wagyourtail.jvmdg.j12.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class J_U_C_CompletionStage {

    @Stub(opcVers = Opcodes.V12, include = ExceptionallyAsync.class, subtypes = true, returnDecendant = true)
    public static <T> CompletionStage<T> exceptionallyAsync(CompletionStage<T> stage, Function<Throwable, ? extends T> fn) {
        return stage.handle(new ExceptionallyAsync<>(stage, fn)::handleAsync).thenCompose(Function.identity());
    }

    @Stub(opcVers = Opcodes.V12, include = ExceptionallyAsync.class, subtypes = true, returnDecendant = true)
    public static <T> CompletionStage<T> exceptionallyAsync(CompletionStage<T> stage, Function<Throwable, ? extends T> fn, Executor executor) {
        return stage.handle(new ExceptionallyAsync<>(stage, fn, executor)::handleAsync)
            .thenCompose(Function.identity());
    }

    @Stub(opcVers = Opcodes.V12, include = ExceptionallyComposeAsync.class, subtypes = true, returnDecendant = true)
    public static <T> CompletionStage<T> exceptionallyCompose(CompletionStage<T> stage, Function<Throwable, ? extends CompletionStage<T>> fn) {
        return stage.handle(new ExceptionallyComposeAsync<>(stage, fn)::handle).thenCompose(Function.identity());
    }

    @Stub(opcVers = Opcodes.V12, include = ExceptionallyComposeAsync.class, subtypes = true, returnDecendant = true)
    public static <T> CompletionStage<T> exceptionallyComposeAsync(CompletionStage<T> stage, Function<Throwable, ? extends CompletionStage<T>> fn) {
        return stage.handle(new ExceptionallyComposeAsync<>(stage, fn)::handleAsync).thenCompose(Function.identity());
    }

    @Stub(opcVers = Opcodes.V12, include = ExceptionallyComposeAsync.class, subtypes = true, returnDecendant = true)
    public static <T> CompletionStage<T> exceptionallyComposeAsync(CompletionStage<T> stage, Function<Throwable, ? extends CompletionStage<T>> fn, Executor executor) {
        return stage.handle(new ExceptionallyComposeAsync<>(stage, fn, executor)::handleAsync)
            .thenCompose(Function.identity());
    }


    public static class ExceptionallyAsync<T> {

        private final CompletionStage<T> stage;
        private final Function<Throwable, ? extends T> fn;

        private final Executor executor;

        public ExceptionallyAsync(CompletionStage<T> stage, Function<Throwable, ? extends T> fn) {
            this.stage = stage;
            this.fn = fn;
            this.executor = null;
        }

        public ExceptionallyAsync(CompletionStage<T> stage, Function<Throwable, ? extends T> fn, Executor executor) {
            this.stage = stage;
            this.fn = fn;
            this.executor = executor;
        }

        public CompletionStage<T> handleAsync(T t, Throwable e) {
            if (e == null) {
                return stage;
            } else {
                return executor == null ? stage.handleAsync(this::handle) : stage.handleAsync(this::handle, executor);
            }
        }

        public T handle(T t, Throwable e) {
            return fn.apply(e);
        }

    }

    public static class ExceptionallyComposeAsync<T> {
        private final CompletionStage<T> stage;
        private final Function<Throwable, ? extends CompletionStage<T>> fn;

        private final Executor executor;

        public ExceptionallyComposeAsync(CompletionStage<T> stage, Function<Throwable, ? extends CompletionStage<T>> fn) {
            this.stage = stage;
            this.fn = fn;
            this.executor = null;
        }

        public ExceptionallyComposeAsync(CompletionStage<T> stage, Function<Throwable, ? extends CompletionStage<T>> fn, Executor executor) {
            this.stage = stage;
            this.fn = fn;
            this.executor = executor;
        }

        public CompletionStage<T> handleAsync(T t, Throwable e) {
            if (e == null) {
                return stage;
            } else {
                return (
                    executor == null ? stage.handleAsync(this::handleAsync) : stage.handleAsync(
                        this::handleAsync,
                        executor
                    )
                ).thenCompose(Function.identity());
            }
        }

        public CompletionStage<T> handle(T t, Throwable e) {
            return fn.apply(e);
        }

    }

}
