package xyz.wagyourtail.jvmdg.j16.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_U_Objects {

    @Stub(opcVers = Opcodes.V16, ref = @Ref(value = "Ljava/util/Objects", member = "checkIndex"))
    public static long checkIndex(long index, long length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index out of range: " + index);
        }
        return index;
    }

    @Stub(opcVers = Opcodes.V16, ref = @Ref(value = "Ljava/util/Objects", member = "checkFromToIndex"))
    public static long checkFromToIndex(long fromIndex, long toIndex, long length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException(
                "Range [" + fromIndex + ", " + toIndex + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    @Stub(opcVers = Opcodes.V16, ref = @Ref(value = "Ljava/util/Objects", member = "checkFromIndexSize"))
    public static long checkFromIndexSize(long fromIndex, long size, long length) {
        if (fromIndex < 0 || size < 0 || fromIndex > length - size) {
            throw new IndexOutOfBoundsException(
                "Range [" + fromIndex + ", " + (fromIndex + size) + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

}
