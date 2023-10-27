package xyz.wagyourtail.jvmdg.j11.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.ByteArrayOutputStream;

public class J_I_ByteArrayOutputStream {

    @Stub
    public static void writeBytes(ByteArrayOutputStream baos, byte[] b) {
        baos.write(b, 0, b.length);
    }

}
