package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/DoublePredicate;"))
public interface J_U_F_DoublePredicate {

    boolean test(double value);

    J_U_F_DoublePredicate and(J_U_F_DoublePredicate other);

    J_U_F_DoublePredicate negate();

    J_U_F_DoublePredicate or(J_U_F_DoublePredicate other);

    class DoublePredicateDefaults {

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static J_U_F_DoublePredicate and(final J_U_F_DoublePredicate self, final J_U_F_DoublePredicate other) {
            Objects.requireNonNull(other);
            return new J_U_F_DoublePredicate() {
                @Override
                public boolean test(double value) {
                    return self.test(value) && other.test(value);
                }

                @Override
                public J_U_F_DoublePredicate and(J_U_F_DoublePredicate other) {
                    return DoublePredicateDefaults.and(this, other);
                }

                @Override
                public J_U_F_DoublePredicate negate() {
                    return DoublePredicateDefaults.negate(this);
                }

                @Override
                public J_U_F_DoublePredicate or(J_U_F_DoublePredicate other) {
                    return DoublePredicateDefaults.or(this, other);
                }
            };
        }

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static J_U_F_DoublePredicate negate(final J_U_F_DoublePredicate self) {
            return new J_U_F_DoublePredicate() {
                @Override
                public boolean test(double value) {
                    return !self.test(value);
                }

                @Override
                public J_U_F_DoublePredicate and(J_U_F_DoublePredicate other) {
                    return DoublePredicateDefaults.and(this, other);
                }

                @Override
                public J_U_F_DoublePredicate negate() {
                    return self;
                }

                @Override
                public J_U_F_DoublePredicate or(J_U_F_DoublePredicate other) {
                    return DoublePredicateDefaults.or(this, other);
                }
            };
        }

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static J_U_F_DoublePredicate or(final J_U_F_DoublePredicate self, final J_U_F_DoublePredicate other) {
            Objects.requireNonNull(other);
            return new J_U_F_DoublePredicate() {
                @Override
                public boolean test(double value) {
                    return self.test(value) || other.test(value);
                }

                @Override
                public J_U_F_DoublePredicate and(J_U_F_DoublePredicate other) {
                    return DoublePredicateDefaults.and(this, other);
                }

                @Override
                public J_U_F_DoublePredicate negate() {
                    return DoublePredicateDefaults.negate(this);
                }

                @Override
                public J_U_F_DoublePredicate or(J_U_F_DoublePredicate other) {
                    return DoublePredicateDefaults.or(this, other);
                }
            };
        }

    }

}
