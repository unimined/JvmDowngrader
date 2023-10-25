package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class J_I_InputStream {

    @Stub(opcVers = Opcodes.V9)
    public static byte[] readAllBytes(InputStream in) throws IOException {
        int readBytes = 0;
        byte[] bytes = new byte[8192];
        // read into bytes
        int read;
        while ((read = in.read(bytes, readBytes, bytes.length - readBytes)) != -1) {
            readBytes += read;
            if (readBytes == bytes.length) {
                byte[] old = bytes;
                bytes = new byte[readBytes << 1];
                System.arraycopy(old, 0, bytes, 0, readBytes);
            }
        }
        if (readBytes == bytes.length) return bytes;
        byte[] trimmed = new byte[readBytes];
        System.arraycopy(bytes, 0, trimmed, 0, readBytes);
        return trimmed;
    }

    @Stub(opcVers = Opcodes.V9)
    public static int readNBytes(InputStream in, byte[] b, int off, int len) throws IOException {
        J_U_Objects.checkFromIndexSize(off, len, b.length);

        int total = 0;
        while (total < len) {
            int read = in.read(b, off + total, len - total);
            if (read == -1) {
                break;
            }
            total += read;
        }
        return total;
    }

    @Stub(opcVers = Opcodes.V9)
    public static long transferTo(InputStream in, OutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        long transferred = 0L;
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            transferred += read;
        }
        return transferred;
    }

}
