package xyz.wagyourtail.jvmdg.j9.intl.varhandle;

import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_I_VarHandle;

import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Array;
import java.util.Objects;

/**
 * VarHandle implementation for {@code MethodHandles.arrayElementVarHandle}:
 * coordinates are {@code (Object array, int index)}. Also the base class for the
 * byte-array/ByteBuffer view handles, which override the {@code checkCoord},
 * {@code arrayView}, {@code viewOffset} and {@code rev} hooks below.
 */
public class ArrayVarHandle extends J_L_I_VarHandle {
    long fieldOffset;
    final long arrayIndexScale;
    boolean reverseBytes;

    public ArrayVarHandle(Class<?> arrayClass) {
        super(arrayClass, arrayClass.getComponentType());
        this.fieldOffset = unsafe.arrayBaseOffset(arrayClass);
        this.arrayIndexScale = unsafe.arrayIndexScale(arrayClass);
    }

    @Override
    public boolean jvmdg$isArray() {
        return true;
    }

    @Override
    protected Class<?>[] coordinateClasses() {
        return new Class<?>[] { ownerClass, int.class };
    }

    void checkCoord(Object array, int index, Class<?> type, boolean read, boolean write) {
        Objects.requireNonNull(array);
        Class<?> arrayClass = array.getClass();
        if (this.ownerClass == arrayClass) {
            if ((read && !type.isAssignableFrom(this.type)) || (write && (type == null ? this.type.isPrimitive() : !this.type.isAssignableFrom(type)))) {
                throw new WrongMethodTypeException();
            }
        } else {
            if (!this.ownerClass.isAssignableFrom(arrayClass)) {
                throw new WrongMethodTypeException();
            }
            Class<?> arrayType = arrayClass.getComponentType();
            if (read && !type.isAssignableFrom(arrayType)) {
                throw new WrongMethodTypeException();
            }
            if (write) {
                if (type == null ? this.type.isPrimitive() : !this.type.isAssignableFrom(type)) {
                    throw new WrongMethodTypeException();
                }
                if (type != null && !arrayType.isAssignableFrom(type)) {
                    throw new ArrayStoreException(type.getName());
                }
            }
        }
        if (index < 0 || index >= Array.getLength(array)) {
            throw new IndexOutOfBoundsException();
        }
    }

    Object arrayView(Object owner) {
        return owner;
    }

    long viewOffset(Object owner, int index) {
        return fieldOffset + index * arrayIndexScale;
    }

    char rev(char v) {
        return v;
    }

    short rev(short v) {
        return v;
    }

    int rev(int v) {
        return v;
    }

    long rev(long v) {
        return v;
    }

    float rev(float v) {
        return v;
    }

    double rev(double v) {
        return v;
    }

    public final Object get(Object owner, int index) {

        if (Object.class.isAssignableFrom(type)) {
            checkCoord(owner, index, Object.class, true, false);
            return unsafe.getObject(arrayView(owner), viewOffset(owner, index));
        } else {
            if (type == boolean.class) {
                checkCoord(owner, index, boolean.class, true, false);
                return unsafe.getBoolean(arrayView(owner), viewOffset(owner, index));
            } else if (type == byte.class) {
                checkCoord(owner, index, byte.class, true, false);
                return unsafe.getByte(arrayView(owner), viewOffset(owner, index));
            } else if (type == char.class) {
                checkCoord(owner, index, char.class, true, false);
                return rev(unsafe.getChar(arrayView(owner), viewOffset(owner, index)));
            } else if (type == short.class) {
                checkCoord(owner, index, short.class, true, false);
                return rev(unsafe.getShort(arrayView(owner), viewOffset(owner, index)));
            } else if (type == int.class) {
                checkCoord(owner, index, int.class, true, false);
                return rev(unsafe.getInt(arrayView(owner), viewOffset(owner, index)));
            } else if (type == long.class) {
                checkCoord(owner, index, long.class, true, false);
                return rev(unsafe.getLong(arrayView(owner), viewOffset(owner, index)));
            } else if (type == float.class) {
                checkCoord(owner, index, float.class, true, false);
                return rev(unsafe.getFloat(arrayView(owner), viewOffset(owner, index)));
            } else if (type == double.class) {
                checkCoord(owner, index, double.class, true, false);
                return rev(unsafe.getDouble(arrayView(owner), viewOffset(owner, index)));
            }
            throw new WrongMethodTypeException();
        }
    }

