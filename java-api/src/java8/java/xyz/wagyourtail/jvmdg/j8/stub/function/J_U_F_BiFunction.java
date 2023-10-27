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

    <V> J_U_F_BiFunction<T, U, V> andThen(J_U_F_Function<? super R, ? extends V> after);

    class BiFunctionDefaults {

        @Stub(abstractDefault = true)
        public static <T, U, V, R> J_U_F_BiFunction<T, U, R> andThen(final J_U_F_BiFunction<T, U, ? extends V> f1, final J_U_F_Function<? super V, ? extends R> f2) {
            Objects.requireNonNull(f2);
            return new J_U_F_BiFunction<T, U, R>() {
                @Override
                public R apply(T t, U u) {
                    return f2.apply(f1.apply(t, u));
                }

                @Override
                public <V> J_U_F_BiFunction<T, U, V> andThen(J_U_F_Function<? super R, ? extends V> after) {
                    Objects.requireNonNull(after);
                    return BiFunctionDefaults.andThen(this, after);
                }
            };
        }
    }

}
