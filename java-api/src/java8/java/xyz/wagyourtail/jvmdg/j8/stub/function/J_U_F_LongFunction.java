package xyz.wagyourtail.jvmdg.j8.stub.function;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/LongFunction"))
public interface J_U_F_LongFunction<R> {

    R apply(long value);

}
