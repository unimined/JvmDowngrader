package xyz.wagyourtail.jvmdg.j11.impl.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class StreamIterator implements Iterator<ByteBuffer> {
    public static final int BUFSIZE = Integer.parseInt(System.getProperty("jdk.httpclient.bufsize", "16384"));

    final InputStream is;
    final Supplier<? extends ByteBuffer> bufSupplier;
    volatile ByteBuffer nextBuffer;
    volatile boolean need2Read = true;
    volatile boolean haveNext;
    private volatile boolean eof;

    public StreamIterator(InputStream is) {
        this(is, () -> ByteBuffer.allocate(BUFSIZE));
    }

    StreamIterator(InputStream is, Supplier<? extends ByteBuffer> bufSupplier) {
        this.is = is;
        this.bufSupplier = bufSupplier;
    }

    private int read() throws IOException {
        if (eof)
            return -1;
        nextBuffer = bufSupplier.get();
        nextBuffer.clear();
        byte[] buf = nextBuffer.array();
        int offset = nextBuffer.arrayOffset();
        int cap = nextBuffer.capacity();
        int n = is.read(buf, offset, cap);
        if (n == -1) {
            eof = true;
            return -1;
        }
        //flip
        nextBuffer.limit(n);
        nextBuffer.position(0);
        return n;
    }

    private void closeStream() {
        try {
            is.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public synchronized boolean hasNext() {
        if (need2Read) {
            try {
                haveNext = read() != -1;
                if (haveNext) {
                    need2Read = false;
                }
            } catch (IOException e) {
                haveNext = false;
                need2Read = false;
                throw new UncheckedIOException(e);
            } finally {
                if (!haveNext) {
                    closeStream();
                }
            }
        }
        return haveNext;
    }

    @Override
    public synchronized ByteBuffer next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        need2Read = true;
        return nextBuffer;
    }

}