package xyz.wagyourtail.jvmdg.j11.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class J_U_Z_Inflater {

    @Stub(opcVers = Opcodes.V11)
    public static int inflate(Inflater inf, ByteBuffer buf) throws DataFormatException {
        return inf.inflate(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
    }

    @Stub(opcVers = Opcodes.V11)
    public static void setDictionary(Inflater inf, ByteBuffer buf) {
        inf.setDictionary(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
    }

    @Stub(opcVers = Opcodes.V11)
    public static void setInput(Deflater def, ByteBuffer buf) {
        throw new UnsupportedOperationException(
            "JVMDowngrader, setInput(ByteBuffer) is not supported because it's impure.");
    }

}
