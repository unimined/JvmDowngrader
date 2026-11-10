package xyz.wagyourtail.jvmdg.j13.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.Buffer;

public class J_N_Buffer {

    @Stub
    public static Buffer slice(Buffer buffer, int index, int length) {
        if (index < 0 || length < 0 || index > buffer.limit() - length) {
            throw new IndexOutOfBoundsException(
                "index=" + index + ", length=" + length + ", limit=" + buffer.limit());
        }
        int pos = buffer.position();
        buffer.position(index);
        var sub = buffer.slice().limit(length);
        buffer.position(pos);
        return sub;
    }

}
