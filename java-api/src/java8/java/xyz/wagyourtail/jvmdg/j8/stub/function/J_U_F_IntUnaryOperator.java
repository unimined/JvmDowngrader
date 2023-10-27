package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/IntUnaryOperator;"))
public interface J_U_F_IntUnaryOperator {

    int applyAsInt(int operand);

    J_U_F_IntUnaryOperator compose(J_U_F_IntUnaryOperator before);

    J_U_F_IntUnaryOperator andThen(J_U_F_IntUnaryOperator after);

    class IntUnaryOperatorDefaults {

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static J_U_F_IntUnaryOperator compose(final J_U_F_IntUnaryOperator f1, final J_U_F_IntUnaryOperator f2) {
            Objects.requireNonNull(f2);
            return new J_U_F_IntUnaryOperator() {
                @Override
                public int applyAsInt(int operand) {
                    return f1.applyAsInt(f2.applyAsInt(operand));
                }

                @Override
                public J_U_F_IntUnaryOperator compose(J_U_F_IntUnaryOperator before) {
                    return IntUnaryOperatorDefaults.compose(this, before);
                }

                @Override
                public J_U_F_IntUnaryOperator andThen(J_U_F_IntUnaryOperator after) {
                    return IntUnaryOperatorDefaults.andThen(this, after);
                }
            };
        }

        @Stub(opcVers = Opcodes.V1_8, defaultMethod = true)
        public static J_U_F_IntUnaryOperator andThen(final J_U_F_IntUnaryOperator f1, final J_U_F_IntUnaryOperator f2) {
            Objects.requireNonNull(f2);
            return compose(f2, f1);
        }
    }

    class IntUnaryOperatorStatic {

        @Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/function/IntUnaryOperator;"))
        public static J_U_F_IntUnaryOperator identity() {
            return new J_U_F_IntUnaryOperator() {
                @Override
                public int applyAsInt(int operand) {
                    return operand;
                }

                @Override
                public J_U_F_IntUnaryOperator compose(J_U_F_IntUnaryOperator before) {
                    return IntUnaryOperatorDefaults.compose(this, before);
                }

                @Override
                public J_U_F_IntUnaryOperator andThen(J_U_F_IntUnaryOperator after) {
                    return IntUnaryOperatorDefaults.andThen(this, after);
                }
            };
        }

    }

}
