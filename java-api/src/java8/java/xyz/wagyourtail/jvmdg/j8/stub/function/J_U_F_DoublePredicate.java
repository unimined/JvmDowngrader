package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

@J_L_FunctionalInterface
@Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/function/DoublePredicate;"))
public interface J_U_F_DoublePredicate {

    boolean test(double value);

    default J_U_F_DoublePredicate and(J_U_F_DoublePredicate other) {
        return (value) -> test(value) && other.test(value);
    }

    default J_U_F_DoublePredicate negate() {
        return (value) -> !test(value);
    }

    default J_U_F_DoublePredicate or(J_U_F_DoublePredicate other) {
        return (value) -> test(value) || other.test(value);
    }

}
