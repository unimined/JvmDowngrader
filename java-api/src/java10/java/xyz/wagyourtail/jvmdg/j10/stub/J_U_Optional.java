package xyz.wagyourtail.jvmdg.j10.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.NoSuchElementException;
import java.util.Optional;

public class J_U_Optional {

    @Stub(opcVers = Opcodes.V10)
    public static <T> T orElseThrow(Optional<T> optional) {
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NoSuchElementException("No value present");
    }

}
