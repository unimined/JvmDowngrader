package xyz.wagyourtail.jvmdg.j9.intl.varhandle;

import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_I_VarHandle;

import java.lang.invoke.WrongMethodTypeException;
import java.util.Objects;

/**
 * VarHandle implementation for {@code MethodHandles.byteArrayViewVarHandle}:
 * coordinates are {@code (byte[], int)} where the index is a byte offset into the
 * array. {@code reverseBytes} controls the endianness of multi-byte views.
 */
public class ByteArrayViewVarHandle extends ArrayVarHandle {

    public ByteArrayViewVarHandle(Class<?> viewClass, boolean reverseBytes) {
        super(viewClass);
        this.fieldOffset = unsafe.arrayBaseOffset(byte[].class);
        this.reverseBytes = reverseBytes;
    }

    @Override
    public boolean jvmdg$isByteArrayView() {
        return true;
    }

    @Override
    protected Class<?>[] coordinateClasses() {
        return new Class<?>[] { byte[].class, int.class };
    }

    @Override
    void checkCoord(Object array, int index, Class<?> type, boolean read, boolean write) {
        Objects.requireNonNull(array);
        if (type != this.type || array.getClass() != byte[].class) {
            throw new WrongMethodTypeException();
        }
        if (index < 0 || index > ((byte[]) array).length - arrayIndexScale) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    long viewOffset(Object owner, int index) {
        return fieldOffset + index;
    }

    @Override
    char rev(char v) {
        return reverseBytes ? Character.reverseBytes(v) : v;
    }

    @Override
    short rev(short v) {
        return reverseBytes ? Short.reverseBytes(v) : v;
    }

    @Override
    int rev(int v) {
        return reverseBytes ? Integer.reverseBytes(v) : v;
    }

    @Override
    long rev(long v) {
        return reverseBytes ? Long.reverseBytes(v) : v;
    }

    @Override
    float rev(float v) {
        return reverseBytes ? Float.intBitsToFloat(Integer.reverseBytes(Float.floatToRawIntBits(v))) : v;
    }

    @Override
    double rev(double v) {
        return reverseBytes ? Double.longBitsToDouble(Long.reverseBytes(Double.doubleToRawLongBits(v))) : v;
    }
}
