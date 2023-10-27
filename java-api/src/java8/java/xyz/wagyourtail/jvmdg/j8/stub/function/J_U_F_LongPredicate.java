package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/LongPredicate"))
public interface J_U_F_LongPredicate {

    boolean test(long value);

    default J_U_F_LongPredicate and(J_U_F_LongPredicate other) {
        return (value) -> test(value) && other.test(value);
    }

    default J_U_F_LongPredicate negate() {
        return (value) -> !test(value);
    }

    default J_U_F_LongPredicate or(J_U_F_LongPredicate other) {
        return (value) -> test(value) || other.test(value);
    }

}
