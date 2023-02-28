package xyz.wagyourtail.jvmdg.internal.mods.stub._14;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.PrintStream;

public class J_I_PrintStream {

    @Stub(JavaVersion.VERSION_14)
    public static void write(PrintStream stream, byte[] buf) {
        stream.write(buf, 0, buf.length);
    }

    @Stub(JavaVersion.VERSION_14)
    public static void writeBytes(PrintStream stream, byte[] buf) {
        stream.write(buf, 0, buf.length);
    }
}
