package xyz.wagyourtail.jvmdg.j8.stub.function;

import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Stub;

@J_L_FunctionalInterface
@Adapter("Ljava/util/function/LongPredicate;")
public interface J_U_F_LongPredicate {

    boolean test(long value);

    J_U_F_LongPredicate and(J_U_F_LongPredicate other);

    J_U_F_LongPredicate negate();

    J_U_F_LongPredicate or(J_U_F_LongPredicate other);

    class LongPredicateDefaults {

        @Stub(abstractDefault = true)
        public static J_U_F_LongPredicate and(final J_U_F_LongPredicate self, final J_U_F_LongPredicate other) {
            return new J_U_F_LongPredicate.LongPredicateAdapter() {
                @Override
                public boolean test(long value) {
                    return self.test(value) && other.test(value);
                }
            };
        }

        @Stub(abstractDefault = true)
        public static J_U_F_LongPredicate negate(final J_U_F_LongPredicate self) {
            return new J_U_F_LongPredicate.LongPredicateAdapter() {
                @Override
                public boolean test(long value) {
                    return !self.test(value);
                }
            };
        }

        @Stub(abstractDefault = true)
        public static J_U_F_LongPredicate or(final J_U_F_LongPredicate self, final J_U_F_LongPredicate other) {
            return new J_U_F_LongPredicate.LongPredicateAdapter() {
                @Override
                public boolean test(long value) {
                    return self.test(value) || other.test(value);
                }
            };
        }

    }

    abstract class LongPredicateAdapter implements J_U_F_LongPredicate {

        @Override
        public J_U_F_LongPredicate and(J_U_F_LongPredicate other) {
            return LongPredicateDefaults.and(this, other);
        }

        @Override
        public J_U_F_LongPredicate negate() {
            return LongPredicateDefaults.negate(this);
        }

        @Override
        public J_U_F_LongPredicate or(J_U_F_LongPredicate other) {
            return LongPredicateDefaults.or(this, other);
        }

    }

}
