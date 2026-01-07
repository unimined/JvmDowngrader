package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.exc.PartialStubError;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.*;

public class J_N_Buffer {

    @Stub
    public static Buffer slice(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            return ((ByteBuffer) buffer).slice();
        }
        if (buffer instanceof ShortBuffer) {
            return ((ShortBuffer) buffer).slice();
        }
        if (buffer instanceof CharBuffer) {
            return ((CharBuffer) buffer).slice();
        }
        if (buffer instanceof IntBuffer) {
            return ((IntBuffer) buffer).slice();
        }
        if (buffer instanceof FloatBuffer) {
            return ((FloatBuffer) buffer).slice();
        }
        if (buffer instanceof DoubleBuffer) {
            return ((DoubleBuffer) buffer).slice();
        }
        if (buffer instanceof LongBuffer) {
            return ((LongBuffer) buffer).slice();
        }
        throw PartialStubError.create();
    }

    @Stub
    public static Buffer duplicate(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            return ((ByteBuffer) buffer).duplicate();
        }
        if (buffer instanceof ShortBuffer) {
            return ((ShortBuffer) buffer).duplicate();
        }
        if (buffer instanceof CharBuffer) {
            return ((CharBuffer) buffer).duplicate();
        }
        if (buffer instanceof IntBuffer) {
            return ((IntBuffer) buffer).duplicate();
        }
        if (buffer instanceof FloatBuffer) {
            return ((FloatBuffer) buffer).duplicate();
        }
        if (buffer instanceof DoubleBuffer) {
            return ((DoubleBuffer) buffer).duplicate();
        }
        if (buffer instanceof LongBuffer) {
            return ((LongBuffer) buffer).duplicate();
        }
        throw PartialStubError.create();
    }

    @Stub
    public static Buffer position(Buffer buffer, int newPosition) {
        return buffer.position(newPosition);
    }

    @Stub
    public static Buffer limit(Buffer buffer, int newLimit) {
        return buffer.limit(newLimit);
    }

    @Stub
    public static Buffer mark(Buffer buffer) {
        return buffer.mark();
    }

    @Stub
    public static Buffer reset(Buffer buffer) {
        return buffer.reset();
    }

    @Stub
    public static Buffer clear(Buffer buffer) {
        return buffer.clear();
    }

    @Stub
    public static Buffer flip(Buffer buffer) {
        return buffer.flip();
    }

    @Stub
    public static Buffer rewind(Buffer buffer) {
        return buffer.rewind();
    }

}
