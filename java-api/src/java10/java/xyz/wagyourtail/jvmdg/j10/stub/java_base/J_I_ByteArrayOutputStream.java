package xyz.wagyourtail.jvmdg.j10.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

public class J_I_ByteArrayOutputStream {

    @Stub
    public static String toString(ByteArrayOutputStream baos, Charset charset) {
        return new String(baos.toByteArray(), charset);
    }

}
