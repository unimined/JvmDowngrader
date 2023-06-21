package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BiFunction;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Function;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.Executor;

@Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/concurrent/CompletionStage"))
public interface J_U_C_CompletionStage<T> {

    <U> J_U_C_CompletionStage<U> thenApply(J_U_F_Function<? super T, ? extends U> fn);

    <U> J_U_C_CompletionStage<U> thenApplyAsync(J_U_F_Function<? super T, ? extends U> fn);

    <U> J_U_C_CompletionStage<U> thenApplyAsync(J_U_F_Function<? super T, ? extends U> fn, Executor executor);

    J_U_C_CompletionStage<Void> thenAccept(J_U_F_Function<? super T, Void> action);

    J_U_C_CompletionStage<Void> thenAcceptAsync(J_U_F_Function<? super T, Void> action);

    J_U_C_CompletionStage<Void> thenAcceptAsync(J_U_F_Function<? super T, Void> action, Executor executor);

    J_U_C_CompletionStage<Void> thenRun(Runnable action);

    J_U_C_CompletionStage<Void> thenRunAsync(Runnable action);

    J_U_C_CompletionStage<Void> thenRunAsync(Runnable action, Executor executor);

    <U, V> J_U_C_CompletionStage<V> thenCombine(J_U_C_CompletionStage<? extends U> other, J_U_F_BiFunction<? super T, ? super U, ? extends V> fn);

    <U, V> J_U_C_CompletionStage<V> thenCombineAsync(J_U_C_CompletionStage<? extends U> other, J_U_F_BiFunction<? super T, ? super U, ? extends V> fn);

    <U, V> J_U_C_CompletionStage<V> thenCombineAsync(J_U_C_CompletionStage<? extends U> other, J_U_F_BiFunction<? super T, ? super U, ? extends V> fn, Executor executor);

    <U> J_U_C_CompletionStage<Void> thenAcceptBoth(J_U_C_CompletionStage<? extends U> other, J_U_F_BiFunction<? super T, ? super U, Void> action);

    <U> J_U_C_CompletionStage<Void> thenAcceptBothAsync(J_U_C_CompletionStage<? extends U> other, J_U_F_BiFunction<? super T, ? super U, Void> action);

    <U> J_U_C_CompletionStage<Void> thenAcceptBothAsync(J_U_C_CompletionStage<? extends U> other, J_U_F_BiFunction<? super T, ? super U, Void> action, Executor executor);

    J_U_C_CompletionStage<Void> runAfterBoth(J_U_C_CompletionStage<?> other, Runnable action);

    J_U_C_CompletionStage<Void> runAfterBothAsync(J_U_C_CompletionStage<?> other, Runnable action);

    J_U_C_CompletionStage<Void> runAfterBothAsync(J_U_C_CompletionStage<?> other, Runnable action, Executor executor);

    <U> J_U_C_CompletionStage<U> applyToEither(J_U_C_CompletionStage<? extends T> other, J_U_F_Function<? super T, U> fn);

    <U> J_U_C_CompletionStage<U> applyToEitherAsync(J_U_C_CompletionStage<? extends T> other, J_U_F_Function<? super T, U> fn);

    <U> J_U_C_CompletionStage<U> applyToEitherAsync(J_U_C_CompletionStage<? extends T> other, J_U_F_Function<? super T, U> fn, Executor executor);

    J_U_C_CompletionStage<Void> acceptEither(J_U_C_CompletionStage<? extends T> other, J_U_F_Function<? super T, Void> action);

    J_U_C_CompletionStage<Void> acceptEitherAsync(J_U_C_CompletionStage<? extends T> other, J_U_F_Function<? super T, Void> action);

    J_U_C_CompletionStage<Void> acceptEitherAsync(J_U_C_CompletionStage<? extends T> other, J_U_F_Function<? super T, Void> action, Executor executor);

    J_U_C_CompletionStage<Void> runAfterEither(J_U_C_CompletionStage<?> other, Runnable action);

    J_U_C_CompletionStage<Void> runAfterEitherAsync(J_U_C_CompletionStage<?> other, Runnable action);

    J_U_C_CompletionStage<Void> runAfterEitherAsync(J_U_C_CompletionStage<?> other, Runnable action, Executor executor);

    <U> J_U_C_CompletionStage<U> thenCompose(J_U_F_Function<? super T, ? extends J_U_C_CompletionStage<U>> fn);

    <U> J_U_C_CompletionStage<U> thenComposeAsync(J_U_F_Function<? super T, ? extends J_U_C_CompletionStage<U>> fn);

    <U> J_U_C_CompletionStage<U> thenComposeAsync(J_U_F_Function<? super T, ? extends J_U_C_CompletionStage<U>> fn, Executor executor);

    <U> J_U_C_CompletionStage<T> handle(J_U_F_BiFunction<? super T, Throwable, ? extends U> fn);

    <U> J_U_C_CompletionStage<U> handleAsync(J_U_F_BiFunction<? super T, Throwable, ? extends U> fn);

    <U> J_U_C_CompletionStage<U> handleAsync(J_U_F_BiFunction<? super T, Throwable, ? extends U> fn, Executor executor);

    J_U_C_CompletionStage<T> whenComplete(J_U_F_BiFunction<? super T, Throwable, ? extends Void> action);

    J_U_C_CompletionStage<T> whenCompleteAsync(J_U_F_BiFunction<? super T, Throwable, ? extends Void> action);

    J_U_C_CompletionStage<T> whenCompleteAsync(J_U_F_BiFunction<? super T, Throwable, ? extends Void> action, Executor executor);

    J_U_C_CompletableFuture<T> toCompletableFuture();
}
