package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/Function"))
public interface J_U_F_Function<T, R> {

    R apply(T t);

    <V> J_U_F_Function<V, R> compose(J_U_F_Function<? super V, ? extends T> before);

    <V> J_U_F_Function<T, V> andThen(J_U_F_Function<? super R, ? extends V> after);

    class FunctionDefaults {

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static <T, R, V> J_U_F_Function<V, R> compose(final J_U_F_Function<T, R> f1, final J_U_F_Function<? super V, ? extends T> f2) {
            Objects.requireNonNull(f2);
            return new J_U_F_Function<V, R>() {
                @Override
                public R apply(V v) {
                    return f1.apply(f2.apply(v));
                }

                @Override
                public <V1> J_U_F_Function<V1, R> compose(J_U_F_Function<? super V1, ? extends V> before) {
                    return FunctionDefaults.compose(this, before);
                }

                @Override
                public <V1> J_U_F_Function<V, V1> andThen(J_U_F_Function<? super R, ? extends V1> after) {
                    return FunctionDefaults.andThen(this, after);
                }
            };
        }

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static <T, R, V> J_U_F_Function<T, V> andThen(final J_U_F_Function<T, R> f1, final J_U_F_Function<? super R, ? extends V> f2) {
            Objects.requireNonNull(f2);
            return (J_U_F_Function<T, V>) compose(f2, f1);
        }

    }

    class FunctionStatic {

        @Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/function/Function;"))
        public static <T> J_U_F_Function<T, T> identity() {
            return new J_U_F_Function<T, T>() {
                @Override
                public T apply(T t) {
                    return t;
                }

                @Override
                public <V> J_U_F_Function<V, T> compose(J_U_F_Function<? super V, ? extends T> before) {
                    return FunctionDefaults.compose(this, before);
                }

                @Override
                public <V> J_U_F_Function<T, V> andThen(J_U_F_Function<? super T, ? extends V> after) {
                    return FunctionDefaults.andThen(this, after);
                }
            };
        }
    }

}
