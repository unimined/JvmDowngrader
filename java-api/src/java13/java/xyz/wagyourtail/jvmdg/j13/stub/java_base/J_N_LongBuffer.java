package xyz.wagyourtail.jvmdg.j13.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Objects;

public class J_N_LongBuffer {

    @Stub
    public static LongBuffer get(LongBuffer self, int index, long[] dst) {
        return get(self, index, dst, 0, dst.length);
    }

    @Stub
    public static LongBuffer get(LongBuffer self, int index, long[] dst, int offset, int length) {
        Objects.checkFromIndexSize(index, length, self.limit());
        Objects.checkFromIndexSize(offset, length, dst.length);
        int end = offset + length;
        for (int i = offset, j = index; i < end; i++, j++)
            dst[i] = self.get(j);
        return self;
    }

    @Stub
    public static LongBuffer put(LongBuffer self, int index, long[] src) {
        return put(self, index, src, 0, src.length);
    }

    @Stub
    public static LongBuffer put(LongBuffer self, int index, long[] src, int offset, int length) {
        if (self.isReadOnly())
            throw new ReadOnlyBufferException();
        Objects.checkFromIndexSize(index, length, self.limit());
        Objects.checkFromIndexSize(offset, length, src.length);
        int end = offset + length;
        for (int i = offset, j = index; i < end; i++, j++)
            self.put(j, src[i]);
        return self;
    }

}
