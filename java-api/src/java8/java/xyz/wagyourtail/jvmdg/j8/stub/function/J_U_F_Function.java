package xyz.wagyourtail.jvmdg.j8.stub.function;

import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Adapter("Ljava/util/function/Function;")
public interface J_U_F_Function<T, R> {

    R apply(T t);

    <V> J_U_F_Function<V, R> compose(J_U_F_Function<? super V, ? extends T> before);

    <V> J_U_F_Function<T, V> andThen(J_U_F_Function<? super R, ? extends V> after);

    class FunctionDefaults {

        @Stub(abstractDefault = true)
        public static <T, R, V> J_U_F_Function<V, R> compose(final J_U_F_Function<T, R> f1, final J_U_F_Function<? super V, ? extends T> f2) {
            Objects.requireNonNull(f2);
            return new J_U_F_Function.FunctionAdapter<V, R>() {
                @Override
                public R apply(V v) {
                    return f1.apply(f2.apply(v));
                }
            };
        }

        @Stub(abstractDefault = true)
        public static <T, R, V> J_U_F_Function<T, V> andThen(final J_U_F_Function<T, R> f1, final J_U_F_Function<? super R, ? extends V> f2) {
            Objects.requireNonNull(f2);
            return (J_U_F_Function<T, V>) compose(f2, f1);
        }

    }

    class FunctionStatic {

        @Stub(ref = @Ref("xyz/wagyourtail/jvmdg/j8/stub/function/J_U_F_Function"))
        public static <T> J_U_F_Function<T, T> identity() {
            return new J_U_F_Function.FunctionAdapter<T, T>() {
                @Override
                public T apply(T t) {
                    return t;
                }
            };
        }

    }

    abstract class FunctionAdapter<T, R> implements J_U_F_Function<T, R> {

        @Override
        public <V> J_U_F_Function<V, R> compose(J_U_F_Function<? super V, ? extends T> before) {
            return FunctionDefaults.compose(this, before);
        }

        @Override
        public <V> J_U_F_Function<T, V> andThen(J_U_F_Function<? super R, ? extends V> after) {
            return FunctionDefaults.andThen(this, after);
        }

    }

}
