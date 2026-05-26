package xyz.wagyourtail.jvmdg.j26.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.UUID;

public class J_U_UUID {
    private static final SecureRandom random = new SecureRandom();

    @Stub(ref = @Ref("java/util/UUID"))
    public static UUID ofEpochMillis(long j) {
        if (j > 0x0000FFFFFFFFFFFFL) {
            throw new IllegalArgumentException("Supplied timestamp: " + j + " does not fit in 48 bytes");
        }
        byte[] bytes = new byte[18];
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        buff.putLong(j);
        byte[] bytes2 = new byte[10];
        random.nextBytes(bytes2);
        buff.put(8, bytes2);

        // Set version to 7
        buff.put(8, (byte) ((buff.get(8) & 0x0f) | 0x70));

        // Set variant to IETF
        buff.put(10, (byte) ((buff.get(10) & 0x3f) | 0x80));

        long msb = buff.getLong(2);
        long lsb = buff.getLong(10);
        return new UUID(msb, lsb);
    }

}
