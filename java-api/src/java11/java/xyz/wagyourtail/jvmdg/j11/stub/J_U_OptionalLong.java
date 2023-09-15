package xyz.wagyourtail.jvmdg.j11.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.OptionalInt;
import java.util.OptionalLong;

public class J_U_OptionalLong {

    @Stub(opcVers = Opcodes.V11)
    public static boolean isEmpty(OptionalLong optional) {
        return !optional.isPresent();
    }

}
