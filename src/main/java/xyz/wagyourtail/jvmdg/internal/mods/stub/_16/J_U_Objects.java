package xyz.wagyourtail.jvmdg.internal.mods.stub._16;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_U_Objects {

    @Stub(value = JavaVersion.VERSION_16, desc = "Ljava/util/Objects;checkIndex(JJ)J")
    public static long checkIndex(long index, long length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index out of range: " + index);
        }
        return index;
    }

    @Stub(value = JavaVersion.VERSION_16, desc = "Ljava/util/Objects;checkFromToIndex(JJJ)J")
    public static long checkFromToIndex(long fromIndex, long toIndex, long length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    @Stub(value = JavaVersion.VERSION_16, desc = "Ljava/util/Objects;checkFromIndexSize(JJJ)J")
    public static long checkFromIndexSize(long fromIndex, long size, long length) {
        if (fromIndex < 0 || size < 0 || fromIndex > length - size) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + (fromIndex + size) + ") out of bounds for length " + length);
        }
        return fromIndex;
    }
}
