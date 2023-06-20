package xyz.wagyourtail.jvmdg.j11.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.ByteBuffer;
import java.util.zip.Deflater;

public class J_U_Z_Deflater {

    @Stub(opcVers = Opcodes.V11)
    public static int deflate(Deflater def, ByteBuffer buf) {
        return def.deflate(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
    }

    @Stub(opcVers = Opcodes.V11)
    public static int deflate(Deflater def, ByteBuffer buf, int flush) {
        return def.deflate(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining(), flush);
    }

    @Stub(opcVers = Opcodes.V11)
    public static void setInput(Deflater def, ByteBuffer buf) {
        throw new UnsupportedOperationException(
            "JVMDowngrader, setInput(ByteBuffer) is not supported because it's impure.");
    }

}
