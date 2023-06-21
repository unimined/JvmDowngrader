package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

@J_L_FunctionalInterface
@Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/function/IntPredicate"))
public interface J_U_F_IntPredicate {

    boolean test(int value);

    default J_U_F_IntPredicate and(J_U_F_IntPredicate other) {
        return (value) -> test(value) && other.test(value);
    }

    default J_U_F_IntPredicate negate() {
        return (value) -> !test(value);
    }

    default J_U_F_IntPredicate or(J_U_F_IntPredicate other) {
        return (value) -> test(value) || other.test(value);
    }

}
