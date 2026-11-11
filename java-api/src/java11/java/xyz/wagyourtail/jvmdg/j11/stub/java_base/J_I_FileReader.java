package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.io.FileReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

@InitRewrite(ref = @Ref(value = "Ljava/io/FileReader;", member = "<init>", desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"), to = "(Ljava/lang/String;)V", keep = {0})
@InitRewrite(ref = @Ref(value = "Ljava/io/FileReader;", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"), to = "(Ljava/io/File;)V", keep = {0})
public class J_I_FileReader {
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle sdGetter;
    private static final MethodHandle decSetter;

    static {
        MethodHandle seGet = null;
        MethodHandle decSet = null;
        try {
            Class<?> sd = Class.forName("sun.nio.cs.StreamDecoder");
            seGet = IMPL_LOOKUP.findGetter(FileReader.class, "sd", sd);
            decSet = IMPL_LOOKUP.findSetter(sd, "decoder", CharsetDecoder.class);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        sdGetter = seGet;
        decSetter = decSet;
    }

    // FileReader(String/File,Charset) -> FileReader(String/File) then setCharset
    @InitPost(target = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V")
    @InitPost(target = "(Ljava/io/File;Ljava/nio/charset/Charset;)V")
    public static void setCharset(FileReader fr, Charset charset) {
        try {
            Object sd = sdGetter.invoke(fr);
            decSetter.invoke(sd, charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
        } catch (Throwable t) {
            Utils.sneakyThrow(t);
        }
    }

}
