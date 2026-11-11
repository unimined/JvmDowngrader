package xyz.wagyourtail.jvmdg.j10.stub.java_base;

import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.nio.charset.Charset;

@InitRewrite(ref = @Ref(value = "Ljava/util/Formatter;", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;Ljava/util/Locale;)V"), to = "(Ljava/io/File;Ljava/lang/String;Ljava/util/Locale;)V", keep = {0, -1, 2})
@InitRewrite(ref = @Ref(value = "Ljava/util/Formatter;", member = "<init>", desc = "(Ljava/io/OutputStream;Ljava/nio/charset/Charset;Ljava/util/Locale;)V"), to = "(Ljava/io/OutputStream;Ljava/lang/String;Ljava/util/Locale;)V", keep = {0, -1, 2})
@InitRewrite(ref = @Ref(value = "Ljava/util/Formatter;", member = "<init>", desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;Ljava/util/Locale;)V"), to = "(Ljava/lang/String;Ljava/lang/String;Ljava/util/Locale;)V", keep = {0, -1, 2})
public class J_U_Formatter {

    // charset ctor arg -> charset.name()
    @InitArg(target = "(Ljava/io/File;Ljava/nio/charset/Charset;Ljava/util/Locale;)V", slot = 1)
    @InitArg(target = "(Ljava/io/OutputStream;Ljava/nio/charset/Charset;Ljava/util/Locale;)V", slot = 1)
    @InitArg(target = "(Ljava/lang/String;Ljava/nio/charset/Charset;Ljava/util/Locale;)V", slot = 1)
    public static String name(Charset cs) {
        return cs.name();
    }

}
