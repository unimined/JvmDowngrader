package xyz.wagyourtail.jvmdg.j8.stub.function;

import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Adapter("Ljava/util/function/IntUnaryOperator;")
public interface J_U_F_IntUnaryOperator {

    int applyAsInt(int operand);

    J_U_F_IntUnaryOperator compose(J_U_F_IntUnaryOperator before);

    J_U_F_IntUnaryOperator andThen(J_U_F_IntUnaryOperator after);

    class IntUnaryOperatorDefaults {

        @Stub(abstractDefault = true)
        public static J_U_F_IntUnaryOperator compose(final J_U_F_IntUnaryOperator f1, final J_U_F_IntUnaryOperator f2) {
            Objects.requireNonNull(f2);
            return new J_U_F_IntUnaryOperator.IntUnaryOperatorAdapter() {
                @Override
                public int applyAsInt(int operand) {
                    return f1.applyAsInt(f2.applyAsInt(operand));
                }
            };
        }

        @Stub(abstractDefault = true)
        public static J_U_F_IntUnaryOperator andThen(final J_U_F_IntUnaryOperator f1, final J_U_F_IntUnaryOperator f2) {
            Objects.requireNonNull(f2);
            return compose(f2, f1);
        }

    }

    class IntUnaryOperatorStatic {

        @Stub(ref = @Ref("Ljava/util/function/IntUnaryOperator;"))
        public static J_U_F_IntUnaryOperator identity() {
            return new J_U_F_IntUnaryOperator.IntUnaryOperatorAdapter() {
                @Override
                public int applyAsInt(int operand) {
                    return operand;
                }
            };
        }

    }

    abstract class IntUnaryOperatorAdapter implements J_U_F_IntUnaryOperator {

        @Override
        public J_U_F_IntUnaryOperator compose(J_U_F_IntUnaryOperator before) {
            return IntUnaryOperatorDefaults.compose(this, before);
        }

        @Override
        public J_U_F_IntUnaryOperator andThen(J_U_F_IntUnaryOperator after) {
            return IntUnaryOperatorDefaults.andThen(this, after);
        }

    }

}
