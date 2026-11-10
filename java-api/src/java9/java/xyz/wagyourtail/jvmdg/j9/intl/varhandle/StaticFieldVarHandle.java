package xyz.wagyourtail.jvmdg.j9.intl.varhandle;

import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_I_VarHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Field;

/**
 * VarHandle implementation for {@code static} fields: coordinates are {@code ()}.
 * Access goes through the {@code staticFieldBase}/{@code staticFieldOffset} pair
 * captured at construction.
 */
public class StaticFieldVarHandle extends J_L_I_VarHandle {
    private final Field field;
    private final Object staticFieldBase;
    private final long fieldOffset;

    public StaticFieldVarHandle(Field field) {
        super(field.getDeclaringClass(), field.getType());
        this.field = field;
        this.staticFieldBase = unsafe.staticFieldBase(field);
        this.fieldOffset = unsafe.staticFieldOffset(field);
    }

    @Override
    public Field jvmdg$field() {
        return field;
    }

    @Override
    public boolean jvmdg$isStatic() {
        return true;
    }

    @Override
    protected Class<?>[] coordinateClasses() {
        return new Class<?>[0];
    }

    private void checkStatic(Class<?> type, boolean read, boolean write) {
        if ((read && !type.isAssignableFrom(this.type)) || (write && (type == null ? this.type.isPrimitive() : !this.type.isAssignableFrom(type)))) {
            throw new WrongMethodTypeException();
        }
    }

    public final Object get() {
        if (Object.class.isAssignableFrom(type)) {
            checkStatic(Object.class, true, false);
            return unsafe.getObject(staticFieldBase, fieldOffset);
        } else {
            if (type == boolean.class) {
                checkStatic(boolean.class, true, false);
                return unsafe.getBoolean(staticFieldBase, fieldOffset);
            } else if (type == byte.class) {
                checkStatic(byte.class, true, false);
                return unsafe.getByte(staticFieldBase, fieldOffset);
            } else if (type == char.class) {
                checkStatic(char.class, true, false);
                char c = unsafe.getChar(staticFieldBase, fieldOffset);

                return c;

            } else if (type == short.class) {
                checkStatic(short.class, true, false);
                return unsafe.getShort(staticFieldBase, fieldOffset);
            } else if (type == int.class) {
                checkStatic(int.class, true, false);
                return unsafe.getInt(staticFieldBase, fieldOffset);
            } else if (type == long.class) {
                checkStatic(long.class, true, false);
                return unsafe.getLong(staticFieldBase, fieldOffset);
            } else if (type == float.class) {
                checkStatic(float.class, true, false);
                return unsafe.getFloat(staticFieldBase, fieldOffset);
            } else if (type == double.class) {
                checkStatic(double.class, true, false);
                return unsafe.getDouble(staticFieldBase, fieldOffset);
            }
            throw new WrongMethodTypeException();
        }
    }

    public final boolean getZ() {
        checkStatic(boolean.class, true, false);
        return unsafe.getBoolean(staticFieldBase, fieldOffset);
    }

    public final byte getB() {
        checkStatic(byte.class, true, false);
        return unsafe.getByte(staticFieldBase, fieldOffset);
    }

    public final char getC() {
        checkStatic(char.class, true, false);
        return unsafe.getChar(staticFieldBase, fieldOffset);
    }

    public final short getS() {
        checkStatic(short.class, true, false);
        return unsafe.getShort(staticFieldBase, fieldOffset);
    }

    public final int getI() {
        checkStatic(int.class, true, false);
        return unsafe.getInt(staticFieldBase, fieldOffset);
    }

    public final long getJ() {
        checkStatic(long.class, true, false);
        return unsafe.getLong(staticFieldBase, fieldOffset);
    }

    public final float getF() {
        checkStatic(float.class, true, false);
        return unsafe.getFloat(staticFieldBase, fieldOffset);
    }

    public final double getD() {
        checkStatic(double.class, true, false);
        return unsafe.getDouble(staticFieldBase, fieldOffset);
    }

    public final void set(Object value) {
        checkStatic(value == null ? null : value.getClass(), false, true);
        unsafe.putObject(staticFieldBase, fieldOffset, value);
    }

    public final void set(boolean value) {
        checkStatic(boolean.class, false, true);
        unsafe.putBoolean(staticFieldBase, fieldOffset, value);
    }

    public final void set(byte value) {
        checkStatic(byte.class, false, true);
        unsafe.putByte(staticFieldBase, fieldOffset, value);
    }

