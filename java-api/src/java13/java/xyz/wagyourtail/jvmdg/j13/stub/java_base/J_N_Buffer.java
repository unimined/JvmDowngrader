package xyz.wagyourtail.jvmdg.j13.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Coerce;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.Buffer;

public class J_N_Buffer {

    @Stub
    public static Buffer slice(Buffer buffer, int index, int length) {
        int pos = buffer.position();
        buffer.position(index);
        var sub = buffer.slice().limit(length);
        buffer.position(pos);
        return sub;
    }

}
