package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.nio.ByteBuffer;
import java.util.zip.Deflater;

public class J_U_Z_Deflater {

    @Stub(JavaVersion.VERSION_11)
    public static int deflate(Deflater def, ByteBuffer buf) {
        return def.deflate(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
    }

    @Stub(JavaVersion.VERSION_11)
    public static int deflate(Deflater def, ByteBuffer buf, int flush) {
        return def.deflate(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining(), flush);
    }

    @Stub(JavaVersion.VERSION_11)
    public static void setInput(Deflater def, ByteBuffer buf) {
        throw new UnsupportedOperationException("JVMDowngrader, setInput(ByteBuffer) is not supported because it's impure.");
    }

}
