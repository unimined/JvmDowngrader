package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class J_I_InputStream {

    @Stub
    public static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    @Stub
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

    @Stub
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
