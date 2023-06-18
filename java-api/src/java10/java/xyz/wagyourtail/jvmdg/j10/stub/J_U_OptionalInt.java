package xyz.wagyourtail.jvmdg.j10.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.NoSuchElementException;
import java.util.OptionalInt;

public class J_U_OptionalInt {

    @Stub(opcVers = Opcodes.V10)
    public static int orElseThrow(OptionalInt optional) {
        if (optional.isPresent()) {
            return optional.getAsInt();
        }
        throw new NoSuchElementException("No value present");
    }

}
