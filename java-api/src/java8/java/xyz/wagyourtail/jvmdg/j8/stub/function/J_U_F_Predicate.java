package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/Predicate"))
public interface J_U_F_Predicate<T> {

    boolean test(T t);

    J_U_F_Predicate<T> and(J_U_F_Predicate<? super T> other);

    J_U_F_Predicate<T> negate();

    J_U_F_Predicate<T> or(J_U_F_Predicate<? super T> other);

    class PredicateDefaults {

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static <T> J_U_F_Predicate<T> and(final J_U_F_Predicate<T> self, final J_U_F_Predicate<? super T> other) {
            return new J_U_F_Predicate<T>() {
                @Override
                public boolean test(T t) {
                    return self.test(t) && other.test(t);
                }

                @Override
                public J_U_F_Predicate<T> and(J_U_F_Predicate<? super T> other) {
                    return PredicateDefaults.and(this, other);
                }

                @Override
                public J_U_F_Predicate<T> negate() {
                    return PredicateDefaults.negate(this);
                }

                @Override
                public J_U_F_Predicate<T> or(J_U_F_Predicate<? super T> other) {
                    return PredicateDefaults.or(this, other);
                }
            };
        }

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static <T> J_U_F_Predicate<T> negate(final J_U_F_Predicate<T> self) {
            return new J_U_F_Predicate<T>() {
                @Override
                public boolean test(T t) {
                    return !self.test(t);
                }

                @Override
                public J_U_F_Predicate<T> and(J_U_F_Predicate<? super T> other) {
                    return PredicateDefaults.and(this, other);
                }

                @Override
                public J_U_F_Predicate<T> negate() {
                    return PredicateDefaults.negate(this);
                }

                @Override
                public J_U_F_Predicate<T> or(J_U_F_Predicate<? super T> other) {
                    return PredicateDefaults.or(this, other);
                }
            };
        }

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static <T> J_U_F_Predicate<T> or(final J_U_F_Predicate<T> self, final J_U_F_Predicate<? super T> other) {
            return new J_U_F_Predicate<T>() {
                @Override
                public boolean test(T t) {
                    return self.test(t) || other.test(t);
                }

                @Override
                public J_U_F_Predicate<T> and(J_U_F_Predicate<? super T> other) {
                    return PredicateDefaults.and(this, other);
                }

                @Override
                public J_U_F_Predicate<T> negate() {
                    return PredicateDefaults.negate(this);
                }

                @Override
                public J_U_F_Predicate<T> or(J_U_F_Predicate<? super T> other) {
                    return PredicateDefaults.or(this, other);
                }
            };
        }

    }

    class PredicateStatic {

        @Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/function/Predicate;"))
        public static <T> J_U_F_Predicate<T> isEqual(final Object targetRef) {
            return new J_U_F_Predicate<T>() {
                @Override
                public boolean test(T t) {
                    if (null == targetRef) {
                        return t == null;
                    } else {
                        return targetRef.equals(t);
                    }
                }

                @Override
                public J_U_F_Predicate<T> and(J_U_F_Predicate<? super T> other) {
                    return PredicateDefaults.and(this, other);
                }

                @Override
                public J_U_F_Predicate<T> negate() {
                    return PredicateDefaults.negate(this);
                }

                @Override
                public J_U_F_Predicate<T> or(J_U_F_Predicate<? super T> other) {
                    return PredicateDefaults.or(this, other);
                }
            };
        }

        @Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/function/Predicate;"))
        public static <T> J_U_F_Predicate<T> not(J_U_F_Predicate<? super T> target) {
            return (J_U_F_Predicate<T>) PredicateDefaults.negate(target);
        }

    }
}
