package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

public class J_I_InputStream {

    @Stub(value = JavaVersion.VERSION_11, desc = "Ljava/io/InputStream;", include = NullInputStream.class)
    public static InputStream nullInputStream() {
        return new NullInputStream();
    }

    @Stub(JavaVersion.VERSION_11)
    public static byte[] readNBytes(InputStream stream, int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }
        byte[] b = new byte[len];
        int n = 0;
        while (n < len) {
            int count = stream.read(b, n, len - n);
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

    public static class NullInputStream extends InputStream {
        private volatile boolean closed = false;

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }

        @Override
        public int available () throws IOException {
            ensureOpen();
            return 0;
        }

        @Override
        public int read() throws IOException {
            ensureOpen();
            return -1;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, b.length);
            if (len == 0) {
                return 0;
            }
            ensureOpen();
            return -1;
        }

        @Override
        public byte[] readAllBytes() throws IOException {
            ensureOpen();
            return new byte[0];
        }

        @Override
        public int readNBytes(byte[] b, int off, int len)
            throws IOException {
            Objects.checkFromIndexSize(off, len, b.length);
            ensureOpen();
            return 0;
        }

        @Override
        public byte[] readNBytes(int len) throws IOException {
            if (len < 0) {
                throw new IllegalArgumentException("len < 0");
            }
            ensureOpen();
            return new byte[0];
        }

        @Override
        public long skip(long n) throws IOException {
            ensureOpen();
            return 0L;
        }

        @Override
        public void skipNBytes(long n) throws IOException {
            ensureOpen();
            if (n > 0) {
                throw new EOFException();
            }
        }

        @Override
        public long transferTo(OutputStream out) throws IOException {
            Objects.requireNonNull(out);
            ensureOpen();
            return 0L;
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }
    }
}
