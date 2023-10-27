package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Comparator;
import java.util.Objects;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/BinaryOperator"))
public interface J_U_F_BinaryOperator<T> extends J_U_F_BiFunction<T, T, T> {


    class BinaryOperatorStatics {

        @Stub(ref = @Ref("Ljava/util/function/BinaryOperator;"))
        public static <T> J_U_F_BinaryOperator<T> minBy(final Comparator<? super T> comparator) {
            Objects.requireNonNull(comparator);
            return new J_U_F_BinaryOperator<T>() {
                @Override
                public T apply(T t, T t2) {
                    return comparator.compare(t, t2) <= 0 ? t : t2;
                }

                @Override
                public <V> J_U_F_BiFunction<T, T, V> andThen(J_U_F_Function<? super T, ? extends V> after) {
                    return BiFunctionDefaults.andThen(this, after);
                }
            };
        }

        @Stub(ref = @Ref("Ljava/util/function/BinaryOperator;"))
        public static <T> J_U_F_BinaryOperator<T> maxBy(final Comparator<? super T> comparator) {
            Objects.requireNonNull(comparator);
            return new J_U_F_BinaryOperator<T>() {
                @Override
                public T apply(T t, T t2) {
                    return comparator.compare(t, t2) >= 0 ? t : t2;
                }

                @Override
                public <V> J_U_F_BiFunction<T, T, V> andThen(J_U_F_Function<? super T, ? extends V> after) {
                    return BiFunctionDefaults.andThen(this, after);
                }
            };
        }

    }

}
