package xyz.wagyourtail.jvmdg.j10.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.NoSuchElementException;
import java.util.OptionalDouble;

public class J_U_OptionalDouble {

    @Stub(javaVersion = Opcodes.V10)
    public static double orElseThrow(OptionalDouble optional) {
        if (optional.isPresent()) {
            return optional.getAsDouble();
        }
        throw new NoSuchElementException("No value present");
    }

}
