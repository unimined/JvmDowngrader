package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/Function"))
public interface J_U_F_Function<T, R> {

    R apply(T t);

    default <V> J_U_F_Function<V, R> compose(J_U_F_Function<? super V, ? extends T> before) {
        return (V v) -> apply(before.apply(v));
    }

    default <V> J_U_F_Function<T, V> andThen(J_U_F_Function<? super R, ? extends V> after) {
        return (T t) -> after.apply(apply(t));
    }

    static <T> J_U_F_Function<T, T> identity() {
        return t -> t;
    }

}
