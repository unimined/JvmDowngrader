package xyz.wagyourtail.jvmdg.j10.stub.java_base;

import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.nio.charset.Charset;

@InitRewrite(ref = @Ref(value = "Ljava/io/PrintStream;", member = "<init>", desc = "(Ljava/io/OutputStream;ZLjava/nio/charset/Charset;)V"), to = "(Ljava/io/OutputStream;ZLjava/lang/String;)V", keep = {0, 1, -1})
@InitRewrite(ref = @Ref(value = "Ljava/io/PrintStream;", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"), to = "(Ljava/io/File;Ljava/lang/String;)V", keep = {0, -1})
@InitRewrite(ref = @Ref(value = "Ljava/io/PrintStream;", member = "<init>", desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"), to = "(Ljava/lang/String;Ljava/lang/String;)V", keep = {0, -1})
public class J_I_PrintStream {

    // charset ctor arg -> charset.name()
    @InitArg(target = "(Ljava/io/OutputStream;ZLjava/nio/charset/Charset;)V", slot = 2)
    @InitArg(target = "(Ljava/io/File;Ljava/nio/charset/Charset;)V", slot = 1)
    @InitArg(target = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V", slot = 1)
    public static String name(Charset cs) {
        return cs.name();
    }

}
