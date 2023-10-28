package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/IntPredicate"))
public interface J_U_F_IntPredicate {

    boolean test(int value);

    J_U_F_IntPredicate and(J_U_F_IntPredicate other);

    J_U_F_IntPredicate negate();

    J_U_F_IntPredicate or(J_U_F_IntPredicate other);

    class IntPredicateDefaults {

        @Stub(abstractDefault = true)
        public static J_U_F_IntPredicate and(final J_U_F_IntPredicate self, final J_U_F_IntPredicate other) {
            return new J_U_F_IntPredicate.IntPredicateAdapter() {
                @Override
                public boolean test(int value) {
                    return self.test(value) && other.test(value);
                }
            };
        }

        @Stub(abstractDefault = true)
        public static J_U_F_IntPredicate negate(final J_U_F_IntPredicate self) {
            return new J_U_F_IntPredicate.IntPredicateAdapter() {
                @Override
                public boolean test(int value) {
                    return !self.test(value);
                }
            };
        }

        @Stub(abstractDefault = true)
        public static J_U_F_IntPredicate or(final J_U_F_IntPredicate self, final J_U_F_IntPredicate other) {
            return new J_U_F_IntPredicate.IntPredicateAdapter() {
                @Override
                public boolean test(int value) {
                    return self.test(value) || other.test(value);
                }
            };
        }


    }

    abstract class IntPredicateAdapter implements J_U_F_IntPredicate {

        @Override
        public J_U_F_IntPredicate and(J_U_F_IntPredicate other) {
            return IntPredicateDefaults.and(this, other);
        }

        @Override
        public J_U_F_IntPredicate negate() {
            return IntPredicateDefaults.negate(this);
        }

        @Override
        public J_U_F_IntPredicate or(J_U_F_IntPredicate other) {
            return IntPredicateDefaults.or(this, other);
        }

    }
}
