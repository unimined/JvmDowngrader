package xyz.wagyourtail.jvmdg.j10.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.NoSuchElementException;
import java.util.OptionalLong;

public class J_U_OptionalLong {

    @Stub(opcVers = Opcodes.V10)
    public static long orElseThrow(OptionalLong optional) {
        if (optional.isPresent()) {
            return optional.getAsLong();
        }
        throw new NoSuchElementException("No value present");
    }

}
