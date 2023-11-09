package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j8.stub.stream.J_U_S_IntStream;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_CharSequence {

    @Stub
    public static J_U_S_IntStream chars(CharSequence cs) {
        throw MissingStubError.create();
    }

    @Stub
    public static J_U_S_IntStream codePoints(CharSequence cs) {
        throw MissingStubError.create();
    }

}
