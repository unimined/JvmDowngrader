package xyz.wagyourtail.jvmdg.j8.stub.function;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.J_L_FunctionalInterface;
import xyz.wagyourtail.jvmdg.version.Stub;

@J_L_FunctionalInterface
@Stub(ref = @Ref("Ljava/util/function/LongConsumer"))
public interface J_U_F_LongConsumer {

    void accept(long value);

    default J_U_F_LongConsumer andThen(J_U_F_LongConsumer after) {
        return (long t) -> {
            accept(t);
            after.accept(t);
        };
    }

}
