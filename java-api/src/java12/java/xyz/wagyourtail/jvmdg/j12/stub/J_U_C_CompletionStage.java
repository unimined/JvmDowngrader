package xyz.wagyourtail.jvmdg.j12.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class J_U_C_CompletionStage {

    @Stub(opcVers = Opcodes.V12)
    public static <T> CompletionStage<T> exceptionallyAsync(CompletionStage<T> stage, Function<Throwable, ? extends T> fn) {
        return stage.handle((r, ex) -> (ex == null) ? stage : stage.<T>handleAsync((r1, ex1) -> fn.apply(ex1))).thenCompose(Function.identity());
    }

    @Stub(opcVers = Opcodes.V12)
    public static <T> CompletionStage<T> exceptionallyAsync(CompletionStage<T> stage, Function<Throwable, ? extends T> fn, Executor executor) {
        return stage.handle((r, ex) -> (ex == null) ? stage : stage.<T>handleAsync((r1, ex1) -> fn.apply(ex1), executor)).thenCompose(Function.identity());
    }

    @Stub(opcVers = Opcodes.V12)
    public static <T> CompletionStage<T> exceptionallyCompose(CompletionStage<T> stage, Function<Throwable, ? extends CompletionStage<T>> fn) {
        return stage.handle((r, ex) -> (ex == null) ? stage : fn.apply(ex)).thenCompose(Function.identity());
    }

    @Stub(opcVers = Opcodes.V12)
    public static <T> CompletionStage<T> exceptionallyComposeAsync(CompletionStage<T> stage, Function<Throwable, ? extends CompletionStage<T>> fn) {
        return stage.handle((r, ex) -> (ex == null) ? stage : stage.handleAsync((r1, ex1) -> fn.apply(ex1)).thenCompose(Function.identity())).thenCompose(Function.identity());
    }

    @Stub(opcVers = Opcodes.V12)
    public static <T> CompletionStage<T> exceptionallyComposeAsync(CompletionStage<T> stage, Function<Throwable, ? extends CompletionStage<T>> fn, Executor executor) {
        return stage.handle((r, ex) -> (ex == null) ? stage : stage.handleAsync((r1, ex1) -> fn.apply(ex1), executor).thenCompose(Function.identity())).thenCompose(Function.identity());
    }
}