    public final boolean getZ(Object owner, int index) {

        checkCoord(owner, index, boolean.class, true, false);
        return unsafe.getBoolean(arrayView(owner), viewOffset(owner, index));
    }

    public final byte getB(Object owner, int index) {

        checkCoord(owner, index, byte.class, true, false);
        return unsafe.getByte(arrayView(owner), viewOffset(owner, index));
    }

    public final char getC(Object owner, int index) {

        checkCoord(owner, index, char.class, true, false);
        return rev(unsafe.getChar(arrayView(owner), viewOffset(owner, index)));
    }

    public final short getS(Object owner, int index) {

        checkCoord(owner, index, short.class, true, false);
        return rev(unsafe.getShort(arrayView(owner), viewOffset(owner, index)));
    }

    public final int getI(Object owner, int index) {

        checkCoord(owner, index, int.class, true, false);
        return rev(unsafe.getInt(arrayView(owner), viewOffset(owner, index)));
    }

    public final long getJ(Object owner, int index) {

        checkCoord(owner, index, long.class, true, false);
        return rev(unsafe.getLong(arrayView(owner), viewOffset(owner, index)));
    }

    public final float getF(Object owner, int index) {

        checkCoord(owner, index, float.class, true, false);
        return rev(unsafe.getFloat(arrayView(owner), viewOffset(owner, index)));
    }

    public final double getD(Object owner, int index) {

        checkCoord(owner, index, double.class, true, false);
        return rev(unsafe.getDouble(arrayView(owner), viewOffset(owner, index)));
    }

    public final void set(Object owner, int index, Object value) {

        checkCoord(owner, index, value == null ? null : value.getClass(), false, true);
        unsafe.putObject(arrayView(owner), viewOffset(owner, index), value);
    }

    public final void set(Object owner, int index, boolean value) {

        checkCoord(owner, index, boolean.class, false, true);
        unsafe.putBoolean(arrayView(owner), viewOffset(owner, index), value);
    }

    public final void set(Object owner, int index, byte value) {

        checkCoord(owner, index, byte.class, false, true);
        unsafe.putByte(arrayView(owner), viewOffset(owner, index), value);
    }

    public final void set(Object owner, int index, char value) {

        checkCoord(owner, index, char.class, false, true);
        unsafe.putChar(arrayView(owner), viewOffset(owner, index), rev(value));
    }

    public final void set(Object owner, int index, short value) {

        checkCoord(owner, index, short.class, false, true);
        unsafe.putShort(arrayView(owner), viewOffset(owner, index), rev(value));
    }

    public final void set(Object owner, int index, int value) {

        checkCoord(owner, index, int.class, false, true);
        unsafe.putInt(arrayView(owner), viewOffset(owner, index), rev(value));
    }

    public final void set(Object owner, int index, long value) {

        checkCoord(owner, index, long.class, false, true);
        unsafe.putLong(arrayView(owner), viewOffset(owner, index), rev(value));
    }

    public final void set(Object owner, int index, float value) {

        checkCoord(owner, index, float.class, false, true);
        unsafe.putFloat(arrayView(owner), viewOffset(owner, index), rev(value));
    }

    public final void set(Object owner, int index, double value) {

        checkCoord(owner, index, double.class, false, true);
        unsafe.putDouble(arrayView(owner), viewOffset(owner, index), rev(value));
    }

