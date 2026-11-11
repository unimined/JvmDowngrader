package xyz.wagyourtail.jvmdg.j10.stub.java_base;

import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

@InitRewrite(ref = @Ref(value = "Ljava/io/PrintWriter;", member = "<init>", desc = "(Ljava/io/OutputStream;ZLjava/nio/charset/Charset;)V"), to = "(Ljava/io/Writer;Z)V", keep = {-1, 1})
@InitRewrite(ref = @Ref(value = "Ljava/io/PrintWriter;", member = "<init>", desc = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"), to = "(Ljava/io/Writer;Z)V", keep = {-1, -1})
@InitRewrite(ref = @Ref(value = "Ljava/io/PrintWriter;", member = "<init>", desc = "(Ljava/io/File;Ljava/nio/charset/Charset;)V"), to = "(Ljava/io/Writer;Z)V", keep = {-1, -1})
public class J_I_PrintWriter {

    // PrintWriter(OutputStream,Z,Charset) -> PrintWriter(BufferedWriter(OutputStreamWriter(os,cs)),Z)
    @InitArg(target = "(Ljava/io/OutputStream;ZLjava/nio/charset/Charset;)V", slot = 0)
    public static Writer wrap(OutputStream os, Charset cs) {
        return new BufferedWriter(new OutputStreamWriter(os, cs));
    }

    // PrintWriter(String/File,Charset) -> PrintWriter(BufferedWriter(OutputStreamWriter(FileOutputStream(f),cs)),true)
    @InitArg(target = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V", slot = 0)
    public static Writer wrap(String f, Charset cs) throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), cs));
    }

    @InitArg(target = "(Ljava/io/File;Ljava/nio/charset/Charset;)V", slot = 0)
    public static Writer wrap(File f, Charset cs) throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), cs));
    }

    @InitArg(target = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V", slot = 1)
    @InitArg(target = "(Ljava/io/File;Ljava/nio/charset/Charset;)V", slot = 1)
    public static boolean autoFlush() {
        return true;
    }

}
