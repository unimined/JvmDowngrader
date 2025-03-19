package xyz.wagyourtail.jvmdg.j24.impl;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Objects;

public class CharSequenceReader extends Reader {
    private final int length;
    private CharSequence cs;
    private int next = 0;
    private int mark = 0;

    public CharSequenceReader(CharSequence cs) {
        this.length = cs.length();
        this.cs = cs;
    }

    private void ensureOpen() throws IOException {
        if (cs == null) {
            throw new IOException("Reader is closed");
        }
    }

    public int read() throws IOException {
        synchronized (lock) {
            ensureOpen();
            if (next >= length) {
                return -1;
            }
            return cs.charAt(next++);
        }
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        synchronized (lock) {
            ensureOpen();
            Objects.checkFromIndexSize(off, len, cbuf.length);
            if (len == 0) {
                return 0;
            }
            if (next >= length) {
                return -1;
            }
            int n = Math.min(len, length - next);
            switch (cs) {
                case String s -> s.getChars(next, next + n, cbuf, off);
                case StringBuffer sb -> sb.getChars(next, next + n, cbuf, off);
                case StringBuilder sb -> sb.getChars(next, next + n, cbuf, off);
                case CharBuffer cb -> cb.get(next, cbuf, off, n);
                default -> {
                    for (int i = 0; i < n; i++) {
                        cbuf[off + i] = cs.charAt(next + i);
                    }
                }
            }
            next += n;
            return n;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        synchronized (lock) {
            ensureOpen();
            if (next >= length) {
                return 0;
            }
            long toSkip = Math.min(n, length - next);
            toSkip = Math.max(-next, toSkip);
            next += (int) toSkip;
            return toSkip;
        }
    }

    @Override
    public boolean ready() throws IOException {
        synchronized (lock) {
            ensureOpen();
            return true;
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        if (readAheadLimit < 0) {
            throw new IllegalArgumentException("readAheadLimit < 0");
        }
        synchronized (lock) {
            ensureOpen();
            mark = next;
        }
    }

    @Override
    public void reset() throws IOException {
        synchronized (lock) {
            ensureOpen();
            next = mark;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            cs = null;
        }
    }

}
