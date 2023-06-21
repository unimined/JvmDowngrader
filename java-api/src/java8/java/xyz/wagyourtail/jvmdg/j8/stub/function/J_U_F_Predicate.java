package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@J_L_FunctionalInterface
@Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/function/Predicate"))
public interface J_U_F_Predicate<T> {

    boolean test(T t);

    default J_U_F_Predicate<T> and(J_U_F_Predicate<? super T> other) {
        return (t) -> test(t) && other.test(t);
    }

    default J_U_F_Predicate<T> negate() {
        return (t) -> !test(t);
    }

    default J_U_F_Predicate<T> or(J_U_F_Predicate<? super T> other) {
        return (t) -> test(t) || other.test(t);
    }

    static <T> J_U_F_Predicate<T> isEqual(Object targetRef) {
        return (null == targetRef)
                ? Objects::isNull
                : object -> targetRef.equals(object);
    }

    static <T> J_U_F_Predicate<T> not(J_U_F_Predicate<? super T> target) {
        return target.negate();
    }
}
