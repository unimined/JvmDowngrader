package xyz.wagyourtail.jvmdg.j11.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Optional;

public class J_U_Optional {

    @Stub(opcVers = Opcodes.V11)
    public static boolean isEmpty(Optional<?> optional) {
        return !optional.isPresent();
    }

}
