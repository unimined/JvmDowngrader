package xyz.wagyourtail.jvmdg.j11.stub.java_base;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Optional;

public class J_U_Optional {

    @Stub
    public static boolean isEmpty(Optional<?> optional) {
        return !optional.isPresent();
    }

}