    public final Object getVolatile(Object owner, int index) {

        if (Object.class.isAssignableFrom(type)) {
            checkCoord(owner, index, Object.class, true, false);
            return unsafe.getObjectVolatile(arrayView(owner), viewOffset(owner, index));
        } else {
            if (type == boolean.class) {
                checkCoord(owner, index, boolean.class, true, false);
                return unsafe.getBooleanVolatile(arrayView(owner), viewOffset(owner, index));
            } else if (type == byte.class) {
                checkCoord(owner, index, byte.class, true, false);
                return unsafe.getByteVolatile(arrayView(owner), viewOffset(owner, index));
            } else if (type == char.class) {
                checkCoord(owner, index, char.class, true, false);
                return rev(unsafe.getCharVolatile(arrayView(owner), viewOffset(owner, index)));
            } else if (type == short.class) {
                checkCoord(owner, index, short.class, true, false);
                return rev(unsafe.getShortVolatile(arrayView(owner), viewOffset(owner, index)));
            } else if (type == int.class) {
                checkCoord(owner, index, int.class, true, false);
                return rev(unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index)));
            } else if (type == long.class) {
                checkCoord(owner, index, long.class, true, false);
                return rev(unsafe.getLongVolatile(arrayView(owner), viewOffset(owner, index)));
            } else if (type == float.class) {
                checkCoord(owner, index, float.class, true, false);
                return rev(unsafe.getFloatVolatile(arrayView(owner), viewOffset(owner, index)));
            } else if (type == double.class) {
                checkCoord(owner, index, double.class, true, false);
                return rev(unsafe.getDoubleVolatile(arrayView(owner), viewOffset(owner, index)));
            }
            throw new WrongMethodTypeException();
        }
    }

    public final boolean getVolatileZ(Object owner, int index) {

        checkCoord(owner, index, boolean.class, true, false);
        return unsafe.getBooleanVolatile(arrayView(owner), viewOffset(owner, index));
    }

    public final byte getVolatileB(Object owner, int index) {

        checkCoord(owner, index, byte.class, true, false);
        return unsafe.getByteVolatile(arrayView(owner), viewOffset(owner, index));
    }

    public final char getVolatileC(Object owner, int index) {

        checkCoord(owner, index, char.class, true, false);
        return rev(unsafe.getCharVolatile(arrayView(owner), viewOffset(owner, index)));
    }

    public final short getVolatileS(Object owner, int index) {

        checkCoord(owner, index, short.class, true, false);
        return rev(unsafe.getShortVolatile(arrayView(owner), viewOffset(owner, index)));
    }

    public final int getVolatileI(Object owner, int index) {

        checkCoord(owner, index, int.class, true, false);
        return rev(unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index)));
    }

    public final long getVolatileJ(Object owner, int index) {

        checkCoord(owner, index, long.class, true, false);
        return rev(unsafe.getLongVolatile(arrayView(owner), viewOffset(owner, index)));
    }

    public final float getVolatileF(Object owner, int index) {

        checkCoord(owner, index, float.class, true, false);
        return rev(unsafe.getFloatVolatile(arrayView(owner), viewOffset(owner, index)));
    }

    public final double getVolatileD(Object owner, int index) {

        checkCoord(owner, index, double.class, true, false);
        return rev(unsafe.getDoubleVolatile(arrayView(owner), viewOffset(owner, index)));
    }

    public final void setVolatile(Object owner, int index, Object value) {

        checkCoord(owner, index, value == null ? null : value.getClass(), false, true);
        unsafe.putObjectVolatile(arrayView(owner), viewOffset(owner, index), value);
    }

    public final void setVolatile(Object owner, int index, boolean value) {

        checkCoord(owner, index, boolean.class, false, true);
        unsafe.putBooleanVolatile(arrayView(owner), viewOffset(owner, index), value);
    }

    public final void setVolatile(Object owner, int index, byte value) {

        checkCoord(owner, index, byte.class, false, true);
        unsafe.putByteVolatile(arrayView(owner), viewOffset(owner, index), value);
    }

    public final void setVolatile(Object owner, int index, char value) {

        checkCoord(owner, index, char.class, false, true);
        unsafe.putCharVolatile(arrayView(owner), viewOffset(owner, index), rev(value));
    }

    public final void setVolatile(Object owner, int index, short value) {

        checkCoord(owner, index, short.class, false, true);
        unsafe.putShortVolatile(arrayView(owner), viewOffset(owner, index), rev(value));
    }

    public final void setVolatile(Object owner, int index, int value) {

        checkCoord(owner, index, int.class, false, true);
        unsafe.putIntVolatile(arrayView(owner), viewOffset(owner, index), rev(value));
    }

    public final void setVolatile(Object owner, int index, long value) {

        checkCoord(owner, index, long.class, false, true);
        unsafe.putLongVolatile(arrayView(owner), viewOffset(owner, index), rev(value));
    }

    public final void setVolatile(Object owner, int index, float value) {

        checkCoord(owner, index, float.class, false, true);
        unsafe.putFloatVolatile(arrayView(owner), viewOffset(owner, index), rev(value));
    }

    public final void setVolatile(Object owner, int index, double value) {

        checkCoord(owner, index, double.class, false, true);
        unsafe.putDoubleVolatile(arrayView(owner), viewOffset(owner, index), rev(value));
    }

    public final boolean compareAndSet(Object owner, int index, Object expected, Object newValue) {

        checkCoord(owner, index, Object.class, false, true);
        return unsafe.compareAndSwapObject(arrayView(owner), viewOffset(owner, index), expected, newValue);
    }

    public final boolean compareAndSet(Object owner, int index, boolean expected, boolean newValue) {

        checkCoord(owner, index, boolean.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
            if (((byte) i != 0) != expected) return false;
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), i, newValue ? 1 : 0));
        return true;
    }

    public final boolean compareAndSet(Object owner, int index, byte expected, byte newValue) {

        checkCoord(owner, index, byte.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
            if ((byte) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), i, newValue));
        return true;
    }

    public final boolean compareAndSet(Object owner, int index, char expected, char newValue) {

        checkCoord(owner, index, char.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
            if (rev((char) i) != expected) return false;
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), i, rev(newValue)));
        return true;
    }

    public final boolean compareAndSet(Object owner, int index, short expected, short newValue) {

        checkCoord(owner, index, short.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
            if (rev((short) i) != expected) return false;
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), i, rev(newValue)));
        return true;
    }

    public final boolean compareAndSet(Object owner, int index, int expected, int newValue) {

        checkCoord(owner, index, int.class, false, true);
        return unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), rev(expected), rev(newValue));
    }

    public final boolean compareAndSet(Object owner, int index, long expected, long newValue) {

        checkCoord(owner, index, long.class, false, true);
        return unsafe.compareAndSwapLong(arrayView(owner), viewOffset(owner, index), rev(expected), rev(newValue));
    }

    public final boolean compareAndSet(Object owner, int index, float expected, float newValue) {

        checkCoord(owner, index, float.class, false, true);
        return unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), Float.floatToRawIntBits(rev(expected)), Float.floatToRawIntBits(rev(newValue)));
    }

    public final boolean compareAndSet(Object owner, int index, double expected, double newValue) {

        checkCoord(owner, index, double.class, false, true);
        return unsafe.compareAndSwapLong(arrayView(owner), viewOffset(owner, index), Double.doubleToRawLongBits(rev(expected)), Double.doubleToRawLongBits(rev(newValue)));
    }

    public final Object compareAndExchange(Object owner, int index, Object expected, Object newValue) {

        checkCoord(owner, index, Object.class, true, true);
        Object v;
        do {
            v = unsafe.getObjectVolatile(arrayView(owner), viewOffset(owner, index));
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapObject(arrayView(owner), viewOffset(owner, index), v, newValue));
        return expected;
    }

    public final boolean compareAndExchange(Object owner, int index, boolean expected, boolean newValue) {

        checkCoord(owner, index, boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
            if (((byte) v != 0) != expected) return (byte) v != 0;
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, newValue ? 1 : 0));
        return expected;
    }

    public final byte compareAndExchange(Object owner, int index, byte expected, byte newValue) {

        checkCoord(owner, index, byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
            if ((byte) v != expected) return (byte) v;
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, newValue));
        return expected;
    }

    public final char compareAndExchange(Object owner, int index, char expected, char newValue) {

        checkCoord(owner, index, char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
            if (rev((char) v) != expected) return rev((char) v);
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev(newValue)));
        return expected;
    }

    public final short compareAndExchange(Object owner, int index, short expected, short newValue) {

        checkCoord(owner, index, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
            if (rev((short) v) != expected) return rev((short) v);
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev(newValue)));
        return expected;
    }

    public final int compareAndExchange(Object owner, int index, int expected, int newValue) {

        checkCoord(owner, index, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
            if (rev(v) != expected) return rev(v);
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev(newValue)));
        return expected;
    }

    public final long compareAndExchange(Object owner, int index, long expected, long newValue) {

        checkCoord(owner, index, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(arrayView(owner), viewOffset(owner, index));
            if (rev(v) != expected) return rev(v);
        } while (!unsafe.compareAndSwapLong(arrayView(owner), viewOffset(owner, index), v, rev(newValue)));
        return expected;
    }

    public final float compareAndExchange(Object owner, int index, float expected, float newValue) {

        checkCoord(owner, index, float.class, true, true);
        float v;
        do {
            v = rev(unsafe.getFloatVolatile(arrayView(owner), viewOffset(owner, index)));
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), Float.floatToRawIntBits(rev(v)), Float.floatToRawIntBits(rev(newValue))));
        return expected;
    }

    public final double compareAndExchange(Object owner, int index, double expected, double newValue) {

        checkCoord(owner, index, double.class, true, true);
        double v;
        do {
            v = rev(unsafe.getDoubleVolatile(arrayView(owner), viewOffset(owner, index)));
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(arrayView(owner), viewOffset(owner, index), Double.doubleToRawLongBits(rev(v)), Double.doubleToRawLongBits(rev(newValue))));
        return expected;
    }

    public final Object getAndSet(Object owner, int index, Object newValue) {

        checkCoord(owner, index, newValue == null ? null : newValue.getClass(), false, true);
        return unsafe.getAndSetObject(arrayView(owner), viewOffset(owner, index), index);
    }

    public final boolean getAndSet(Object owner, int index, boolean newValue) {

        checkCoord(owner, index, boolean.class, true, true);
        return (byte) unsafe.getAndSetInt(arrayView(owner), viewOffset(owner, index), newValue ? 1 : 0) != 0;
    }

    public final byte getAndSet(Object owner, int index, byte newValue) {

        checkCoord(owner, index, byte.class, true, true);
        return (byte) unsafe.getAndSetInt(arrayView(owner), viewOffset(owner, index), newValue);
    }

    public final char getAndSet(Object owner, int index, char newValue) {

        checkCoord(owner, index, char.class, true, true);
        return rev((char) unsafe.getAndSetInt(arrayView(owner), viewOffset(owner, index), rev(newValue)));
    }

    public final short getAndSet(Object owner, int index, short newValue) {

        checkCoord(owner, index, short.class, true, true);
        return rev((short) unsafe.getAndSetInt(arrayView(owner), viewOffset(owner, index), rev(newValue)));
    }

    public final int getAndSet(Object owner, int index, int newValue) {

        checkCoord(owner, index, int.class, true, true);
        return rev(unsafe.getAndSetInt(arrayView(owner), viewOffset(owner, index), rev(newValue)));
    }

    public final long getAndSet(Object owner, int index, long newValue) {

        checkCoord(owner, index, long.class, true, true);
        return rev(unsafe.getAndSetLong(arrayView(owner), viewOffset(owner, index), rev(newValue)));
    }

    public final float getAndSet(Object owner, int index, float newValue) {

        checkCoord(owner, index, float.class, true, true);
        return Float.intBitsToFloat(rev(unsafe.getAndSetInt(arrayView(owner), viewOffset(owner, index), Float.floatToRawIntBits(rev(newValue)))));
    }

    public final double getAndSet(Object owner, int index, double newValue) {

        checkCoord(owner, index, double.class, true, true);
        return Double.longBitsToDouble(rev(unsafe.getAndSetLong(arrayView(owner), viewOffset(owner, index), Double.doubleToRawLongBits(rev(newValue)))));
    }

    public final byte getAndAdd(Object owner, int index, byte value) {

        checkCoord(owner, index, byte.class, true, true);
        byte v;
        do {
            v = unsafe.getByte(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, v + value));
        return v;
    }

    public final char getAndAdd(Object owner, int index, char value) {

        checkCoord(owner, index, char.class, true, true);
        char v;
        do {
            v = rev(unsafe.getChar(arrayView(owner), viewOffset(owner, index)));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), rev(v), rev((char) (v + value))));
        return v;
    }

    public final short getAndAdd(Object owner, int index, short value) {

        checkCoord(owner, index, short.class, true, true);
        short v;
        do {
            v = rev(unsafe.getShort(arrayView(owner), viewOffset(owner, index)));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), rev(v), rev((short) (v + value))));
        return v;
    }

    public final int getAndAdd(Object owner, int index, int value) {

        checkCoord(owner, index, int.class, true, true);
        if (reverseBytes) {
            int v;
            do {
                v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
            } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev(rev(v) + value)));
            return rev(v);
        }
        return unsafe.getAndAddInt(arrayView(owner), viewOffset(owner, index), value);
    }

    public final long getAndAdd(Object owner, int index, long value) {

        checkCoord(owner, index, long.class, true, true);
        if (reverseBytes) {
            long v;
            do {
                v = unsafe.getLongVolatile(arrayView(owner), viewOffset(owner, index));
            } while (!unsafe.compareAndSwapLong(arrayView(owner), viewOffset(owner, index), v, rev(rev(v) + value)));
            return rev(v);
        }
        return unsafe.getAndAddLong(arrayView(owner), viewOffset(owner, index), value);
    }

    public final float getAndAdd(Object owner, int index, float value) {

        checkCoord(owner, index, float.class, true, true);
        float v;
        do {
            v = rev(unsafe.getFloat(arrayView(owner), viewOffset(owner, index)));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), Float.floatToRawIntBits(rev(v)), Float.floatToRawIntBits(rev(v + value))));
        return v;
    }

    public final double getAndAdd(Object owner, int index, double value) {

        checkCoord(owner, index, double.class, true, true);
        double v;
        do {
            v = rev(unsafe.getDouble(arrayView(owner), viewOffset(owner, index)));
        } while (!unsafe.compareAndSwapLong(arrayView(owner), viewOffset(owner, index), Double.doubleToRawLongBits(rev(v)), Double.doubleToRawLongBits(rev(v + value))));
        return v;
    }

    public final boolean getAndBitwiseOr(Object owner, int index, boolean value) {

        checkCoord(owner, index, boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, (byte) v != 0 || value ? 1 : 0));
        return (byte) v != 0;
    }

    public final byte getAndBitwiseOr(Object owner, int index, byte value) {

        checkCoord(owner, index, byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, v | value));
        return (byte) v;
    }

    public final char getAndBitwiseOr(Object owner, int index, char value) {

        checkCoord(owner, index, char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev((char) (rev((char) v) | value))));
        return rev((char) v);
    }

    public final short getAndBitwiseOr(Object owner, int index, short value) {

        checkCoord(owner, index, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev((short) (rev((short) v) | value))));
        return rev((short) v);
    }

    public final int getAndBitwiseOr(Object owner, int index, int value) {

        checkCoord(owner, index, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev(rev(v) | value)));
        return rev(v);
    }

    public final long getAndBitwiseOr(Object owner, int index, long value) {

        checkCoord(owner, index, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapLong(arrayView(owner), viewOffset(owner, index), v, rev(rev(v) | value)));
        return rev(v);
    }

    public final boolean getAndBitwiseAnd(Object owner, int index, boolean value) {

        checkCoord(owner, index, boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, (byte) v != 0 && value ? 1 : 0));
        return (byte) v != 0;
    }

    public final byte getAndBitwiseAnd(Object owner, int index, byte value) {

        checkCoord(owner, index, byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, v & value));
        return (byte) v;
    }

    public final char getAndBitwiseAnd(Object owner, int index, char value) {

        checkCoord(owner, index, char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev((char) (rev((char) v) & value))));
        return rev((char) v);
    }

    public final short getAndBitwiseAnd(Object owner, int index, short value) {

        checkCoord(owner, index, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev((short) (rev((short) v) & value))));
        return rev((short) v);
    }

    public final int getAndBitwiseAnd(Object owner, int index, int value) {

        checkCoord(owner, index, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev(rev(v) & value)));
        return rev(v);
    }

    public final long getAndBitwiseAnd(Object owner, int index, long value) {

        checkCoord(owner, index, long.class, true, true);
        long v;
        do {
             v = unsafe.getLongVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapLong(arrayView(owner), viewOffset(owner, index), v, rev(rev(v) & value)));
        return rev(v);
    }

    public final boolean getAndBitwiseXor(Object owner, int index, boolean value) {

        checkCoord(owner, index, boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, ((byte) v != 0) ^ value ? 1 : 0));
        return (byte) v != 0;
    }

    public final byte getAndBitwiseXor(Object owner, int index, byte value) {

        checkCoord(owner, index, byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, v ^ value));
        return (byte) v;
    }

    public final char getAndBitwiseXor(Object owner, int index, char value) {

        checkCoord(owner, index, char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev((char) (rev((char) v) ^ value))));
        return rev((char) v);
    }

    public final short getAndBitwiseXor(Object owner, int index, short value) {

        checkCoord(owner, index, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev((short) (rev((short) v) ^ value))));
        return rev((short) v);
    }

    public final int getAndBitwiseXor(Object owner, int index, int value) {

        checkCoord(owner, index, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapInt(arrayView(owner), viewOffset(owner, index), v, rev(rev(v) ^ value)));
        return rev(v);
    }

    public final long getAndBitwiseXor(Object owner, int index, long value) {

        checkCoord(owner, index, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(arrayView(owner), viewOffset(owner, index));
        } while (!unsafe.compareAndSwapLong(arrayView(owner), viewOffset(owner, index), v, rev(rev(v) ^ value)));
        return rev(v);
    }
}
