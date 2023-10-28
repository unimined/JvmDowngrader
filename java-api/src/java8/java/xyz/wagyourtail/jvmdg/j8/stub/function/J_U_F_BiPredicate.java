package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/BiPredicate"))
public interface J_U_F_BiPredicate<T, U> {

    boolean test(T t, U u);

    J_U_F_BiPredicate<T, U> and(J_U_F_BiPredicate<? super T, ? super U> other);

    J_U_F_BiPredicate<T, U> negate();

    J_U_F_BiPredicate<T, U> or(J_U_F_BiPredicate<? super T, ? super U> other);

    class BiPredicateDefaults {

        @Stub(abstractDefault = true)
        public static <T, U> J_U_F_BiPredicate<T, U> and(final J_U_F_BiPredicate<T, U> p1, final J_U_F_BiPredicate<? super T, ? super U> p2) {
            Objects.requireNonNull(p2);
            return new J_U_F_BiPredicate.BiPredicateAdapter<T, U>() {
                @Override
                public boolean test(T t, U u) {
                    return p1.test(t, u) && p2.test(t, u);
                }
            };
        }

        @Stub(abstractDefault = true)
        public static <T, U> J_U_F_BiPredicate<T, U> negate(final J_U_F_BiPredicate<T, U> p) {
            return new J_U_F_BiPredicate.BiPredicateAdapter<T, U>() {
                @Override
                public boolean test(T t, U u) {
                    return !p.test(t, u);
                }
            };
        }

        @Stub(abstractDefault = true)
        public static <T, U> J_U_F_BiPredicate<T, U> or(final J_U_F_BiPredicate<T, U> p1, final J_U_F_BiPredicate<? super T, ? super U> p2) {
            Objects.requireNonNull(p2);
            return new J_U_F_BiPredicate.BiPredicateAdapter<T, U>() {
                @Override
                public boolean test(T t, U u) {
                    return p1.test(t, u) || p2.test(t, u);
                }
            };
        }
    }

    abstract class BiPredicateAdapter<T, U> implements J_U_F_BiPredicate<T, U> {

        @Override
        public J_U_F_BiPredicate<T, U> and(J_U_F_BiPredicate<? super T, ? super U> other) {
            return BiPredicateDefaults.and(this, other);
        }

        @Override
        public J_U_F_BiPredicate<T, U> negate() {
            return BiPredicateDefaults.negate(this);
        }

        @Override
        public J_U_F_BiPredicate<T, U> or(J_U_F_BiPredicate<? super T, ? super U> other) {
            return BiPredicateDefaults.or(this, other);
        }

    }

}
