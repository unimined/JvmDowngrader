package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Objects;

public class J_N_URLDecoder {

    @Stub(value = JavaVersion.VERSION_1_10, desc = "Ljava/net/URLDecoder;")
    public static String decode(String s, Charset charset) throws UnsupportedEncodingException {
        return URLDecoder.decode(s, charset.name());
    }
}
