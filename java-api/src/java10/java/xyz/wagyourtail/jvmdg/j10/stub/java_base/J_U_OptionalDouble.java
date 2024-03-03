package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.NoSuchElementException;
import java.util.OptionalDouble;

public class J_U_OptionalDouble {

    @Stub
    public static double orElseThrow(OptionalDouble optional) {
        if (optional.isPresent()) {
            return optional.getAsDouble();
        }
        throw new NoSuchElementException("No value present");
    }

}
