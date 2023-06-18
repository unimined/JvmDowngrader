package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_R_Constructor {

    @Stub(opcVers = Opcodes.V1_8)
    public static int getParameterCount(java.lang.reflect.Constructor<?> self) {
        return self.getParameterTypes().length;
    }

    // getAnnotatedReturnType

    // getAnnotatedReceiverType
}
