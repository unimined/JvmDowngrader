package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/UnaryOperator"))
public interface J_U_F_UnaryOperator<T> extends J_U_F_Function<T, T> {

    static <T> J_U_F_UnaryOperator<T> identity() {
        return t -> t;
    }

}
