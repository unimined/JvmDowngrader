package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/BiConsumer"))
public interface J_U_F_BiFunction<T, U, R> {

    R apply(T t, U u);

    default <V> J_U_F_BiFunction<T, U, V> andThen(J_U_F_Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, U u) -> after.apply(apply(t, u));
    }

}
