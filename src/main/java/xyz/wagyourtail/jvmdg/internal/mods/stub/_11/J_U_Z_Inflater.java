package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class J_U_Z_Inflater {

    @Stub(JavaVersion.VERSION_11)
    public static int inflate(Inflater inf, ByteBuffer buf) throws DataFormatException {
        return inf.inflate(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
    }

    @Stub(JavaVersion.VERSION_11)
    public static void setDictionary(Inflater inf, ByteBuffer buf) {
        inf.setDictionary(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
    }

    @Stub(JavaVersion.VERSION_11)
    public static void setInput(Deflater def, ByteBuffer buf) {
        throw new UnsupportedOperationException("JVMDowngrader, setInput(ByteBuffer) is not supported because it's impure.");
    }

}
