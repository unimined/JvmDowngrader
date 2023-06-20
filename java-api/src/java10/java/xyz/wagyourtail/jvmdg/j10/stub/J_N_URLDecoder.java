package xyz.wagyourtail.jvmdg.j10.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

public class J_N_URLDecoder {

    @Stub(opcVers = Opcodes.V10, ref = @Ref("Ljava/net/URLDecoder;"))
    public static String decode(String s, Charset charset) throws UnsupportedEncodingException {
        return URLDecoder.decode(s, charset.name());
    }

}
