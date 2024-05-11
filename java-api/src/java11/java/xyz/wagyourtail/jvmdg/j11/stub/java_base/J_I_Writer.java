package xyz.wagyourtail.jvmdg.j11.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

public class J_I_Writer {

    @Stub(ref = @Ref("Ljava/io/Writer;"))
    public static Writer nullWriter() {
        return new NullWriter();
    }

    public static class NullWriter extends Writer {
        private volatile boolean closed;

        @Override
        public void write(int c) throws IOException {
            ensureOpen();
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, cbuf.length);
            ensureOpen();
        }

        @Override
        public void write(String str) throws IOException {
            Objects.requireNonNull(str);
            ensureOpen();
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, str.length());
            ensureOpen();
        }

        @Override
        public Writer append(CharSequence csq) throws IOException {
            ensureOpen();
            return this;
        }

        @Override
        public Writer append(CharSequence csq, int start, int end) throws IOException {
            ensureOpen();
            if (csq != null) {
                Objects.checkFromToIndex(start, end, csq.length());
            }
            return this;
        }

        @Override
        public Writer append(char c) throws IOException {
            ensureOpen();
            return this;
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }

        @Override
        public void flush() throws IOException {
            ensureOpen();
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }

    }

}