    public final void set(char value) {
        checkStatic(char.class, false, true);
        unsafe.putChar(staticFieldBase, fieldOffset, value);
    }

    public final void set(short value) {
        checkStatic(short.class, false, true);
        unsafe.putShort(staticFieldBase, fieldOffset, value);
    }

    public final void set(int value) {
        checkStatic(int.class, false, true);
        unsafe.putInt(staticFieldBase, fieldOffset, value);
    }

    public final void set(long value) {
        checkStatic(long.class, false, true);
        unsafe.putLong(staticFieldBase, fieldOffset, value);
    }

    public final void set(float value) {
        checkStatic(float.class, false, true);
        unsafe.putFloat(staticFieldBase, fieldOffset, value);
    }

    public final void set(double value) {
        checkStatic(double.class, false, true);
        unsafe.putDouble(staticFieldBase, fieldOffset, value);
    }

    public final Object getVolatile() {
        if (Object.class.isAssignableFrom(type)) {
            checkStatic(Object.class, true, false);
            return unsafe.getObjectVolatile(staticFieldBase, fieldOffset);
        } else {
            if (type == boolean.class) {
                checkStatic(boolean.class, true, false);
                return unsafe.getBooleanVolatile(staticFieldBase, fieldOffset);
            } else if (type == byte.class) {
                checkStatic(byte.class, true, false);
                return unsafe.getByteVolatile(staticFieldBase, fieldOffset);
            } else if (type == char.class) {
                checkStatic(char.class, true, false);
                return unsafe.getCharVolatile(staticFieldBase, fieldOffset);
            } else if (type == short.class) {
                checkStatic(short.class, true, false);
                return unsafe.getShortVolatile(staticFieldBase, fieldOffset);
            } else if (type == int.class) {
                checkStatic(int.class, true, false);
                return unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            } else if (type == long.class) {
                checkStatic(long.class, true, false);
                return unsafe.getLongVolatile(staticFieldBase, fieldOffset);
            } else if (type == float.class) {
                checkStatic(float.class, true, false);
                return unsafe.getFloatVolatile(staticFieldBase, fieldOffset);
            } else if (type == double.class) {
                checkStatic(double.class, true, false);
                return unsafe.getDoubleVolatile(staticFieldBase, fieldOffset);
            }
            throw new WrongMethodTypeException();
        }
    }

    public final boolean getVolatileZ() {
        checkStatic(boolean.class, true, false);
        return unsafe.getBooleanVolatile(staticFieldBase, fieldOffset);
    }

    public final byte getVolatileB() {
        checkStatic(byte.class, true, false);
        return unsafe.getByteVolatile(staticFieldBase, fieldOffset);
    }

    public final char getVolatileC() {
        checkStatic(char.class, true, false);
        return unsafe.getCharVolatile(staticFieldBase, fieldOffset);
    }

    public final short getVolatileS() {
        checkStatic(short.class, true, false);
        return unsafe.getShortVolatile(staticFieldBase, fieldOffset);
    }

    public final int getVolatileI() {
        checkStatic(int.class, true, false);
        return unsafe.getIntVolatile(staticFieldBase, fieldOffset);
    }

    public final long getVolatileJ() {
        checkStatic(long.class, true, false);
        return unsafe.getLongVolatile(staticFieldBase, fieldOffset);
    }

    public final float getVolatileF() {
        checkStatic(float.class, true, false);
        return unsafe.getFloatVolatile(staticFieldBase, fieldOffset);
    }

    public final double getVolatileD() {
        checkStatic(double.class, true, false);
        return unsafe.getDoubleVolatile(staticFieldBase, fieldOffset);
    }

