package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.Objects;

public class J_I_Reader {

    @Stub(value = JavaVersion.VERSION_11, desc = "Ljava/io/Reader;", include = NullReader.class)
    public static Reader nullReader() {
        return new NullReader();
    }

    public static class NullReader extends Reader {
        private volatile boolean closed;

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }

        @Override
        public int read() throws IOException {
            ensureOpen();
            return -1;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, cbuf.length);
            ensureOpen();
            if (len == 0) {
                return 0;
            }
            return -1;
        }

        @Override
        public int read(CharBuffer target) throws IOException {
            Objects.requireNonNull(target);
            ensureOpen();
            if (target.hasRemaining()) {
                return -1;
            }
            return 0;
        }

        @Override
        public boolean ready() throws IOException {
            ensureOpen();
            return false;
        }

        @Override
        public long skip(long n) throws IOException {
            ensureOpen();
            return 0L;
        }

        @Override
        public long transferTo(Writer out) throws IOException {
            Objects.requireNonNull(out);
            ensureOpen();
            return 0L;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
