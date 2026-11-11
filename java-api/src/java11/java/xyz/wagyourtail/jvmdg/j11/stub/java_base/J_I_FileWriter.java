package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.io.FileWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

@InitRewrite(ref = @Ref(value = "Ljava/io/FileWriter;", member = "<init>", desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"), to = "(Ljava/lang/String;)V", keep = {0})
@InitRewrite(ref = @Ref(value = "Ljava/io/FileWriter;", member = "<init>", desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;Z)V"), to = "(Ljava/lang/String;Z)V", keep = {0, 2})
@InitRewrite(ref = @Ref(value = "Ljava/io/FileWriter;", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"), to = "(Ljava/io/File;)V", keep = {0})
@InitRewrite(ref = @Ref(value = "Ljava/io/FileWriter;", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;Z)V"), to = "(Ljava/io/File;Z)V", keep = {0, 2})
public class J_I_FileWriter {
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle seGetter;
    private static final MethodHandle encSetter;

    static {
        MethodHandle seGet = null;
        MethodHandle encSet = null;
        try {
            Class<?> se = Class.forName("sun.nio.cs.StreamEncoder");
            seGet = IMPL_LOOKUP.findGetter(FileWriter.class, "se", se);
            encSet = IMPL_LOOKUP.findSetter(se, "encoder", CharsetEncoder.class);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        seGetter = seGet;
        encSetter = encSet;
    }

    // FileWriter(String/File,Charset[,Z]) -> FileWriter(String/File[,Z]) then setCharset
    @InitPost(target = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V")
    @InitPost(target = "(Ljava/lang/String;Ljava/nio/charset/Charset;Z)V")
    @InitPost(target = "(Ljava/io/File;Ljava/nio/charset/Charset;)V")
    @InitPost(target = "(Ljava/io/File;Ljava/nio/charset/Charset;Z)V")
    public static void setCharset(FileWriter fw, Charset charset) {
        try {
            Object se = seGetter.invoke(fw);
            encSetter.invoke(se, charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
        } catch (Throwable t) {
            Utils.sneakyThrow(t);
        }
    }

}