    public final void setVolatile(Object value) {
        checkStatic(value == null ? null : value.getClass(), false, true);
        unsafe.putObjectVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(boolean value) {
        checkStatic(boolean.class, false, true);
        unsafe.putBooleanVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(byte value) {
        checkStatic(byte.class, false, true);
        unsafe.putByteVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(char value) {
        checkStatic(char.class, false, true);
        unsafe.putCharVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(short value) {
        checkStatic(short.class, false, true);
        unsafe.putShortVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(int value) {
        checkStatic(int.class, false, true);
        unsafe.putIntVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(long value) {
        checkStatic(long.class, false, true);
        unsafe.putLongVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(float value) {
        checkStatic(float.class, false, true);
        unsafe.putFloatVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(double value) {
        checkStatic(double.class, false, true);
        unsafe.putDoubleVolatile(staticFieldBase, fieldOffset, value);
    }

    public final boolean compareAndSet(Object expected, Object newValue) {
        checkStatic(Object.class, false, true);
        return unsafe.compareAndSwapObject(staticFieldBase, fieldOffset, expected, newValue);
    }

    public final boolean compareAndSet(boolean expected, boolean newValue) {
        checkStatic(boolean.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if (((byte) i != 0) != expected) return false;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, i, newValue ? 1 : 0));
        return true;
    }

    public final boolean compareAndSet(byte expected, byte newValue) {
        checkStatic(byte.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((byte) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, i, newValue));
        return true;
    }

    public final boolean compareAndSet(char expected, char newValue) {
        checkStatic(char.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((char) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, i, newValue));
        return true;
    }

    public final boolean compareAndSet(short expected, short newValue) {
        checkStatic(short.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((short) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, i, newValue));
        return true;
    }

    public final boolean compareAndSet(int expected, int newValue) {
        checkStatic(int.class, false, true);
        return unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, expected, newValue);
    }

    public final boolean compareAndSet(long expected, long newValue) {
        checkStatic(long.class, false, true);
        return unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, expected, newValue);
    }

    public final boolean compareAndSet(float expected, float newValue) {
        checkStatic(float.class, false, true);
        return unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, Float.floatToRawIntBits(expected), Float.floatToRawIntBits(newValue));
    }

    public final boolean compareAndSet(double expected, double newValue) {
        checkStatic(double.class, false, true);
        return unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(newValue));
    }

    public final Object compareAndExchange(Object expected, Object newValue) {
        checkStatic(Object.class, true, true);
        Object v;
        do {
            v = unsafe.getObjectVolatile(staticFieldBase, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapObject(staticFieldBase, fieldOffset, expected, newValue));
        return expected;
    }

    public final boolean compareAndExchange(boolean expected, boolean newValue) {
        checkStatic(boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if (((byte) v != 0) != expected) return (byte) v != 0;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, newValue ? 1 : 0));
        return expected;
    }

    public final byte compareAndExchange(byte expected, byte newValue) {
        checkStatic(byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((byte) v != expected) return (byte) v;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, newValue));
        return expected;
    }

    public final char compareAndExchange(char expected, char newValue) {
        checkStatic(char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((char) v != expected) return (char) v;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, newValue));
        return expected;
    }

    public final short compareAndExchange(short expected, short newValue) {
        checkStatic(short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((short) v != expected) return (short) v;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, newValue));
        return expected;
    }

