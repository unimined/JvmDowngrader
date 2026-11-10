package xyz.wagyourtail.jvmdg.j9.intl.varhandle;

import xyz.wagyourtail.jvmdg.j9.stub.java_base.J_L_I_VarHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * VarHandle implementation for instance fields: coordinates are {@code (Object owner)}.
 * Access goes through {@code fieldOffset} applied to the owner object.
 */
public class InstanceFieldVarHandle extends J_L_I_VarHandle {
    private final Field field;
    private final long fieldOffset;

    public InstanceFieldVarHandle(Field field) {
        super(field.getDeclaringClass(), field.getType());
        this.field = field;
        this.fieldOffset = unsafe.objectFieldOffset(field);
    }

    @Override
    public Field jvmdg$field() {
        return field;
    }

    @Override
    protected Class<?>[] coordinateClasses() {
        return new Class<?>[] { ownerClass };
    }

    private void checkOwner(Object owner, Class<?> type, boolean read, boolean write) {
        Objects.requireNonNull(owner);
        if (!this.ownerClass.isAssignableFrom(owner.getClass()) || (read && !type.isAssignableFrom(this.type)) || (write && (type == null ? this.type.isPrimitive() : !this.type.isAssignableFrom(type)))) {
            throw new WrongMethodTypeException();
        }
    }

    public final Object get(Object owner) {
        if (Object.class.isAssignableFrom(type)) {
            checkOwner(owner, Object.class, true, false);
            return unsafe.getObject(owner, fieldOffset);
        } else {
            if (type == boolean.class) {
                checkOwner(owner, boolean.class, true, false);
                return unsafe.getBoolean(owner, fieldOffset);
            } else if (type == byte.class) {
                checkOwner(owner, byte.class, true, false);
                return unsafe.getByte(owner, fieldOffset);
            } else if (type == char.class) {
                checkOwner(owner, char.class, true, false);
                return unsafe.getChar(owner, fieldOffset);
            } else if (type == short.class) {
                checkOwner(owner, short.class, true, false);
                return unsafe.getShort(owner, fieldOffset);
            } else if (type == int.class) {
                checkOwner(owner, int.class, true, false);
                return unsafe.getInt(owner, fieldOffset);
            } else if (type == long.class) {
                checkOwner(owner, long.class, true, false);
                return unsafe.getLong(owner, fieldOffset);
            } else if (type == float.class) {
                checkOwner(owner, float.class, true, false);
                return unsafe.getFloat(owner, fieldOffset);
            } else if (type == double.class) {
                checkOwner(owner, double.class, true, false);
                return unsafe.getDouble(owner, fieldOffset);
            }
            throw new WrongMethodTypeException();
        }
    }

    public final boolean getZ(Object owner) {
        checkOwner(owner, boolean.class, true, false);
        return unsafe.getBoolean(owner, fieldOffset);
    }

    public final byte getB(Object owner) {
        checkOwner(owner, byte.class, true, false);
        return unsafe.getByte(owner, fieldOffset);
    }

    public final char getC(Object owner) {
        checkOwner(owner, char.class, true, false);
        return unsafe.getChar(owner, fieldOffset);
    }

    public final short getS(Object owner) {
        checkOwner(owner, short.class, true, false);
        return unsafe.getShort(owner, fieldOffset);
    }

    public final int getI(Object owner) {
        checkOwner(owner, int.class, true, false);
        return unsafe.getInt(owner, fieldOffset);
    }

    public final long getJ(Object owner) {
        checkOwner(owner, long.class, true, false);
        return unsafe.getLong(owner, fieldOffset);
    }

    public final float getF(Object owner) {
        checkOwner(owner, float.class, true, false);
        return unsafe.getFloat(owner, fieldOffset);
    }

    public final double getD(Object owner) {
        checkOwner(owner, double.class, true, false);
        return unsafe.getDouble(owner, fieldOffset);
    }

    public final void set(Object owner, Object value) {
        checkOwner(owner, value == null ? null : value.getClass(), false, true);
        unsafe.putObject(owner, fieldOffset, value);
    }

    public final void set(Object owner, boolean value) {
        checkOwner(owner, boolean.class, false, true);
        unsafe.putBoolean(owner, fieldOffset, value);
    }

    public final void set(Object owner, byte value) {
        checkOwner(owner, byte.class, false, true);
        unsafe.putByte(owner, fieldOffset, value);
    }

    public final void set(Object owner, char value) {
        checkOwner(owner, char.class, false, true);
        unsafe.putChar(owner, fieldOffset, value);
    }

    public final void set(Object owner, short value) {
        checkOwner(owner, short.class, false, true);
        unsafe.putShort(owner, fieldOffset, value);
    }

    public final void set(Object owner, int value) {
        checkOwner(owner, int.class, false, true);
        unsafe.putInt(owner, fieldOffset, value);
    }

