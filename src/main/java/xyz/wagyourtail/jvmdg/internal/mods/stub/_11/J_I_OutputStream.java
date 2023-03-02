package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class J_I_OutputStream {

    @Stub(value = JavaVersion.VERSION_11, desc = "Ljava/io/OutputStream;", include = NullOutputStream.class)
    public static OutputStream nullOutputStream() {
        return new NullOutputStream();
    }

    public static class NullOutputStream extends OutputStream {
        private volatile boolean closed = false;

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }

        @Override
        public void write(int b) throws IOException {
            ensureOpen();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, b.length);
            ensureOpen();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
