package xyz.wagyourtail.jvmdg.j11.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class J_I_OutputStream {

    @Stub(opcVers = Opcodes.V11, ref = @Ref("Ljava/io/OutputStream;"), include = NullOutputStream.class)
    public static OutputStream nullOutputStream() {
        return new NullOutputStream();
    }

    public static class NullOutputStream extends OutputStream {
        private volatile boolean closed = false;

        @Override
        public void write(int b) throws IOException {
            ensureOpen();
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
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
