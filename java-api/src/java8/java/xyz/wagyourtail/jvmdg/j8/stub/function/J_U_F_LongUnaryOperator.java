package xyz.wagyourtail.jvmdg.j8.stub.function;

import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Adapter("Ljava/util/function/LongUnaryOperator;")
public interface J_U_F_LongUnaryOperator {

    long applyAsLong(long operand);

    J_U_F_LongUnaryOperator compose(J_U_F_LongUnaryOperator before);

    J_U_F_LongUnaryOperator andThen(J_U_F_LongUnaryOperator after);

    class LongUnaryOperatorDefault {

        @Stub(abstractDefault = true)
        public static J_U_F_LongUnaryOperator compose(final J_U_F_LongUnaryOperator f1, final J_U_F_LongUnaryOperator f2) {
            Objects.requireNonNull(f2);
            return new J_U_F_LongUnaryOperator.LongUnaryOperatorAdapter() {
                @Override
                public long applyAsLong(long v) {
                    return f1.applyAsLong(f2.applyAsLong(v));
                }
            };
        }

        @Stub(abstractDefault = true)
        public static J_U_F_LongUnaryOperator andThen(final J_U_F_LongUnaryOperator f1, final J_U_F_LongUnaryOperator f2) {
            Objects.requireNonNull(f2);
            return compose(f2, f1);
        }

    }

    class LongUnaryOperatorStatic {

        @Stub(ref = @Ref("Ljava/util/function/LongUnaryOperator;"))
        public static J_U_F_LongUnaryOperator identity() {
            return new J_U_F_LongUnaryOperator.LongUnaryOperatorAdapter() {
                @Override
                public long applyAsLong(long operand) {
                    return operand;
                }
            };
        }

    }

    abstract class LongUnaryOperatorAdapter implements J_U_F_LongUnaryOperator {

        @Override
        public J_U_F_LongUnaryOperator compose(J_U_F_LongUnaryOperator before) {
            return LongUnaryOperatorDefault.compose(this, before);
        }

        @Override
        public J_U_F_LongUnaryOperator andThen(J_U_F_LongUnaryOperator after) {
            return LongUnaryOperatorDefault.andThen(this, after);
        }

    }

}
