package xyz.wagyourtail.jvmdg.j11.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Optional;
import java.util.OptionalDouble;

public class J_U_OptionalDouble {

    @Stub(opcVers = Opcodes.V11)
    public static boolean isEmpty(OptionalDouble optional) {
        return !optional.isPresent();
    }

}

