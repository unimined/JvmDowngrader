package xyz.wagyourtail.jvmdg.j11.stub.java_base;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Optional;
import java.util.OptionalDouble;

public class J_U_OptionalDouble {

    @Stub
    public static boolean isEmpty(OptionalDouble optional) {
        return !optional.isPresent();
    }

}

