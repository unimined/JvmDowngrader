package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;
import java.util.function.Supplier;

public class J_U_Objects {


    @Stub(ref = @Ref("Ljava/util/Objects;"))
    public static <T> T requireNonNullElse(T obj, T defaultObj) {
        return (obj != null) ? obj : Objects.requireNonNull(defaultObj, "defaultObj");
    }

    @Stub(ref = @Ref("Ljava/util/Objects;"))
    public static <T> T requireNonNullElseGet(T obj, Supplier<? extends T> defaultObjSupplier) {
        return (obj != null) ? obj : Objects.requireNonNull(Objects.requireNonNull(defaultObjSupplier, "supplier")
            .get(), "supplier.get()");
    }

    @Stub(ref = @Ref("Ljava/util/Objects;"))
    public static int checkIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
        return index;
    }

    @Stub(ref = @Ref("Ljava/util/Objects;"))
    public static int checkFromToIndex(int fromIndex, int toIndex, int length) {
        if (fromIndex < 0 || toIndex > length || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException(
                "Range [" + fromIndex + ", " + toIndex + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    @Stub(ref = @Ref("Ljava/util/Objects;"))
    public static int checkFromIndexSize(int fromIndex, int size, int length) {
        if (fromIndex < 0 || size < 0 || fromIndex > length - size) {
            throw new IndexOutOfBoundsException(
                "Range [" + fromIndex + ", " + (fromIndex + size) + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

}
