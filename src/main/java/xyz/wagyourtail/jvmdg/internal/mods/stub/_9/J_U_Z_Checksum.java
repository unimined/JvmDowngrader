package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.nio.ByteBuffer;
import java.util.zip.Checksum;

public class J_U_Z_Checksum {

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/zip/Checksum;", subtypes = true)
    public static void update(Checksum checksum, byte[] b) {
        checksum.update(b, 0, b.length);
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/zip/Checksum;", subtypes = true)
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
