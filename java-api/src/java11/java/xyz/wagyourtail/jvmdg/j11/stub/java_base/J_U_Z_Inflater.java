package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class J_U_Z_Inflater {

    @Stub
    public static int inflate(Inflater inf, ByteBuffer buf) throws DataFormatException {
        return inf.inflate(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
    }

    @Stub
    public static void setDictionary(Inflater inf, ByteBuffer buf) {
        inf.setDictionary(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
    }

    @Stub
    public static void setInput(Inflater inf, ByteBuffer buf) {
        byte[] remain = new byte[buf.remaining()];
        buf.get(remain);
        inf.setInput(remain);
    }

}
