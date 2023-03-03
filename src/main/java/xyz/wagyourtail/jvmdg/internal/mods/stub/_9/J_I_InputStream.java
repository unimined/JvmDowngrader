package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class J_I_InputStream {

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true)
    public static byte[] readAllBytes(InputStream in) throws IOException {
        return in.readNBytes(Integer.MAX_VALUE);
    }

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true)
    public static int readNBytes(InputStream in, byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);

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

    @Stub(value = JavaVersion.VERSION_1_9, subtypes = true)
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
