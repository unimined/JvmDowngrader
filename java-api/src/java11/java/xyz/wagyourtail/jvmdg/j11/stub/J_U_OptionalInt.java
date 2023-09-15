package xyz.wagyourtail.jvmdg.j11.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Optional;
import java.util.OptionalInt;

public class J_U_OptionalInt {

    @Stub(opcVers = Opcodes.V11)
    public static boolean isEmpty(OptionalInt optional) {
        return !optional.isPresent();
    }

}
