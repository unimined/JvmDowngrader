package xyz.wagyourtail.jvmdg.j8.stub.function;

import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

@J_L_FunctionalInterface
@Adapter("Ljava/util/function/UnaryOperator;")
public interface J_U_F_UnaryOperator<T> extends J_U_F_Function<T, T> {


    class UnaryOperatorStatic {

        @Stub(ref = @Ref("Ljava/util/function/UnaryOperator;"))
        public static <T> J_U_F_UnaryOperator<T> identity() {
            return new J_U_F_UnaryOperator.UnaryOperatorAdapter<T>() {
                @Override
                public T apply(T t) {
                    return t;
                }
            };
        }

    }

    abstract class UnaryOperatorAdapter<T> implements J_U_F_UnaryOperator<T> {
        @Override
        public <V> J_U_F_Function<V, T> compose(J_U_F_Function<? super V, ? extends T> before) {
            return FunctionDefaults.compose(this, before);
        }

        @Override
        public <V> J_U_F_Function<T, V> andThen(J_U_F_Function<? super T, ? extends V> after) {
            return FunctionDefaults.andThen(this, after);
        }

    }

}