    public final int compareAndExchange(int expected, int newValue) {
        checkStatic(int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, newValue));
        return expected;
    }

    public final long compareAndExchange(long expected, long newValue) {
        checkStatic(long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, newValue));
        return expected;
    }

    public final float compareAndExchange(float expected, float newValue) {
        checkStatic(float.class, true, true);
        float v;
        do {
            v = unsafe.getFloatVolatile(staticFieldBase, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, Float.floatToRawIntBits(v), Float.floatToRawIntBits(newValue)));
        return expected;
    }

    public final double compareAndExchange(double expected, double newValue) {
        checkStatic(double.class, true, true);
        double v;
        do {
            v = unsafe.getDoubleVolatile(staticFieldBase, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, Double.doubleToRawLongBits(v), Double.doubleToRawLongBits(newValue)));
        return expected;
    }

    public final Object getAndSet(Object newValue) {
        checkStatic(newValue == null ? null : newValue.getClass(), false, true);
        return unsafe.getAndSetObject(staticFieldBase, fieldOffset, newValue);
    }

    public final boolean getAndSet(boolean newValue) {
        checkStatic(boolean.class, true, true);
        return (byte) unsafe.getAndSetInt(staticFieldBase, fieldOffset, newValue ? 1 : 0) != 0;
    }

    public final byte getAndSet(byte newValue) {
        checkStatic(byte.class, true, true);
        return (byte) unsafe.getAndSetInt(staticFieldBase, fieldOffset, newValue);
    }

    public final char getAndSet(char newValue) {
        checkStatic(char.class, true, true);
        return (char) unsafe.getAndSetInt(staticFieldBase, fieldOffset, newValue);
    }

    public final short getAndSet(short newValue) {
        checkStatic(short.class, true, true);
        return (short) unsafe.getAndSetInt(staticFieldBase, fieldOffset, newValue);
    }

    public final int getAndSet(int newValue) {
        checkStatic(int.class, true, true);
        return unsafe.getAndSetInt(staticFieldBase, fieldOffset, newValue);
    }

    public final long getAndSet(long newValue) {
        checkStatic(long.class, true, true);
        return unsafe.getAndSetLong(staticFieldBase, fieldOffset, newValue);
    }

    public final float getAndSet(float newValue) {
        checkStatic(float.class, true, true);
        return Float.intBitsToFloat(unsafe.getAndSetInt(staticFieldBase, fieldOffset, Float.floatToRawIntBits(newValue)));
    }

    public final double getAndSet(double newValue) {
        checkStatic(double.class, true, true);
        return Double.longBitsToDouble(unsafe.getAndSetLong(staticFieldBase, fieldOffset, Double.doubleToRawLongBits(newValue)));
    }

    public final byte getAndAdd(byte value) {
        checkStatic(byte.class, true, true);
        byte v;
        do {
            v = unsafe.getByte(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v + value));
        return v;
    }

    public final char getAndAdd(char value) {
        checkStatic(char.class, true, true);
        char v;
        do {
            v = unsafe.getChar(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, value));
        return v;
    }

    public final short getAndAdd(short value) {
        checkStatic(short.class, true, true);
        short v;
        do {
            v = unsafe.getShort(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, value));
        return v;
    }

    public final int getAndAdd(int value) {
        checkStatic(int.class, true, true);

        return unsafe.getAndAddInt(staticFieldBase, fieldOffset, value);
    }

    public final long getAndAdd(long value) {
        checkStatic(long.class, true, true);

        return unsafe.getAndAddLong(staticFieldBase, fieldOffset, value);
    }

    public final float getAndAdd(float value) {
        checkStatic(float.class, true, true);
        float v;
        do {
            v = unsafe.getFloat(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, Float.floatToRawIntBits(v), Float.floatToRawIntBits(v + value)));
        return v;
    }

    public final double getAndAdd(double value) {
        checkStatic(double.class, true, true);
        double v;
        do {
            v = unsafe.getDouble(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, Double.doubleToRawLongBits(v), Double.doubleToRawLongBits(v + value)));
        return v;
    }

    public final boolean getAndBitwiseOr(boolean value) {
        checkStatic(boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, value || (byte) v != 0 ? 1 : 0));
        return (byte) v != 0;
    }

    public final byte getAndBitwiseOr(byte value) {
        checkStatic(byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v | value));
        return (byte) v;
    }

    public final char getAndBitwiseOr(char value) {
        checkStatic(char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, (char) ((char) v | value)));
        return (char) v;
    }

    public final short getAndBitwiseOr(short value) {
        checkStatic(short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, (short) ((short) v | value)));
        return (short) v;
    }

    public final int getAndBitwiseOr(int value) {
        checkStatic(int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v | value));
        return v;
    }

    public final long getAndBitwiseOr(long value) {
        checkStatic(long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, v | value));
        return v;
    }

    public final boolean getAndBitwiseAnd(boolean value) {
        checkStatic(boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, (byte) v != 0 && value ? 1 : 0));
        return (byte) v != 0;
    }

    public final byte getAndBitwiseAnd(byte value) {
        checkStatic(byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v & value));
        return (byte) v;
    }

    public final char getAndBitwiseAnd(char value) {
        checkStatic(char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, (char) ((char) v & value)));
        return (char) v;
    }

    public final short getAndBitwiseAnd(short value) {
        checkStatic(short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, (short) ((short) v & value)));
        return (short) v;
    }

    public final int getAndBitwiseAnd(int value) {
        checkStatic(int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v & value));
        return v;
    }

    public final long getAndBitwiseAnd(long value) {
        checkStatic(long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, v & value));
        return v;
    }

    public final boolean getAndBitwiseXor(boolean value) {
        checkStatic(boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, ((byte) v != 0) ^ value ? 1 : 0));
        return (byte) v != 0;
    }

    public final byte getAndBitwiseXor(byte value) {
        checkStatic(byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v ^ value));
        return (byte) v;
    }

    public final char getAndBitwiseXor(char value) {
        checkStatic(char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, (char) ((char) v ^ value)));
        return (char) v;
    }

    public final short getAndBitwiseXor(short value) {
        checkStatic(short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, (short) ((short) v ^ value)));
        return (short) v;
    }

    public final int getAndBitwiseXor(int value) {
        checkStatic(int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v ^ value));
        return v;
    }

    public final long getAndBitwiseXor(long value) {
        checkStatic(long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, v ^ value));
        return v;
    }
}
