package xyz.wagyourtail.jvmdg.j18.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

public class J_N_C_Charset {

    @Stub(ref = @Ref("Ljava/nio/charset/Charset;"))
    public static Charset forName(String name, Charset defaultCharset) {
        try {
            return Charset.forName(name);
        } catch (UnsupportedCharsetException e) {
            return defaultCharset;
        }
    }

    /**
     * JEP 400
     */
    @Stub(ref = @Ref("java/nio/charset/Charset"))
    public static Charset defaultCharset() {
        return StandardCharsets.UTF_8;
    }


}
