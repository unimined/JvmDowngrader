package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.*;
import java.nio.charset.Charset;

public class J_I_PrintWriter {

    @Stub(value = JavaVersion.VERSION_1_10, desc = "Ljava/io/PrintWriter;<init>")
    public static PrintWriter init(OutputStream out, boolean autoFlush, Charset encoding) {
        return new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, encoding)), autoFlush);
    }
}
