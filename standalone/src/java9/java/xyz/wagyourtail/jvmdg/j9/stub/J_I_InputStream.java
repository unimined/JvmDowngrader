package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

public class J_I_InputStream {

    @Stub(javaVersion = Opcodes.V9, subtypes = true)
    public static byte[] readAllBytes(InputStream in) throws IOException {
        byte[] b = new byte[Integer.MAX_VALUE];
        int n = 0;
        while (n < Integer.MAX_VALUE) {
            int count = in.read(b, n, Integer.MAX_VALUE - n);
            if (count < 0) {
                if (n == 0) {
                    return null;
                } else {
                    return Arrays.copyOf(b, n);
                }
            }
            n += count;
        }
        return b;
    }

    @Stub(javaVersion = Opcodes.V9, subtypes = true)
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

    @Stub(javaVersion = Opcodes.V9, subtypes = true)
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
