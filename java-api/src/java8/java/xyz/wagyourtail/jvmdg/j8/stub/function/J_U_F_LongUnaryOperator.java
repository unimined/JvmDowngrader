package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/LongUnaryOperator;"))
public interface J_U_F_LongUnaryOperator {

    static J_U_F_LongUnaryOperator identity() {
        return t -> t;
    }

    long applyAsLong(long operand);

    default J_U_F_LongUnaryOperator compose(J_U_F_LongUnaryOperator before) {
        return (long v) -> applyAsLong(before.applyAsLong(v));
    }

    default J_U_F_LongUnaryOperator andThen(J_U_F_LongUnaryOperator after) {
        return (long t) -> {
            return after.applyAsLong(applyAsLong(t));
        };
    }

}
