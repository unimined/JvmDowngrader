package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/function/DoubleUnaryOperator;"))
public interface J_U_F_DoubleUnaryOperator {

    double applyAsDouble(double operand);

    default J_U_F_DoubleUnaryOperator compose(J_U_F_DoubleUnaryOperator before) {
        Objects.requireNonNull(before);
        return (double v) -> applyAsDouble(before.applyAsDouble(v));
    }

    default J_U_F_DoubleUnaryOperator andThen(J_U_F_DoubleUnaryOperator after) {
        Objects.requireNonNull(after);
        return (double t) -> after.applyAsDouble(applyAsDouble(t));
    }

    static J_U_F_DoubleUnaryOperator identity() {
        return t -> t;
    }

}
