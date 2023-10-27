package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/DoubleUnaryOperator;"))
public interface J_U_F_DoubleUnaryOperator {

    double applyAsDouble(double operand);

    J_U_F_DoubleUnaryOperator compose(J_U_F_DoubleUnaryOperator before);

    J_U_F_DoubleUnaryOperator andThen(J_U_F_DoubleUnaryOperator after);

    class DoubleUnaryOperatorDefaults {

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static J_U_F_DoubleUnaryOperator compose(final J_U_F_DoubleUnaryOperator f1, final J_U_F_DoubleUnaryOperator f2) {
            Objects.requireNonNull(f2);
            return new J_U_F_DoubleUnaryOperator() {
                @Override
                public double applyAsDouble(double operand) {
                    return f1.applyAsDouble(f2.applyAsDouble(operand));
                }

                @Override
                public J_U_F_DoubleUnaryOperator compose(J_U_F_DoubleUnaryOperator before) {
                    return DoubleUnaryOperatorDefaults.compose(this, before);
                }

                @Override
                public J_U_F_DoubleUnaryOperator andThen(J_U_F_DoubleUnaryOperator after) {
                    return DoubleUnaryOperatorDefaults.andThen(this, after);
                }
            };
        }

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static J_U_F_DoubleUnaryOperator andThen(final J_U_F_DoubleUnaryOperator f1, final J_U_F_DoubleUnaryOperator f2) {
            Objects.requireNonNull(f2);
            return compose(f2, f1);
        }
    }

    class DoubleUnaryOperatorStatic {

        @Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/function/DoubleUnaryOperator;"))
        public static J_U_F_DoubleUnaryOperator identity() {
            return new J_U_F_DoubleUnaryOperator() {
                @Override
                public double applyAsDouble(double operand) {
                    return operand;
                }

                @Override
                public J_U_F_DoubleUnaryOperator compose(J_U_F_DoubleUnaryOperator before) {
                    return DoubleUnaryOperatorDefaults.compose(this, before);
                }

                @Override
                public J_U_F_DoubleUnaryOperator andThen(J_U_F_DoubleUnaryOperator after) {
                    return DoubleUnaryOperatorDefaults.andThen(this, after);
                }
            };
        }

    }

}
