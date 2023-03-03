package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.CharArrayWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Objects;

public class J_N_URLEncoder {

    @Stub(value = JavaVersion.VERSION_1_10, desc = "Ljava/net/URLEncoder;")
    public static String encode(String s, Charset charset) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, charset.name());
    }
}
