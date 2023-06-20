package xyz.wagyourtail.jvmdg.j13.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.*;

public class J_N_Buffer {


    @Stub(opcVers = Opcodes.V13, subtypes = true, returnDecendant = true)
    public static Buffer slice(Buffer buffer, int index, int length) {
        int pos = buffer.position();
        buffer.position(index);
        var sub = buffer.slice().limit(length);
        buffer.position(pos);
        return sub;
    }

}
