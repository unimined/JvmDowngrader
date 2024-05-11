package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Adapter("Ljava/util/function/DoubleUnaryOperator;")
public interface J_U_F_DoubleUnaryOperator {

    static J_U_F_DoubleUnaryOperator identity() {
        return t -> t;
    }

    double applyAsDouble(double operand);

    J_U_F_DoubleUnaryOperator compose(J_U_F_DoubleUnaryOperator before);

    J_U_F_DoubleUnaryOperator andThen(J_U_F_DoubleUnaryOperator after);

    class DoubleUnaryOperatorDefaults {

        @Stub(abstractDefault = true)
        public static J_U_F_DoubleUnaryOperator compose(final J_U_F_DoubleUnaryOperator f1, final J_U_F_DoubleUnaryOperator f2) {
            Objects.requireNonNull(f2);
            return new J_U_F_DoubleUnaryOperator.DoubleUnaryOperatorAdapter() {
                @Override
                public double applyAsDouble(double operand) {
                    return f1.applyAsDouble(f2.applyAsDouble(operand));
                }
            };
        }

        @Stub(abstractDefault = true)
        public static J_U_F_DoubleUnaryOperator andThen(final J_U_F_DoubleUnaryOperator f1, final J_U_F_DoubleUnaryOperator f2) {
            Objects.requireNonNull(f2);
            return compose(f2, f1);
        }
    }

    class DoubleUnaryOperatorStatic {

        @Stub(ref = @Ref("Ljava/util/function/DoubleUnaryOperator;"))
        public static J_U_F_DoubleUnaryOperator identity() {
            return new J_U_F_DoubleUnaryOperator.DoubleUnaryOperatorAdapter() {
                @Override
                public double applyAsDouble(double operand) {
                    return operand;
                }
            };
        }

    }

    abstract class DoubleUnaryOperatorAdapter implements J_U_F_DoubleUnaryOperator {

        @Override
        public J_U_F_DoubleUnaryOperator compose(J_U_F_DoubleUnaryOperator before) {
            return DoubleUnaryOperatorDefaults.compose(this, before);
        }

        @Override
        public J_U_F_DoubleUnaryOperator andThen(J_U_F_DoubleUnaryOperator after) {
            return DoubleUnaryOperatorDefaults.andThen(this, after);
        }

    }

}
