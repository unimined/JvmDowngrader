package xyz.wagyourtail.jvmdg.j10.stub.java_base;

import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.nio.charset.Charset;

@InitRewrite(ref = @Ref(value = "Ljava/util/Scanner;", member = "<init>", desc = "(Ljava/nio/channels/ReadableByteChannel;Ljava/nio/charset/Charset;)V"), to = "(Ljava/nio/channels/ReadableByteChannel;Ljava/lang/String;)V", keep = {0, -1})
@InitRewrite(ref = @Ref(value = "Ljava/util/Scanner;", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"), to = "(Ljava/io/File;Ljava/lang/String;)V", keep = {0, -1})
@InitRewrite(ref = @Ref(value = "Ljava/util/Scanner;", member = "<init>", desc = "(Ljava/io/InputStream;Ljava/nio/charset/Charset;)V"), to = "(Ljava/io/InputStream;Ljava/lang/String;)V", keep = {0, -1})
public class J_U_Scanner {

    // charset ctor arg -> charset.name()
    @InitArg(target = "(Ljava/nio/channels/ReadableByteChannel;Ljava/nio/charset/Charset;)V", slot = 1)
    @InitArg(target = "(Ljava/io/File;Ljava/nio/charset/Charset;)V", slot = 1)
    @InitArg(target = "(Ljava/io/InputStream;Ljava/nio/charset/Charset;)V", slot = 1)
    public static String name(Charset cs) {
        return cs.name();
    }

}
