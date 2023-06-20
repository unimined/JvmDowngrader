package xyz.wagyourtail.jvmdg.j14.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.PrintStream;

public class J_I_PrintStream {

    @Stub(opcVers = Opcodes.V14)
    public static void write(PrintStream stream, byte[] buf) {
        stream.write(buf, 0, buf.length);
    }

    @Stub(opcVers = Opcodes.V14)
    public static void writeBytes(PrintStream stream, byte[] buf) {
        stream.write(buf, 0, buf.length);
    }

}