    public final void set(Object owner, long value) {
        checkOwner(owner, long.class, false, true);
        unsafe.putLong(owner, fieldOffset, value);
    }

    public final void set(Object owner, float value) {
        checkOwner(owner, float.class, false, true);
        unsafe.putFloat(owner, fieldOffset, value);
    }

    public final void set(Object owner, double value) {
        checkOwner(owner, double.class, false, true);
        unsafe.putDouble(owner, fieldOffset, value);
    }

    public final Object getVolatile(Object owner) {
        if (Object.class.isAssignableFrom(type)) {
            checkOwner(owner, Object.class, true, false);
            return unsafe.getObjectVolatile(owner, fieldOffset);
        } else {
            if (type == boolean.class) {
                checkOwner(owner, boolean.class, true, false);
                return unsafe.getBooleanVolatile(owner, fieldOffset);
            } else if (type == byte.class) {
                checkOwner(owner, byte.class, true, false);
                return unsafe.getByteVolatile(owner, fieldOffset);
            } else if (type == char.class) {
                checkOwner(owner, char.class, true, false);
                return unsafe.getCharVolatile(owner, fieldOffset);
            } else if (type == short.class) {
                checkOwner(owner, short.class, true, false);
                return unsafe.getShortVolatile(owner, fieldOffset);
            } else if (type == int.class) {
                checkOwner(owner, int.class, true, false);
                return unsafe.getIntVolatile(owner, fieldOffset);
            } else if (type == long.class) {
                checkOwner(owner, long.class, true, false);
                return unsafe.getLongVolatile(owner, fieldOffset);
            } else if (type == float.class) {
                checkOwner(owner, float.class, true, false);
                return unsafe.getFloatVolatile(owner, fieldOffset);
            } else if (type == double.class) {
                checkOwner(owner, double.class, true, false);
                return unsafe.getDoubleVolatile(owner, fieldOffset);
            }
            throw new WrongMethodTypeException();
        }
    }

    public final boolean getVolatileZ(Object owner) {
        checkOwner(owner, boolean.class, true, false);
        return unsafe.getBooleanVolatile(owner, fieldOffset);
    }

    public final byte getVolatileB(Object owner) {
        checkOwner(owner, byte.class, true, false);
        return unsafe.getByteVolatile(owner, fieldOffset);
    }

    public final char getVolatileC(Object owner) {
        checkOwner(owner, char.class, true, false);
        return unsafe.getCharVolatile(owner, fieldOffset);
    }

    public final short getVolatileS(Object owner) {
        checkOwner(owner, short.class, true, false);
        return unsafe.getShortVolatile(owner, fieldOffset);
    }

    public final int getVolatileI(Object owner) {
        checkOwner(owner, int.class, true, false);
        return unsafe.getIntVolatile(owner, fieldOffset);
    }

    public final long getVolatileJ(Object owner) {
        checkOwner(owner, long.class, true, false);
        return unsafe.getLongVolatile(owner, fieldOffset);
    }

    public final float getVolatileF(Object owner) {
        checkOwner(owner, float.class, true, false);
        return unsafe.getFloatVolatile(owner, fieldOffset);
    }

    public final double getVolatileD(Object owner) {
        checkOwner(owner, double.class, true, false);
        return unsafe.getDoubleVolatile(owner, fieldOffset);
    }

    public final void setVolatile(Object owner, Object value) {
        checkOwner(owner, value == null ? null : value.getClass(), false, true);
        unsafe.putObjectVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, boolean value) {
        checkOwner(owner, boolean.class, false, true);
        unsafe.putBooleanVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, byte value) {
        checkOwner(owner, byte.class, false, true);
        unsafe.putByteVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, char value) {
        checkOwner(owner, char.class, false, true);
        unsafe.putCharVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, short value) {
        checkOwner(owner, short.class, false, true);
        unsafe.putShortVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, int value) {
        checkOwner(owner, int.class, false, true);
        unsafe.putIntVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, long value) {
        checkOwner(owner, long.class, false, true);
        unsafe.putLongVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, float value) {
        checkOwner(owner, float.class, false, true);
        unsafe.putFloatVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, double value) {
        checkOwner(owner, double.class, false, true);
        unsafe.putDoubleVolatile(owner, fieldOffset, value);
    }

    public final boolean compareAndSet(Object owner, Object expected, Object newValue) {
        checkOwner(owner, Object.class, false, true);
        return unsafe.compareAndSwapObject(owner, fieldOffset, expected, newValue);
    }

