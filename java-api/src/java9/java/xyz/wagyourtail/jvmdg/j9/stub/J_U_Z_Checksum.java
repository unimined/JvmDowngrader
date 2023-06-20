package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.ByteBuffer;
import java.util.zip.Checksum;

public class J_U_Z_Checksum {

    @Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/util/zip/Checksum;"), subtypes = true)
    public static void update(Checksum checksum, byte[] b) {
        checksum.update(b, 0, b.length);
    }

    @Stub(opcVers = Opcodes.V9, ref = @Ref("Ljava/util/zip/Checksum;"), subtypes = true)
    public static void update(Checksum checksum, ByteBuffer buffer) {
        if (buffer.hasArray()) {
            checksum.update(buffer.array(), buffer.position() + buffer.arrayOffset(), buffer.remaining());
        } else {
            byte[] b = new byte[buffer.remaining()];
            buffer.get(b);
            checksum.update(b, 0, b.length);
        }
        buffer.position(buffer.limit());
    }

}