    public final boolean compareAndSet(Object owner, boolean expected, boolean newValue) {
        checkOwner(owner, boolean.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset);
            if (((byte) i != 0) != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, i, newValue ? 1 : 0));
        return true;
    }

    public final boolean compareAndSet(Object owner, byte expected, byte newValue) {
        checkOwner(owner, byte.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset);
            if ((byte) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, i, newValue));
        return true;
    }

    public final boolean compareAndSet(Object owner, char expected, char newValue) {
        checkOwner(owner, char.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset);
            if ((char) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, i, newValue));
        return true;
    }

    public final boolean compareAndSet(Object owner, short expected, short newValue) {
        checkOwner(owner, short.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset);
            if ((short) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, i, newValue));
        return true;
    }

    public final boolean compareAndSet(Object owner, int expected, int newValue) {
        checkOwner(owner, int.class, false, true);
        return unsafe.compareAndSwapInt(owner, fieldOffset, expected, newValue);
    }

    public final boolean compareAndSet(Object owner, long expected, long newValue) {
        checkOwner(owner, long.class, false, true);
        return unsafe.compareAndSwapLong(owner, fieldOffset, expected, newValue);
    }

    public final boolean compareAndSet(Object owner, float expected, float newValue) {
        checkOwner(owner, float.class, false, true);
        return unsafe.compareAndSwapInt(owner, fieldOffset, Float.floatToRawIntBits(expected), Float.floatToRawIntBits(newValue));
    }

    public final boolean compareAndSet(Object owner, double expected, double newValue) {
        checkOwner(owner, double.class, false, true);
        return unsafe.compareAndSwapLong(owner, fieldOffset, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(newValue));
    }

    public final Object compareAndExchange(Object owner, Object expected, Object newValue) {
        checkOwner(owner, Object.class, true, true);
        Object v;
        do {
            v = unsafe.getObjectVolatile(owner, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapObject(owner, fieldOffset, v, newValue));
        return expected;
    }

    public final boolean compareAndExchange(Object owner, boolean expected, boolean newValue) {
        checkOwner(owner, boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
            if (((byte) v != 0) != expected) return (byte) v != 0;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, newValue ? 1 : 0));
        return expected;
    }

    public final byte compareAndExchange(Object owner, byte expected, byte newValue) {
        checkOwner(owner, byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
            if ((byte) v != expected) return (byte) v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, newValue));
        return expected;
    }

    public final char compareAndExchange(Object owner, char expected, char newValue) {
        checkOwner(owner, char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
            if ((char) v != expected) return (char) v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, newValue));
        return expected;
    }

    public final short compareAndExchange(Object owner, short expected, short newValue) {
        checkOwner(owner, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
            if ((short) v != expected) return (short) v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, newValue));
        return expected;
    }

    public final int compareAndExchange(Object owner, int expected, int newValue) {
        checkOwner(owner, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, newValue));
        return expected;
    }

    public final long compareAndExchange(Object owner, long expected, long newValue) {
        checkOwner(owner, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, newValue));
        return expected;
    }

    public final float compareAndExchange(Object owner, float expected, float newValue) {
        checkOwner(owner, float.class, true, true);
        float v;
        do {
            v = unsafe.getFloatVolatile(owner, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, Float.floatToRawIntBits(v), Float.floatToRawIntBits(newValue)));
        return expected;
    }

    public final double compareAndExchange(Object owner, double expected, double newValue) {
        checkOwner(owner, double.class, true, true);
        double v;
        do {
            v = unsafe.getDoubleVolatile(owner, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, Double.doubleToRawLongBits(v), Double.doubleToRawLongBits(newValue)));
        return expected;
    }

    public final Object getAndSet(Object owner, Object newValue) {
        checkOwner(owner, newValue == null ? null : newValue.getClass(), false, true);
        return unsafe.getAndSetObject(owner, fieldOffset, newValue);
    }

    public final boolean getAndSet(Object owner, boolean newValue) {
        checkOwner(owner, boolean.class, true, true);
        return (byte) unsafe.getAndSetInt(owner, fieldOffset, newValue ? 1 : 0) != 0;
    }

    public final byte getAndSet(Object owner, byte newValue) {
        checkOwner(owner, byte.class, true, true);
        return (byte) unsafe.getAndSetInt(owner, fieldOffset, newValue);
    }

    public final char getAndSet(Object owner, char newValue) {
        checkOwner(owner, char.class, true, true);
        return (char) unsafe.getAndSetInt(owner, fieldOffset, newValue);
    }

    public final short getAndSet(Object owner, short newValue) {
        checkOwner(owner, short.class, true, true);
        return (short) unsafe.getAndSetInt(owner, fieldOffset, newValue);
    }

    public final int getAndSet(Object owner, int newValue) {
        checkOwner(owner, int.class, true, true);
        return unsafe.getAndSetInt(owner, fieldOffset, newValue);
    }

    public final long getAndSet(Object owner, long newValue) {
        checkOwner(owner, long.class, true, true);
        return unsafe.getAndSetLong(owner, fieldOffset, newValue);
    }

    public final float getAndSet(Object owner, float newValue) {
        checkOwner(owner, float.class, true, true);
        return Float.intBitsToFloat(unsafe.getAndSetInt(owner, fieldOffset, Float.floatToRawIntBits(newValue)));
    }

    public final double getAndSet(Object owner, double newValue) {
        checkOwner(owner, double.class, true, true);
        return Double.longBitsToDouble(unsafe.getAndSetLong(owner, fieldOffset, Double.doubleToRawLongBits(newValue)));
    }

    public final byte getAndAdd(Object owner, byte value) {
        checkOwner(owner, byte.class, true, true);
        byte v;
        do {
            v = unsafe.getByte(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v + value));
        return v;
    }

    public final char getAndAdd(Object owner, char value) {
        checkOwner(owner, char.class, true, true);
        char v;
        do {
            v = unsafe.getChar(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, (char) (v + value)));
        return v;
    }

    public final short getAndAdd(Object owner, short value) {
        checkOwner(owner, short.class, true, true);
        short v;
        do {
            v = unsafe.getShort(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, (short) (v + value)));
        return v;
    }

    public final int getAndAdd(Object owner, int value) {
        checkOwner(owner, int.class, true, true);

        return unsafe.getAndAddInt(owner, fieldOffset, value);
    }

    public final long getAndAdd(Object owner, long value) {
        checkOwner(owner, long.class, true, true);

        return unsafe.getAndAddLong(owner, fieldOffset, value);
    }

    public final float getAndAdd(Object owner, float value) {
        checkOwner(owner, float.class, true, true);
        float v;
        do {
            v = unsafe.getFloat(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, Float.floatToRawIntBits(v), Float.floatToRawIntBits(v + value)));
        return v;
    }

    public final double getAndAdd(Object owner, double value) {
        checkOwner(owner, double.class, true, true);
        double v;
        do {
            v = unsafe.getDouble(owner, fieldOffset);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, Double.doubleToRawLongBits(v), Double.doubleToRawLongBits(v + value)));
        return v;
    }

    public final boolean getAndBitwiseOr(Object owner, boolean value) {
        checkOwner(owner, boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, value || (byte) v != 0 ? 1 : 0));
        return (byte) v != 0;
    }

    public final byte getAndBitwiseOr(Object owner, byte value) {
        checkOwner(owner, byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v | value));
        return (byte) v;
    }

    public final char getAndBitwiseOr(Object owner, char value) {
        checkOwner(owner, char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, (char) ((char) v | value)));
        return (char) v;
    }

    public final short getAndBitwiseOr(Object owner, short value) {
        checkOwner(owner, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, (short) ((short) v | value)));
        return (short) v;
    }

    public final int getAndBitwiseOr(Object owner, int value) {
        checkOwner(owner, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v | value));
        return v;
    }

    public final long getAndBitwiseOr(Object owner, long value) {
        checkOwner(owner, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, v | value));
        return v;
    }

    public final boolean getAndBitwiseAnd(Object owner, boolean value) {
        checkOwner(owner, boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, (byte) v != 0 && value ? 1 : 0));
        return (byte) v != 0;
    }

    public final byte getAndBitwiseAnd(Object owner, byte value) {
        checkOwner(owner, byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v & value));
        return (byte) v;
    }

    public final char getAndBitwiseAnd(Object owner, char value) {
        checkOwner(owner, char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, (char) ((char) v & value)));
        return (char) v;
    }

    public final short getAndBitwiseAnd(Object owner, short value) {
        checkOwner(owner, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, (short) ((short) v & value)));
        return (short) v;
    }

    public final int getAndBitwiseAnd(Object owner, int value) {
        checkOwner(owner, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v & value));
        return v;
    }

    public final long getAndBitwiseAnd(Object owner, long value) {
        checkOwner(owner, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, v & value));
        return v;
    }

    public final boolean getAndBitwiseXor(Object owner, boolean value) {
        checkOwner(owner, boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, ((byte) v != 0) ^ value ? 1 : 0));
        return (byte) v != 0;
    }

    public final byte getAndBitwiseXor(Object owner, byte value) {
        checkOwner(owner, byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v ^ value));
        return (byte) v;
    }

    public final char getAndBitwiseXor(Object owner, char value) {
        checkOwner(owner, char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, (char) ((char) v ^ value)));
        return (char) v;
    }

    public final short getAndBitwiseXor(Object owner, short value) {
        checkOwner(owner, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, (short) ((short) v ^ value)));
        return (short) v;
    }

    public final int getAndBitwiseXor(Object owner, int value) {
        checkOwner(owner, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v ^ value));
        return v;
    }

    public final long getAndBitwiseXor(Object owner, long value) {
        checkOwner(owner, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, v ^ value));
        return v;
    }
}
