package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Adapter("java/lang/invoke/VarHandle")
public class J_L_I_VarHandle {
    private static final Unsafe unsafe = Utils.getUnsafe();

    @Modify(ref = @Ref("xyz/wagyourtail/jvmdg/j9/stub/java_base/J_L_I_VarHandle"))
    public static void modifyPolymorphics(MethodNode mnode, int i) {
        MethodInsnNode min = (MethodInsnNode) mnode.instructions.get(i);
        if (!AccessMode.byName.containsKey(min.name)) return;
        Type ret = Type.getReturnType(min.desc);
        min.name = fixMethodName(min.name, ret.getDescriptor());
        Type[] args = Type.getArgumentTypes(min.desc);
        for (int j = 0; j < args.length; j++) {
            if (args[j].getSort() >= 9) args[j] = Type.getObjectType("java/lang/Object");
        }
        if (ret.getSort() >= 9) {
            mnode.instructions.insert(min, new TypeInsnNode(Opcodes.CHECKCAST, ret.getInternalName()));
            ret = Type.getObjectType("java/lang/Object");
        }
        min.desc = Type.getMethodDescriptor(ret, args);
    }

    private static String fixMethodName(String name, String desc) {
        if (name.startsWith("get") && !name.startsWith("getAnd")) {
            if (desc.length() == 1) {
                return name + desc;
            }
        }
        return name;
    }


    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private final Object staticFieldBase;
    private final long fieldOffset;
    private final Field field;
    private final Class<?> ownerClass;
    private final Class<?> type;
    private final boolean isStatic;
    private final boolean isArray;
    private final long arrayIndexScale;

    public J_L_I_VarHandle(Field field) {
        this.field = field;
        this.ownerClass = field.getDeclaringClass();
        this.type = field.getType();
        this.isStatic = Modifier.isStatic(field.getModifiers());
        this.isArray = false;
        this.arrayIndexScale = 0;
        if (this.isStatic) {
            this.staticFieldBase = unsafe.staticFieldBase(field);
            this.fieldOffset = unsafe.staticFieldOffset(field);
        } else {
            this.staticFieldBase = null;
            this.fieldOffset = unsafe.objectFieldOffset(field);
        }
    }

    public J_L_I_VarHandle(Class<?> arrayClass) {
        this.field = null;
        this.ownerClass = arrayClass;
        this.type = arrayClass.getComponentType();
        this.isStatic = false;
        this.isArray = true;
        this.staticFieldBase = null;
        this.fieldOffset = unsafe.arrayBaseOffset(arrayClass);
        this.arrayIndexScale = unsafe.arrayIndexScale(arrayClass);
    }

    private void checkStatic(Class<?> type) {
        if (isArray || !isStatic || !type.isAssignableFrom(this.type)) {
            throw new WrongMethodTypeException();
        }
    }

    private void checkOwner(Object owner, Class<?> type) {
        Objects.requireNonNull(owner);
        if (isArray || isStatic || !this.ownerClass.isAssignableFrom(owner.getClass())|| !type.isAssignableFrom(this.type)) {
            throw new WrongMethodTypeException();
        }
    }

    private void checkArray(Object array, int index, Class<?> type) {
        Objects.requireNonNull(array);
        if (!isArray || !this.ownerClass.isAssignableFrom(array.getClass()) || !type.isAssignableFrom(this.type)) {
            throw new WrongMethodTypeException();
        }
        if (index < 0 || index >= Array.getLength(array)) {
            throw new IndexOutOfBoundsException();
        }
    }

    public final Object get() {
        checkStatic(Object.class);
        return unsafe.getObject(staticFieldBase, fieldOffset);
    }

    public final boolean getZ() {
        checkStatic(boolean.class);
        return unsafe.getBoolean(staticFieldBase, fieldOffset);
    }

    public final byte getB() {
        checkStatic(byte.class);
        return unsafe.getByte(staticFieldBase, fieldOffset);
    }

    public final char getC() {
        checkStatic(char.class);
        return unsafe.getChar(staticFieldBase, fieldOffset);
    }

    public final short getS() {
        checkStatic(short.class);
        return unsafe.getShort(staticFieldBase, fieldOffset);
    }

    public final int getI() {
        checkStatic(int.class);
        return unsafe.getInt(staticFieldBase, fieldOffset);
    }

    public final long getJ() {
        checkStatic(long.class);
        return unsafe.getLong(staticFieldBase, fieldOffset);
    }

    public final float getF() {
        checkStatic(float.class);
        return unsafe.getFloat(staticFieldBase, fieldOffset);
    }

    public final double getD() {
        checkStatic(double.class);
        return unsafe.getDouble(staticFieldBase, fieldOffset);
    }

    public final Object get(Object owner) {
        checkOwner(owner, Object.class);
        return unsafe.getObject(owner, fieldOffset);
    }

    public final boolean getZ(Object owner) {
        checkOwner(owner, boolean.class);
        return unsafe.getBoolean(owner, fieldOffset);
    }

    public final byte getB(Object owner) {
        checkOwner(owner, byte.class);
        return unsafe.getByte(owner, fieldOffset);
    }

    public final char getC(Object owner) {
        checkOwner(owner, char.class);
        return unsafe.getChar(owner, fieldOffset);
    }

    public final short getS(Object owner) {
        checkOwner(owner, short.class);
        return unsafe.getShort(owner, fieldOffset);
    }

    public final int getI(Object owner) {
        checkOwner(owner, int.class);
        return unsafe.getInt(owner, fieldOffset);
    }

    public final long getJ(Object owner) {
        checkOwner(owner, long.class);
        return unsafe.getLong(owner, fieldOffset);
    }

    public final float getF(Object owner) {
        checkOwner(owner, float.class);
        return unsafe.getFloat(owner, fieldOffset);
    }

    public final double getD(Object owner) {
        checkOwner(owner, double.class);
        return unsafe.getDouble(owner, fieldOffset);
    }

    public final Object get(Object owner, int index) {
        checkArray(owner, index, Object.class);
        return unsafe.getObject(owner, fieldOffset + index * arrayIndexScale);
    }

    public final boolean getZ(Object owner, int index) {
        checkArray(owner, index, boolean.class);
        return unsafe.getBoolean(owner, fieldOffset + index * arrayIndexScale);
    }

    public final byte getB(Object owner, int index) {
        checkArray(owner, index, byte.class);
        return unsafe.getByte(owner, fieldOffset + index * arrayIndexScale);
    }

    public final char getC(Object owner, int index) {
        checkArray(owner, index, char.class);
        return unsafe.getChar(owner, fieldOffset + index * arrayIndexScale);
    }

    public final short getS(Object owner, int index) {
        checkArray(owner, index, short.class);
        return unsafe.getShort(owner, fieldOffset + index * arrayIndexScale);
    }

    public final int getI(Object owner, int index) {
        checkArray(owner, index, int.class);
        return unsafe.getInt(owner, fieldOffset + index * arrayIndexScale);
    }

    public final long getJ(Object owner, int index) {
        checkArray(owner, index, long.class);
        return unsafe.getLong(owner, fieldOffset + index * arrayIndexScale);
    }

    public final float getF(Object owner, int index) {
        checkArray(owner, index, float.class);
        return unsafe.getFloat(owner, fieldOffset + index * arrayIndexScale);
    }

    public final double getD(Object owner, int index) {
        checkArray(owner, index, double.class);
        return unsafe.getDouble(owner, fieldOffset + index * arrayIndexScale);
    }

    public final void set(Object value) {
        checkStatic(Object.class);
        unsafe.putObject(staticFieldBase, fieldOffset, value);
    }

    public final void set(boolean value) {
        checkStatic(boolean.class);
        unsafe.putBoolean(staticFieldBase, fieldOffset, value);
    }

    public final void set(byte value) {
        checkStatic(byte.class);
        unsafe.putByte(staticFieldBase, fieldOffset, value);
    }

    public final void set(char value) {
        checkStatic(char.class);
        unsafe.putChar(staticFieldBase, fieldOffset, value);
    }

    public final void set(short value) {
        checkStatic(short.class);
        unsafe.putShort(staticFieldBase, fieldOffset, value);
    }

    public final void set(int value) {
        checkStatic(int.class);
        unsafe.putInt(staticFieldBase, fieldOffset, value);
    }

    public final void set(long value) {
        checkStatic(long.class);
        unsafe.putLong(staticFieldBase, fieldOffset, value);
    }

    public final void set(float value) {
        checkStatic(float.class);
        unsafe.putFloat(staticFieldBase, fieldOffset, value);
    }

    public final void set(double value) {
        checkStatic(double.class);
        unsafe.putDouble(staticFieldBase, fieldOffset, value);
    }

    public final void set(Object owner, Object value) {
        checkOwner(owner, Object.class);
        unsafe.putObject(owner, fieldOffset, value);
    }

    public final void set(Object owner, boolean value) {
        checkOwner(owner, boolean.class);
        unsafe.putBoolean(owner, fieldOffset, value);
    }

    public final void set(Object owner, byte value) {
        checkOwner(owner, byte.class);
        unsafe.putByte(owner, fieldOffset, value);
    }

    public final void set(Object owner, char value) {
        checkOwner(owner, char.class);
        unsafe.putChar(owner, fieldOffset, value);
    }

    public final void set(Object owner, short value) {
        checkOwner(owner, short.class);
        unsafe.putShort(owner, fieldOffset, value);
    }

    public final void set(Object owner, int value) {
        checkOwner(owner, int.class);
        unsafe.putInt(owner, fieldOffset, value);
    }

    public final void set(Object owner, long value) {
        checkOwner(owner, long.class);
        unsafe.putLong(owner, fieldOffset, value);
    }

    public final void set(Object owner, float value) {
        checkOwner(owner, float.class);
        unsafe.putFloat(owner, fieldOffset, value);
    }

    public final void set(Object owner, int index, Object value) {
        checkArray(owner, index, Object.class);
        unsafe.putObject(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void set(Object owner, int index, boolean value) {
        checkArray(owner, index, boolean.class);
        unsafe.putBoolean(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void set(Object owner, int index, byte value) {
        checkArray(owner, index, byte.class);
        unsafe.putByte(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void set(Object owner, int index, char value) {
        checkArray(owner, index, char.class);
        unsafe.putChar(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void set(Object owner, int index, short value) {
        checkArray(owner, index, short.class);
        unsafe.putShort(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void set(Object owner, int index, int value) {
        checkArray(owner, index, int.class);
        unsafe.putInt(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void set(Object owner, int index, long value) {
        checkArray(owner, index, long.class);
        unsafe.putLong(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void set(Object owner, int index, float value) {
        checkArray(owner, index, float.class);
        unsafe.putFloat(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void set(Object owner, int index, double value) {
        checkArray(owner, index, double.class);
        unsafe.putDouble(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final Object getVolatile() {
        checkStatic(Object.class);
        return unsafe.getObjectVolatile(staticFieldBase, fieldOffset);
    }

    public final boolean getVolatileZ() {
        checkStatic(boolean.class);
        return unsafe.getBooleanVolatile(staticFieldBase, fieldOffset);
    }

    public final byte getVolatileB() {
        checkStatic(byte.class);
        return unsafe.getByteVolatile(staticFieldBase, fieldOffset);
    }

    public final char getVolatileC() {
        checkStatic(char.class);
        return unsafe.getCharVolatile(staticFieldBase, fieldOffset);
    }

    public final short getVolatileS() {
        checkStatic(short.class);
        return unsafe.getShortVolatile(staticFieldBase, fieldOffset);
    }

    public final int getVolatileI() {
        checkStatic(int.class);
        return unsafe.getIntVolatile(staticFieldBase, fieldOffset);
    }

    public final long getVolatileJ() {
        checkStatic(long.class);
        return unsafe.getLongVolatile(staticFieldBase, fieldOffset);
    }

    public final float getVolatileF() {
        checkStatic(float.class);
        return unsafe.getFloatVolatile(staticFieldBase, fieldOffset);
    }

    public final double getVolatileD() {
        checkStatic(double.class);
        return unsafe.getDoubleVolatile(staticFieldBase, fieldOffset);
    }

    public final Object getVolatile(Object owner) {
        checkOwner(owner, Object.class);
        return unsafe.getObjectVolatile(owner, fieldOffset);
    }

    public final boolean getVolatileZ(Object owner) {
        checkOwner(owner, boolean.class);
        return unsafe.getBooleanVolatile(owner, fieldOffset);
    }

    public final byte getVolatileB(Object owner) {
        checkOwner(owner, byte.class);
        return unsafe.getByteVolatile(owner, fieldOffset);
    }

    public final char getVolatileC(Object owner) {
        checkOwner(owner, char.class);
        return unsafe.getCharVolatile(owner, fieldOffset);
    }

    public final short getVolatileS(Object owner) {
        checkOwner(owner, short.class);
        return unsafe.getShortVolatile(owner, fieldOffset);
    }

    public final int getVolatileI(Object owner) {
        checkOwner(owner, int.class);
        return unsafe.getIntVolatile(owner, fieldOffset);
    }

    public final long getVolatileJ(Object owner) {
        checkOwner(owner, long.class);
        return unsafe.getLongVolatile(owner, fieldOffset);
    }

    public final float getVolatileF(Object owner) {
        checkOwner(owner, float.class);
        return unsafe.getFloatVolatile(owner, fieldOffset);
    }

    public final double getVolatileD(Object owner) {
        checkOwner(owner, double.class);
        return unsafe.getDoubleVolatile(owner, fieldOffset);
    }

    public final Object getVolatile(Object owner, int index) {
        checkArray(owner, index, Object.class);
        return unsafe.getObjectVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final boolean getVolatileZ(Object owner, int index) {
        checkArray(owner, index, boolean.class);
        return unsafe.getBooleanVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final byte getVolatileB(Object owner, int index) {
        checkArray(owner, index, byte.class);
        return unsafe.getByteVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final char getVolatileC(Object owner, int index) {
        checkArray(owner, index, char.class);
        return unsafe.getCharVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final short getVolatileS(Object owner, int index) {
        checkArray(owner, index, short.class);
        return unsafe.getShortVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final int getVolatileI(Object owner, int index) {
        checkArray(owner, index, int.class);
        return unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final long getVolatileJ(Object owner, int index) {
        checkArray(owner, index, long.class);
        return unsafe.getLongVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final float getVolatileF(Object owner, int index) {
        checkArray(owner, index, float.class);
        return unsafe.getFloatVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final double getVolatileD(Object owner, int index) {
        checkArray(owner, index, double.class);
        return unsafe.getDoubleVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final void setVolatile(Object value) {
        checkStatic(Object.class);
        unsafe.putObjectVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(boolean value) {
        checkStatic(boolean.class);
        unsafe.putBooleanVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(byte value) {
        checkStatic(byte.class);
        unsafe.putByteVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(char value) {
        checkStatic(char.class);
        unsafe.putCharVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(short value) {
        checkStatic(short.class);
        unsafe.putShortVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(int value) {
        checkStatic(int.class);
        unsafe.putIntVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(long value) {
        checkStatic(long.class);
        unsafe.putLongVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(float value) {
        checkStatic(float.class);
        unsafe.putFloatVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(double value) {
        checkStatic(double.class);
        unsafe.putDoubleVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setVolatile(Object owner, Object value) {
        checkOwner(owner, Object.class);
        unsafe.putObjectVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, boolean value) {
        checkOwner(owner, boolean.class);
        unsafe.putBooleanVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, byte value) {
        checkOwner(owner, byte.class);
        unsafe.putByteVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, char value) {
        checkOwner(owner, char.class);
        unsafe.putCharVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, short value) {
        checkOwner(owner, short.class);
        unsafe.putShortVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, int value) {
        checkOwner(owner, int.class);
        unsafe.putIntVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, long value) {
        checkOwner(owner, long.class);
        unsafe.putLongVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, float value) {
        checkOwner(owner, float.class);
        unsafe.putFloatVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, double value) {
        checkOwner(owner, double.class);
        unsafe.putDoubleVolatile(owner, fieldOffset, value);
    }

    public final void setVolatile(Object owner, int index, Object value) {
        checkArray(owner, index, Object.class);
        unsafe.putObjectVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setVolatile(Object owner, int index, boolean value) {
        checkArray(owner, index, boolean.class);
        unsafe.putBooleanVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setVolatile(Object owner, int index, byte value) {
        checkArray(owner, index, byte.class);
        unsafe.putByteVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setVolatile(Object owner, int index, char value) {
        checkArray(owner, index, char.class);
        unsafe.putCharVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setVolatile(Object owner, int index, short value) {
        checkArray(owner, index, short.class);
        unsafe.putShortVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setVolatile(Object owner, int index, int value) {
        checkArray(owner, index, int.class);
        unsafe.putIntVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setVolatile(Object owner, int index, long value) {
        checkArray(owner, index, long.class);
        unsafe.putLongVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setVolatile(Object owner, int index, float value) {
        checkArray(owner, index, float.class);
        unsafe.putFloatVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setVolatile(Object owner, int index, double value) {
        checkArray(owner, index, double.class);
        unsafe.putDoubleVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final Object getOpaque() {
        checkStatic(Object.class);
        return unsafe.getObjectVolatile(staticFieldBase, fieldOffset);
    }

    public final boolean getOpaqueZ() {
        checkStatic(boolean.class);
        return unsafe.getBooleanVolatile(staticFieldBase, fieldOffset);
    }

    public final byte getOpaqueB() {
        checkStatic(byte.class);
        return unsafe.getByteVolatile(staticFieldBase, fieldOffset);
    }

    public final char getOpaqueC() {
        checkStatic(char.class);
        return unsafe.getCharVolatile(staticFieldBase, fieldOffset);
    }

    public final short getOpaqueS() {
        checkStatic(short.class);
        return unsafe.getShortVolatile(staticFieldBase, fieldOffset);
    }

    public final int getOpaqueI() {
        checkStatic(int.class);
        return unsafe.getIntVolatile(staticFieldBase, fieldOffset);
    }

    public final long getOpaqueJ() {
        checkStatic(long.class);
        return unsafe.getLongVolatile(staticFieldBase, fieldOffset);
    }

    public final float getOpaqueF() {
        checkStatic(float.class);
        return unsafe.getFloatVolatile(staticFieldBase, fieldOffset);
    }

    public final double getOpaqueD() {
        checkStatic(double.class);
        return unsafe.getDoubleVolatile(staticFieldBase, fieldOffset);
    }

    public final Object getOpaque(Object owner) {
        checkOwner(owner, Object.class);
        return unsafe.getObjectVolatile(owner, fieldOffset);
    }

    public final boolean getOpaqueZ(Object owner) {
        checkOwner(owner, boolean.class);
        return unsafe.getBooleanVolatile(owner, fieldOffset);
    }

    public final byte getOpaqueB(Object owner) {
        checkOwner(owner, byte.class);
        return unsafe.getByteVolatile(owner, fieldOffset);
    }

    public final char getOpaqueC(Object owner) {
        checkOwner(owner, char.class);
        return unsafe.getCharVolatile(owner, fieldOffset);
    }

    public final short getOpaqueS(Object owner) {
        checkOwner(owner, short.class);
        return unsafe.getShortVolatile(owner, fieldOffset);
    }

    public final int getOpaqueI(Object owner) {
        checkOwner(owner, int.class);
        return unsafe.getIntVolatile(owner, fieldOffset);
    }

    public final long getOpaqueJ(Object owner) {
        checkOwner(owner, long.class);
        return unsafe.getLongVolatile(owner, fieldOffset);
    }

    public final float getOpaqueF(Object owner) {
        checkOwner(owner, float.class);
        return unsafe.getFloatVolatile(owner, fieldOffset);
    }

    public final double getOpaqueD(Object owner) {
        checkOwner(owner, double.class);
        return unsafe.getDoubleVolatile(owner, fieldOffset);
    }

    public final Object getOpaque(Object owner, int index) {
        checkArray(owner, index, Object.class);
        return unsafe.getObjectVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final boolean getOpaqueZ(Object owner, int index) {
        checkArray(owner, index, boolean.class);
        return unsafe.getBooleanVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final byte getOpaqueB(Object owner, int index) {
        checkArray(owner, index, byte.class);
        return unsafe.getByteVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final char getOpaqueC(Object owner, int index) {
        checkArray(owner, index, char.class);
        return unsafe.getCharVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final short getOpaqueS(Object owner, int index) {
        checkArray(owner, index, short.class);
        return unsafe.getShortVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final int getOpaqueI(Object owner, int index) {
        checkArray(owner, index, int.class);
        return unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final long getOpaqueJ(Object owner, int index) {
        checkArray(owner, index, long.class);
        return unsafe.getLongVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final float getOpaqueF(Object owner, int index) {
        checkArray(owner, index, float.class);
        return unsafe.getFloatVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final double getOpaqueD(Object owner, int index) {
        checkArray(owner, index, double.class);
        return unsafe.getDoubleVolatile(owner, fieldOffset + index * arrayIndexScale);
    }

    public final void setOpaque(Object value) {
        checkStatic(Object.class);
        unsafe.putObjectVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setOpaque(boolean value) {
        checkStatic(boolean.class);
        unsafe.putBooleanVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setOpaque(byte value) {
        checkStatic(byte.class);
        unsafe.putByteVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setOpaque(char value) {
        checkStatic(char.class);
        unsafe.putCharVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setOpaque(short value) {
        checkStatic(short.class);
        unsafe.putShortVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setOpaque(int value) {
        checkStatic(int.class);
        unsafe.putIntVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setOpaque(long value) {
        checkStatic(long.class);
        unsafe.putLongVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setOpaque(float value) {
        checkStatic(float.class);
        unsafe.putFloatVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setOpaque(double value) {
        checkStatic(double.class);
        unsafe.putDoubleVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setOpaque(Object owner, Object value) {
        checkOwner(owner, Object.class);
        unsafe.putObjectVolatile(owner, fieldOffset, value);
    }

    public final void setOpaque(Object owner, boolean value) {
        checkOwner(owner, boolean.class);
        unsafe.putBooleanVolatile(owner, fieldOffset, value);
    }

    public final void setOpaque(Object owner, byte value) {
        checkOwner(owner, byte.class);
        unsafe.putByteVolatile(owner, fieldOffset, value);
    }

    public final void setOpaque(Object owner, char value) {
        checkOwner(owner, char.class);
        unsafe.putCharVolatile(owner, fieldOffset, value);
    }

    public final void setOpaque(Object owner, short value) {
        checkOwner(owner, short.class);
        unsafe.putShortVolatile(owner, fieldOffset, value);
    }

    public final void setOpaque(Object owner, int value) {
        checkOwner(owner, int.class);
        unsafe.putIntVolatile(owner, fieldOffset, value);
    }

    public final void setOpaque(Object owner, long value) {
        checkOwner(owner, long.class);
        unsafe.putLongVolatile(owner, fieldOffset, value);
    }

    public final void setOpaque(Object owner, float value) {
        checkOwner(owner, float.class);
        unsafe.putFloatVolatile(owner, fieldOffset, value);
    }

    public final void setOpaque(Object owner, double value) {
        checkOwner(owner, double.class);
        unsafe.putDoubleVolatile(owner, fieldOffset, value);
    }

    public final void setOpaque(Object owner, int index, Object value) {
        checkArray(owner, index, Object.class);
        unsafe.putObjectVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setOpaque(Object owner, int index, boolean value) {
        checkArray(owner, index, boolean.class);
        unsafe.putBooleanVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setOpaque(Object owner, int index, byte value) {
        checkArray(owner, index, byte.class);
        unsafe.putByteVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setOpaque(Object owner, int index, char value) {
        checkArray(owner, index, char.class);
        unsafe.putCharVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setOpaque(Object owner, int index, short value) {
        checkArray(owner, index, short.class);
        unsafe.putShortVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setOpaque(Object owner, int index, int value) {
        checkArray(owner, index, int.class);
        unsafe.putIntVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setOpaque(Object owner, int index, long value) {
        checkArray(owner, index, long.class);
        unsafe.putLongVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setOpaque(Object owner, int index, float value) {
        checkArray(owner, index, float.class);
        unsafe.putFloatVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setOpaque(Object owner, int index, double value) {
        checkArray(owner, index, double.class);
        unsafe.putDoubleVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final Object getAcquire() {
        checkStatic(Object.class);
        Object obj = unsafe.getObjectVolatile(staticFieldBase, fieldOffset);
        unsafe.fullFence();
        return obj;
    }

    public final boolean getAcquireZ() {
        checkStatic(boolean.class);
        boolean v = unsafe.getBooleanVolatile(staticFieldBase, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final byte getAcquireB() {
        checkStatic(byte.class);
        byte v = unsafe.getByteVolatile(staticFieldBase, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final char getAcquireC() {
        checkStatic(char.class);
        char v = unsafe.getCharVolatile(staticFieldBase, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final short getAcquireS() {
        checkStatic(short.class);
        short v = unsafe.getShortVolatile(staticFieldBase, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final int getAcquireI() {
        checkStatic(int.class);
        int v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final long getAcquireJ() {
        checkStatic(long.class);
        long v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final float getAcquireF() {
        checkStatic(float.class);
        float v = unsafe.getFloatVolatile(staticFieldBase, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final double getAcquireD() {
        checkStatic(double.class);
        double v = unsafe.getDoubleVolatile(staticFieldBase, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final Object getAcquire(Object owner) {
        checkOwner(owner, Object.class);
        Object obj = unsafe.getObjectVolatile(owner, fieldOffset);
        unsafe.fullFence();
        return obj;
    }

    public final boolean getAcquireZ(Object owner) {
        checkOwner(owner, boolean.class);
        boolean v = unsafe.getBooleanVolatile(owner, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final byte getAcquireB(Object owner) {
        checkOwner(owner, byte.class);
        byte v = unsafe.getByteVolatile(owner, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final char getAcquireC(Object owner) {
        checkOwner(owner, char.class);
        char v = unsafe.getCharVolatile(owner, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final short getAcquireS(Object owner) {
        checkOwner(owner, short.class);
        short v = unsafe.getShortVolatile(owner, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final int getAcquireI(Object owner) {
        checkOwner(owner, int.class);
        int v = unsafe.getIntVolatile(owner, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final long getAcquireJ(Object owner) {
        checkOwner(owner, long.class);
        long v = unsafe.getLongVolatile(owner, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final float getAcquireF(Object owner) {
        checkOwner(owner, float.class);
        float v = unsafe.getFloatVolatile(owner, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final double getAcquireD(Object owner) {
        checkOwner(owner, double.class);
        double v = unsafe.getDoubleVolatile(owner, fieldOffset);
        unsafe.fullFence();
        return v;
    }

    public final Object getAcquire(Object owner, int index) {
        checkArray(owner, index, Object.class);
        Object obj = unsafe.getObjectVolatile(owner, fieldOffset + index * arrayIndexScale);
        unsafe.fullFence();
        return obj;
    }

    public final boolean getAcquireZ(Object owner, int index) {
        checkArray(owner, index, boolean.class);
        boolean v = unsafe.getBooleanVolatile(owner, fieldOffset + index * arrayIndexScale);
        unsafe.fullFence();
        return v;
    }

    public final byte getAcquireB(Object owner, int index) {
        checkArray(owner, index, byte.class);
        byte v = unsafe.getByteVolatile(owner, fieldOffset + index * arrayIndexScale);
        unsafe.fullFence();
        return v;
    }

    public final char getAcquireC(Object owner, int index) {
        checkArray(owner, index, char.class);
        char v = unsafe.getCharVolatile(owner, fieldOffset + index * arrayIndexScale);
        unsafe.fullFence();
        return v;
    }

    public final short getAcquireS(Object owner, int index) {
        checkArray(owner, index, short.class);
        short v = unsafe.getShortVolatile(owner, fieldOffset + index * arrayIndexScale);
        unsafe.fullFence();
        return v;
    }

    public final int getAcquireI(Object owner, int index) {
        checkArray(owner, index, int.class);
        int v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        unsafe.fullFence();
        return v;
    }

    public final long getAcquireJ(Object owner, int index) {
        checkArray(owner, index, long.class);
        long v = unsafe.getLongVolatile(owner, fieldOffset + index * arrayIndexScale);
        unsafe.fullFence();
        return v;
    }

    public final float getAcquireF(Object owner, int index) {
        checkArray(owner, index, float.class);
        float v = unsafe.getFloatVolatile(owner, fieldOffset + index * arrayIndexScale);
        unsafe.fullFence();
        return v;
    }

    public final double getAcquireD(Object owner, int index) {
        checkArray(owner, index, double.class);
        double v = unsafe.getDoubleVolatile(owner, fieldOffset + index * arrayIndexScale);
        unsafe.fullFence();
        return v;
    }

    public final void setRelease(Object value) {
        checkStatic(Object.class);
        unsafe.fullFence();
        unsafe.putObjectVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setRelease(boolean value) {
        checkStatic(boolean.class);
        unsafe.fullFence();
        unsafe.putBooleanVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setRelease(byte value) {
        checkStatic(byte.class);
        unsafe.fullFence();
        unsafe.putByteVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setRelease(char value) {
        checkStatic(char.class);
        unsafe.fullFence();
        unsafe.putCharVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setRelease(short value) {
        checkStatic(short.class);
        unsafe.fullFence();
        unsafe.putShortVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setRelease(int value) {
        checkStatic(int.class);
        unsafe.fullFence();
        unsafe.putIntVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setRelease(long value) {
        checkStatic(long.class);
        unsafe.fullFence();
        unsafe.putLongVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setRelease(float value) {
        checkStatic(float.class);
        unsafe.fullFence();
        unsafe.putFloatVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setRelease(double value) {
        checkStatic(double.class);
        unsafe.fullFence();
        unsafe.putDoubleVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setRelease(Object owner, Object value) {
        checkOwner(owner, Object.class);
        unsafe.fullFence();
        unsafe.putObjectVolatile(owner, fieldOffset, value);
    }

    public final void setRelease(Object owner, boolean value) {
        checkOwner(owner, boolean.class);
        unsafe.fullFence();
        unsafe.putBooleanVolatile(owner, fieldOffset, value);
    }

    public final void setRelease(Object owner, byte value) {
        checkOwner(owner, byte.class);
        unsafe.fullFence();
        unsafe.putByteVolatile(owner, fieldOffset, value);
    }

    public final void setRelease(Object owner, char value) {
        checkOwner(owner, char.class);
        unsafe.fullFence();
        unsafe.putCharVolatile(owner, fieldOffset, value);
    }

    public final void setRelease(Object owner, short value) {
        checkOwner(owner, short.class);
        unsafe.fullFence();
        unsafe.putShortVolatile(owner, fieldOffset, value);
    }

    public final void setRelease(Object owner, int value) {
        checkOwner(owner, int.class);
        unsafe.fullFence();
        unsafe.putIntVolatile(owner, fieldOffset, value);
    }

    public final void setRelease(Object owner, long value) {
        checkOwner(owner, long.class);
        unsafe.fullFence();
        unsafe.putLongVolatile(owner, fieldOffset, value);
    }

    public final void setRelease(Object owner, float value) {
        checkOwner(owner, float.class);
        unsafe.fullFence();
        unsafe.putFloatVolatile(owner, fieldOffset, value);
    }

    public final void setRelease(Object owner, double value) {
        checkOwner(owner, double.class);
        unsafe.fullFence();
        unsafe.putDoubleVolatile(owner, fieldOffset, value);
    }

    public final void setRelease(Object owner, int index, Object value) {
        checkArray(owner, index, Object.class);
        unsafe.fullFence();
        unsafe.putObjectVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setRelease(Object owner, int index, boolean value) {
        checkArray(owner, index, boolean.class);
        unsafe.fullFence();
        unsafe.putBooleanVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setRelease(Object owner, int index, byte value) {
        checkArray(owner, index, byte.class);
        unsafe.fullFence();
        unsafe.putByteVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setRelease(Object owner, int index, char value) {
        checkArray(owner, index, char.class);
        unsafe.fullFence();
        unsafe.putCharVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setRelease(Object owner, int index, short value) {
        checkArray(owner, index, short.class);
        unsafe.fullFence();
        unsafe.putShortVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setRelease(Object owner, int index, int value) {
        checkArray(owner, index, int.class);
        unsafe.fullFence();
        unsafe.putIntVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setRelease(Object owner, int index, long value) {
        checkArray(owner, index, long.class);
        unsafe.fullFence();
        unsafe.putLongVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setRelease(Object owner, int index, float value) {
        checkArray(owner, index, float.class);
        unsafe.fullFence();
        unsafe.putFloatVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final void setRelease(Object owner, int index, double value) {
        checkArray(owner, index, double.class);
        unsafe.fullFence();
        unsafe.putDoubleVolatile(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final boolean compareAndSet(Object expected, Object newValue) {
        checkStatic(Object.class);
        return unsafe.compareAndSwapObject(staticFieldBase, fieldOffset, expected, newValue);
    }

    public final boolean compareAndSet(boolean expected, boolean newValue) {
        checkStatic(boolean.class);
        int i;
        do {
            i = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((i != 0) != expected) return false;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, i, newValue ? 1 : 0));
        return true;
    }

    public final boolean compareAndSet(byte expected, byte newValue) {
        checkStatic(byte.class);
        int i;
        do {
            i = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((byte) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, i, newValue));
        return true;
    }

    public final boolean compareAndSet(char expected, char newValue) {
        checkStatic(char.class);
        int i;
        do {
            i = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((char) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, i, newValue));
        return true;
    }

    public final boolean compareAndSet(short expected, short newValue) {
        checkStatic(short.class);
        int i;
        do {
            i = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((short) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, i, newValue));
        return true;
    }

    public final boolean compareAndSet(int expected, int newValue) {
        checkStatic(int.class);
        return unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, expected, newValue);
    }

    public final boolean compareAndSet(long expected, long newValue) {
        checkStatic(long.class);
        return unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, expected, newValue);
    }

    public final boolean compareAndSet(float expected, float newValue) {
        checkStatic(float.class);
        return unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, Float.floatToRawIntBits(expected), Float.floatToRawIntBits(newValue));
    }

    public final boolean compareAndSet(double expected, double newValue) {
        checkStatic(double.class);
        return unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(newValue));
    }

    public final boolean compareAndSet(Object owner, Object expected, Object newValue) {
        checkOwner(owner, Object.class);
        return unsafe.compareAndSwapObject(owner, fieldOffset, expected, newValue);
    }

    public final boolean compareAndSet(Object owner, boolean expected, boolean newValue) {
        checkOwner(owner, boolean.class);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset);
            if ((i != 0) != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, i, newValue ? 1 : 0));
        return true;
    }

    public final boolean compareAndSet(Object owner, byte expected, byte newValue) {
        checkOwner(owner, byte.class);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset);
            if ((byte) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, i, newValue));
        return true;
    }

    public final boolean compareAndSet(Object owner, char expected, char newValue) {
        checkOwner(owner, char.class);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset);
            if ((char) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, i, newValue));
        return true;
    }

    public final boolean compareAndSet(Object owner, short expected, short newValue) {
        checkOwner(owner, short.class);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset);
            if ((short) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, i, newValue));
        return true;
    }

    public final boolean compareAndSet(Object owner, int expected, int newValue) {
        checkOwner(owner, int.class);
        return unsafe.compareAndSwapInt(owner, fieldOffset, expected, newValue);
    }

    public final boolean compareAndSet(Object owner, long expected, long newValue) {
        checkOwner(owner, long.class);
        return unsafe.compareAndSwapLong(owner, fieldOffset, expected, newValue);
    }

    public final boolean compareAndSet(Object owner, float expected, float newValue) {
        checkOwner(owner, float.class);
        return unsafe.compareAndSwapInt(owner, fieldOffset, Float.floatToRawIntBits(expected), Float.floatToRawIntBits(newValue));
    }

    public final boolean compareAndSet(Object owner, double expected, double newValue) {
        checkOwner(owner, double.class);
        return unsafe.compareAndSwapLong(owner, fieldOffset, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(newValue));
    }

    public final boolean compareAndSet(Object owner, int index, Object expected, Object newValue) {
        checkArray(owner, index, Object.class);
        return unsafe.compareAndSwapObject(owner, fieldOffset + index * arrayIndexScale, expected, newValue);
    }

    public final boolean compareAndSet(Object owner, int index, boolean expected, boolean newValue) {
        checkArray(owner, index, boolean.class);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
            if ((i != 0) != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, i, newValue ? 1 : 0));
        return true;
    }

    public final boolean compareAndSet(Object owner, int index, byte expected, byte newValue) {
        checkArray(owner, index, byte.class);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
            if ((byte) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, i, newValue));
        return true;
    }

    public final boolean compareAndSet(Object owner, int index, char expected, char newValue) {
        checkArray(owner, index, char.class);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
            if ((char) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, i, newValue));
        return true;
    }

    public final boolean compareAndSet(Object owner, int index, short expected, short newValue) {
        checkArray(owner, index, short.class);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
            if ((short) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, i, newValue));
        return true;
    }

    public final boolean compareAndSet(Object owner, int index, int expected, int newValue) {
        checkArray(owner, index, int.class);
        return unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, expected, newValue);
    }

    public final boolean compareAndSet(Object owner, int index, long expected, long newValue) {
        checkArray(owner, index, long.class);
        return unsafe.compareAndSwapLong(owner, fieldOffset + index * arrayIndexScale, expected, newValue);
    }

    public final boolean compareAndSet(Object owner, int index, float expected, float newValue) {
        checkArray(owner, index, float.class);
        return unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, Float.floatToRawIntBits(expected), Float.floatToRawIntBits(newValue));
    }

    public final boolean compareAndSet(Object owner, int index, double expected, double newValue) {
        checkArray(owner, index, double.class);
        return unsafe.compareAndSwapLong(owner, fieldOffset + index * arrayIndexScale, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(newValue));
    }

    public final Object compareAndExchange(Object expected, Object newValue) {
        checkStatic(Object.class);
        Object v;
        do {
            v = unsafe.getObjectVolatile(staticFieldBase, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapObject(staticFieldBase, fieldOffset, expected, newValue));
        return expected;
    }

    public final boolean compareAndExchange(boolean expected, boolean newValue) {
        checkStatic(boolean.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((v != 0) != expected) return v != 0;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, newValue ? 1 : 0));
        return expected;
    }

    public final byte compareAndExchange(byte expected, byte newValue) {
        checkStatic(byte.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((byte) v != expected) return (byte) v;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, newValue));
        return expected;
    }

    public final char compareAndExchange(char expected, char newValue) {
        checkStatic(char.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((char) v != expected) return (char) v;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, newValue));
        return expected;
    }

    public final short compareAndExchange(short expected, short newValue) {
        checkStatic(short.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if ((short) v != expected) return (short) v;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, newValue));
        return expected;
    }

    public final int compareAndExchange(int expected, int newValue) {
        checkStatic(int.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if (v != expected) return (int) v;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, newValue));
        return expected;
    }

    public final long compareAndExchange(long expected, long newValue) {
        checkStatic(long.class);
        long v;
        do {
            v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, newValue));
        return expected;
    }

    public final float compareAndExchange(float expected, float newValue) {
        checkStatic(float.class);
        float v;
        do {
            v = unsafe.getFloatVolatile(staticFieldBase, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, Float.floatToRawIntBits(v), Float.floatToRawIntBits(newValue)));
        return expected;
    }

    public final double compareAndExchange(double expected, double newValue) {
        checkStatic(double.class);
        double v;
        do {
            v = unsafe.getDoubleVolatile(staticFieldBase, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, Double.doubleToRawLongBits(v), Double.doubleToRawLongBits(newValue)));
        return expected;
    }

    public final Object compareAndExchange(Object owner, Object expected, Object newValue) {
        checkOwner(owner, Object.class);
        Object v;
        do {
            v = unsafe.getObjectVolatile(owner, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapObject(owner, fieldOffset, v, newValue));
        return expected;
    }

    public final boolean compareAndExchange(Object owner, boolean expected, boolean newValue) {
        checkOwner(owner, boolean.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
            if ((v != 0) != expected) return v != 0;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, newValue ? 1 : 0));
        return expected;
    }

    public final byte compareAndExchange(Object owner, byte expected, byte newValue) {
        checkOwner(owner, byte.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
            if ((byte) v != expected) return (byte) v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, newValue));
        return expected;
    }

    public final char compareAndExchange(Object owner, char expected, char newValue) {
        checkOwner(owner, char.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
            if ((char) v != expected) return (char) v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, newValue));
        return expected;
    }

    public final short compareAndExchange(Object owner, short expected, short newValue) {
        checkOwner(owner, short.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
            if ((short) v != expected) return (short) v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, newValue));
        return expected;
    }

    public final int compareAndExchange(Object owner, int expected, int newValue) {
        checkOwner(owner, int.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, newValue));
        return expected;
    }

    public final long compareAndExchange(Object owner, long expected, long newValue) {
        checkOwner(owner, long.class);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, newValue));
        return expected;
    }

    public final float compareAndExchange(Object owner, float expected, float newValue) {
        checkOwner(owner, float.class);
        float v;
        do {
            v = unsafe.getFloatVolatile(owner, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, Float.floatToRawIntBits(v), Float.floatToRawIntBits(newValue)));
        return expected;
    }

    public final double compareAndExchange(Object owner, double expected, double newValue) {
        checkOwner(owner, double.class);
        double v;
        do {
            v = unsafe.getDoubleVolatile(owner, fieldOffset);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, Double.doubleToRawLongBits(v), Double.doubleToRawLongBits(newValue)));
        return expected;
    }

    public final Object compareAndExchange(Object owner, int index, Object expected, Object newValue) {
        checkArray(owner, index, Object.class);
        Object v;
        do {
            v = unsafe.getObjectVolatile(owner, fieldOffset + index * arrayIndexScale);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapObject(owner, fieldOffset + index * arrayIndexScale, v, newValue));
        return expected;
    }

    public final boolean compareAndExchange(Object owner, int index, boolean expected, boolean newValue) {
        checkArray(owner, index, boolean.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
            if ((v != 0) != expected) return v != 0;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, newValue ? 1 : 0));
        return expected;
    }

    public final byte compareAndExchange(Object owner, int index, byte expected, byte newValue) {
        checkArray(owner, index, byte.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
            if ((byte) v != expected) return (byte) v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, newValue));
        return expected;
    }

    public final char compareAndExchange(Object owner, int index, char expected, char newValue) {
        checkArray(owner, index, char.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
            if ((char) v != expected) return (char) v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, newValue));
        return expected;
    }

    public final short compareAndExchange(Object owner, int index, short expected, short newValue) {
        checkArray(owner, index, short.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
            if ((short) v != expected) return (short) v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, newValue));
        return expected;
    }

    public final int compareAndExchange(Object owner, int index, int expected, int newValue) {
        checkArray(owner, index, int.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, newValue));
        return expected;
    }

    public final long compareAndExchange(Object owner, int index, long expected, long newValue) {
        checkArray(owner, index, long.class);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset + index * arrayIndexScale);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset + index * arrayIndexScale, v, newValue));
        return expected;
    }

    public final float compareAndExchange(Object owner, int index, float expected, float newValue) {
        checkArray(owner, index, float.class);
        float v;
        do {
            v = unsafe.getFloatVolatile(owner, fieldOffset + index * arrayIndexScale);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, Float.floatToRawIntBits(v), Float.floatToRawIntBits(newValue)));
        return expected;
    }

    public final double compareAndExchange(Object owner, int index, double expected, double newValue) {
        checkArray(owner, index, double.class);
        double v;
        do {
            v = unsafe.getDoubleVolatile(owner, fieldOffset + index * arrayIndexScale);
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset + index * arrayIndexScale, Double.doubleToRawLongBits(v), Double.doubleToRawLongBits(newValue)));
        return expected;
    }

    public final Object compareAndExchangeAcquire(Object expected, Object newValue) {
        Object v = compareAndExchange(expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final boolean compareAndExchangeAcquire(boolean expected, boolean newValue) {
        boolean v = compareAndExchange(expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final byte compareAndExchangeAcquire(byte expected, byte newValue) {
        byte v = compareAndExchange(expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final char compareAndExchangeAcquire(char expected, char newValue) {
        char v = compareAndExchange(expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final short compareAndExchangeAcquire(short expected, short newValue) {
        short v = compareAndExchange(expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final int compareAndExchangeAcquire(int expected, int newValue) {
        int v = compareAndExchange(expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final long compareAndExchangeAcquire(long expected, long newValue) {
        long v = compareAndExchange(expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final float compareAndExchangeAcquire(float expected, float newValue) {
        float v = compareAndExchange(expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final double compareAndExchangeAcquire(double expected, double newValue) {
        double v = compareAndExchange(expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final Object compareAndExchangeAcquire(Object owner, Object expected, Object newValue) {
        Object v = compareAndExchange(owner, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final boolean compareAndExchangeAcquire(Object owner, boolean expected, boolean newValue) {
        boolean v = compareAndExchange(owner, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final byte compareAndExchangeAcquire(Object owner, byte expected, byte newValue) {
        byte v = compareAndExchange(owner, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final char compareAndExchangeAcquire(Object owner, char expected, char newValue) {
        char v = compareAndExchange(owner, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final short compareAndExchangeAcquire(Object owner, short expected, short newValue) {
        short v = compareAndExchange(owner, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final int compareAndExchangeAcquire(Object owner, int expected, int newValue) {
        int v = compareAndExchange(owner, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final long compareAndExchangeAcquire(Object owner, long expected, long newValue) {
        long v = compareAndExchange(owner, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final float compareAndExchangeAcquire(Object owner, float expected, float newValue) {
        float v = compareAndExchange(owner, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final double compareAndExchangeAcquire(Object owner, double expected, double newValue) {
        double v = compareAndExchange(owner, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final Object compareAndExchangeAcquire(Object owner, int index, Object expected, Object newValue) {
        Object v = compareAndExchange(owner, index, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final boolean compareAndExchangeAcquire(Object owner, int index, boolean expected, boolean newValue) {
        boolean v = compareAndExchange(owner, index, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final byte compareAndExchangeAcquire(Object owner, int index, byte expected, byte newValue) {
        byte v = compareAndExchange(owner, index, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final char compareAndExchangeAcquire(Object owner, int index, char expected, char newValue) {
        char v = compareAndExchange(owner, index, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final short compareAndExchangeAcquire(Object owner, int index, short expected, short newValue) {
        short v = compareAndExchange(owner, index, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final int compareAndExchangeAcquire(Object owner, int index, int expected, int newValue) {
        int v = compareAndExchange(owner, index, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final long compareAndExchangeAcquire(Object owner, int index, long expected, long newValue) {
        long v = compareAndExchange(owner, index, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final float compareAndExchangeAcquire(Object owner, int index, float expected, float newValue) {
        float v = compareAndExchange(owner, index, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final double compareAndExchangeAcquire(Object owner, int index, double expected, double newValue) {
        double v = compareAndExchange(owner, index, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    public final Object compareAndExchangeRelease(Object expected, Object newValue) {
        unsafe.fullFence();
        return compareAndExchange(expected, newValue);
    }

    public final boolean compareAndExchangeRelease(boolean expected, boolean newValue) {
        unsafe.fullFence();
        return compareAndExchange(expected, newValue);
    }

    public final byte compareAndExchangeRelease(byte expected, byte newValue) {
        unsafe.fullFence();
        return compareAndExchange(expected, newValue);
    }

    public final char compareAndExchangeRelease(char expected, char newValue) {
        unsafe.fullFence();
        return compareAndExchange(expected, newValue);
    }

    public final short compareAndExchangeRelease(short expected, short newValue) {
        unsafe.fullFence();
        return compareAndExchange(expected, newValue);
    }

    public final int compareAndExchangeRelease(int expected, int newValue) {
        unsafe.fullFence();
        return compareAndExchange(expected, newValue);
    }

    public final long compareAndExchangeRelease(long expected, long newValue) {
        unsafe.fullFence();
        return compareAndExchange(expected, newValue);
    }

    public final float compareAndExchangeRelease(float expected, float newValue) {
        unsafe.fullFence();
        return compareAndExchange(expected, newValue);
    }

    public final double compareAndExchangeRelease(double expected, double newValue) {
        unsafe.fullFence();
        return compareAndExchange(expected, newValue);
    }

    public final Object compareAndExchangeRelease(Object owner, Object expected, Object newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, expected, newValue);
    }

    public final boolean compareAndExchangeRelease(Object owner, boolean expected, boolean newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, expected, newValue);
    }

    public final byte compareAndExchangeRelease(Object owner, byte expected, byte newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, expected, newValue);
    }

    public final char compareAndExchangeRelease(Object owner, char expected, char newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, expected, newValue);
    }

    public final short compareAndExchangeRelease(Object owner, short expected, short newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, expected, newValue);
    }

    public final int compareAndExchangeRelease(Object owner, int expected, int newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, expected, newValue);
    }

    public final long compareAndExchangeRelease(Object owner, long expected, long newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, expected, newValue);
    }

    public final float compareAndExchangeRelease(Object owner, float expected, float newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, expected, newValue);
    }

    public final double compareAndExchangeRelease(Object owner, double expected, double newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, expected, newValue);
    }

    public final Object compareAndExchangeRelease(Object owner, int index, Object expected, Object newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, index, expected, newValue);
    }

    public final boolean compareAndExchangeRelease(Object owner, int index, boolean expected, boolean newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, index, expected, newValue);
    }

    public final byte compareAndExchangeRelease(Object owner, int index, byte expected, byte newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, index, expected, newValue);
    }

    public final char compareAndExchangeRelease(Object owner, int index, char expected, char newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, index, expected, newValue);
    }

    public final short compareAndExchangeRelease(Object owner, int index, short expected, short newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, index, expected, newValue);
    }

    public final int compareAndExchangeRelease(Object owner, int index, int expected, int newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, index, expected, newValue);
    }

    public final long compareAndExchangeRelease(Object owner, int index, long expected, long newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, index, expected, newValue);
    }

    public final float compareAndExchangeRelease(Object owner, int index, float expected, float newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, index, expected, newValue);
    }

    public final double compareAndExchangeRelease(Object owner, int index, double expected, double newValue) {
        unsafe.fullFence();
        return compareAndExchange(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object expected, Object newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(boolean expected, boolean newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(byte expected, byte newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(char expected, char newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(short expected, short newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(int expected, int newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(long expected, long newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(float expected, float newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(double expected, double newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, Object expected, Object newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, boolean expected, boolean newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, byte expected, byte newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, char expected, char newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, short expected, short newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int expected, int newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, long expected, long newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, float expected, float newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, double expected, double newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, Object expected, Object newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, boolean expected, boolean newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, byte expected, byte newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, char expected, char newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, short expected, short newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, int expected, int newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, long expected, long newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, float expected, float newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, double expected, double newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object expected, Object newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSet(boolean expected, boolean newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSet(byte expected, byte newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSet(char expected, char newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSet(short expected, short newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSet(int expected, int newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSet(long expected, long newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSet(float expected, float newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSet(double expected, double newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, Object expected, Object newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, boolean expected, boolean newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, byte expected, byte newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, char expected, char newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, short expected, short newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int expected, int newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, long expected, long newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, float expected, float newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, double expected, double newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, Object expected, Object newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, boolean expected, boolean newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, byte expected, byte newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, char expected, char newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, short expected, short newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, int expected, int newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, long expected, long newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, float expected, float newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, double expected, double newValue) {
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetAcquire(Object expected, Object newValue) {
        boolean b = compareAndSet(expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(boolean expected, boolean newValue) {
        boolean b = compareAndSet(expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(byte expected, byte newValue) {
        boolean b = compareAndSet(expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(char expected, char newValue) {
        boolean b = compareAndSet(expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(short expected, short newValue) {
        boolean b = compareAndSet(expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(int expected, int newValue) {
        boolean b = compareAndSet(expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(long expected, long newValue) {
        boolean b = compareAndSet(expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(float expected, float newValue) {
        boolean b = compareAndSet(expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(double expected, double newValue) {
        boolean b = compareAndSet(expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, Object expected, Object newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, boolean expected, boolean newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, byte expected, byte newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, char expected, char newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, short expected, short newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int expected, int newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, long expected, long newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, float expected, float newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, double expected, double newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, Object expected, Object newValue) {
        boolean b = compareAndSet(owner, index, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, boolean expected, boolean newValue) {
        boolean b = compareAndSet(owner, index, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, byte expected, byte newValue) {
        boolean b = compareAndSet(owner, index, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, char expected, char newValue) {
        boolean b = compareAndSet(owner, index, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, short expected, short newValue) {
        boolean b = compareAndSet(owner, index, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, int expected, int newValue) {
        boolean b = compareAndSet(owner, index, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, long expected, long newValue) {
        boolean b = compareAndSet(owner, index, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, float expected, float newValue) {
        boolean b = compareAndSet(owner, index, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, double expected, double newValue) {
        boolean b = compareAndSet(owner, index, expected, newValue);
        unsafe.fullFence();
        return b;
    }

    public final boolean weakCompareAndSetRelease(Object expected, Object newValue) {
        unsafe.fullFence();
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(boolean expected, boolean newValue) {
        unsafe.fullFence();
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(byte expected, byte newValue) {
        unsafe.fullFence();
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(char expected, char newValue) {
        unsafe.fullFence();
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(short expected, short newValue) {
        unsafe.fullFence();
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(int expected, int newValue) {
        unsafe.fullFence();
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(long expected, long newValue) {
        unsafe.fullFence();
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(float expected, float newValue) {
        unsafe.fullFence();
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(double expected, double newValue) {
        unsafe.fullFence();
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, Object expected, Object newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, boolean expected, boolean newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, byte expected, byte newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, char expected, char newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, short expected, short newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int expected, int newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, long expected, long newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, float expected, float newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, double expected, double newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, Object expected, Object newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, boolean expected, boolean newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, byte expected, byte newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, char expected, char newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, short expected, short newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, int expected, int newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, long expected, long newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, float expected, float newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, double expected, double newValue) {
        unsafe.fullFence();
        return compareAndSet(owner, index, expected, newValue);
    }

    public final Object getAndSet(Object newValue) {
        checkStatic(Object.class);
        return unsafe.getAndSetObject(staticFieldBase, fieldOffset, newValue);
    }

    public final boolean getAndSet(boolean newValue) {
        checkStatic(boolean.class);
        return unsafe.getAndSetInt(staticFieldBase, fieldOffset, newValue ? 1 : 0) != 0;
    }

    public final byte getAndSet(byte newValue) {
        checkStatic(byte.class);
        return (byte) unsafe.getAndSetInt(staticFieldBase, fieldOffset, newValue);
    }

    public final char getAndSet(char newValue) {
        checkStatic(char.class);
        return (char) unsafe.getAndSetInt(staticFieldBase, fieldOffset, newValue);
    }

    public final short getAndSet(short newValue) {
        checkStatic(short.class);
        return (short) unsafe.getAndSetInt(staticFieldBase, fieldOffset, newValue);
    }

    public final int getAndSet(int newValue) {
        checkStatic(int.class);
        return unsafe.getAndSetInt(staticFieldBase, fieldOffset, newValue);
    }

    public final long getAndSet(long newValue) {
        checkStatic(long.class);
        return unsafe.getAndSetLong(staticFieldBase, fieldOffset, newValue);
    }

    public final float getAndSet(float newValue) {
        checkStatic(float.class);
        return Float.intBitsToFloat(unsafe.getAndSetInt(staticFieldBase, fieldOffset, Float.floatToRawIntBits(newValue)));
    }

    public final double getAndSet(double newValue) {
        checkStatic(double.class);
        return Double.longBitsToDouble(unsafe.getAndSetLong(staticFieldBase, fieldOffset, Double.doubleToRawLongBits(newValue)));
    }

    public final Object getAndSet(Object owner, Object newValue) {
        checkOwner(owner, Object.class);
        return unsafe.getAndSetObject(owner, fieldOffset, newValue);
    }

    public final boolean getAndSet(Object owner, boolean newValue) {
        checkOwner(owner, boolean.class);
        return unsafe.getAndSetInt(owner, fieldOffset, newValue ? 1 : 0) != 0;
    }

    public final byte getAndSet(Object owner, byte newValue) {
        checkOwner(owner, byte.class);
        return (byte) unsafe.getAndSetInt(owner, fieldOffset, newValue);
    }

    public final char getAndSet(Object owner, char newValue) {
        checkOwner(owner, char.class);
        return (char) unsafe.getAndSetInt(owner, fieldOffset, newValue);
    }

    public final short getAndSet(Object owner, short newValue) {
        checkOwner(owner, short.class);
        return (short) unsafe.getAndSetInt(owner, fieldOffset, newValue);
    }

    public final int getAndSet(Object owner, int newValue) {
        checkOwner(owner, int.class);
        return unsafe.getAndSetInt(owner, fieldOffset, newValue);
    }

    public final long getAndSet(Object owner, long newValue) {
        checkOwner(owner, long.class);
        return unsafe.getAndSetLong(owner, fieldOffset, newValue);
    }

    public final float getAndSet(Object owner, float newValue) {
        checkOwner(owner, float.class);
        return Float.intBitsToFloat(unsafe.getAndSetInt(owner, fieldOffset, Float.floatToRawIntBits(newValue)));
    }

    public final double getAndSet(Object owner, double newValue) {
        checkOwner(owner, double.class);
        return Double.longBitsToDouble(unsafe.getAndSetLong(owner, fieldOffset, Double.doubleToRawLongBits(newValue)));
    }

    public final Object getAndSet(Object owner, int index, Object newValue) {
        checkArray(owner, index, Object.class);
        return unsafe.getAndSetObject(owner, fieldOffset + index * arrayIndexScale, index);
    }

    public final boolean getAndSet(Object owner, int index, boolean newValue) {
        checkArray(owner, index, boolean.class);
        return unsafe.getAndSetInt(owner, fieldOffset + index * arrayIndexScale, newValue ? 1 : 0) != 0;
    }

    public final byte getAndSet(Object owner, int index, byte newValue) {
        checkArray(owner, index, byte.class);
        return (byte) unsafe.getAndSetInt(owner, fieldOffset + index * arrayIndexScale, newValue);
    }

    public final char getAndSet(Object owner, int index, char newValue) {
        checkArray(owner, index, char.class);
        return (char) unsafe.getAndSetInt(owner, fieldOffset + index * arrayIndexScale, newValue);
    }

    public final short getAndSet(Object owner, int index, short newValue) {
        checkArray(owner, index, short.class);
        return (short) unsafe.getAndSetInt(owner, fieldOffset + index * arrayIndexScale, newValue);
    }

    public final int getAndSet(Object owner, int index, int newValue) {
        checkArray(owner, index, int.class);
        return unsafe.getAndSetInt(owner, fieldOffset + index * arrayIndexScale, newValue);
    }

    public final long getAndSet(Object owner, int index, long newValue) {
        checkArray(owner, index, long.class);
        return unsafe.getAndSetLong(owner, fieldOffset + index * arrayIndexScale, newValue);
    }

    public final float getAndSet(Object owner, int index, float newValue) {
        checkArray(owner, index, float.class);
        return Float.intBitsToFloat(unsafe.getAndSetInt(owner, fieldOffset + index * arrayIndexScale, Float.floatToRawIntBits(newValue)));
    }

    public final double getAndSet(Object owner, int index, double newValue) {
        checkArray(owner, index, double.class);
        return Double.longBitsToDouble(unsafe.getAndSetLong(owner, fieldOffset + index * arrayIndexScale, Double.doubleToRawLongBits(newValue)));
    }

    public final Object getAndSetAcquire(Object newValue) {
        Object v = getAndSet(newValue);
        unsafe.fullFence();
        return v;
    }

    public final boolean getAndSetAcquire(boolean newValue) {
        boolean v = getAndSet(newValue);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndSetAcquire(byte newValue) {
        byte v = getAndSet(newValue);
        unsafe.fullFence();
        return v;
    }

    public final char getAndSetAcquire(char newValue) {
        char v = getAndSet(newValue);
        unsafe.fullFence();
        return v;
    }

    public final short getAndSetAcquire(short newValue) {
        short v = getAndSet(newValue);
        unsafe.fullFence();
        return v;
    }

    public final int getAndSetAcquire(int newValue) {
        int v = getAndSet(newValue);
        unsafe.fullFence();
        return v;
    }

    public final long getAndSetAcquire(long newValue) {
        long v = getAndSet(newValue);
        unsafe.fullFence();
        return v;
    }

    public final float getAndSetAcquire(float newValue) {
        float v = getAndSet(newValue);
        unsafe.fullFence();
        return v;
    }

    public final double getAndSetAcquire(double newValue) {
        double v = getAndSet(newValue);
        unsafe.fullFence();
        return v;
    }

    public final Object getAndSetAcquire(Object owner, Object newValue) {
        Object v = getAndSet(owner, newValue);
        unsafe.fullFence();
        return v;
    }

    public final boolean getAndSetAcquire(Object owner, boolean newValue) {
        boolean v = getAndSet(owner, newValue);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndSetAcquire(Object owner, byte newValue) {
        byte v = getAndSet(owner, newValue);
        unsafe.fullFence();
        return v;
    }

    public final char getAndSetAcquire(Object owner, char newValue) {
        char v = getAndSet(owner, newValue);
        unsafe.fullFence();
        return v;
    }

    public final short getAndSetAcquire(Object owner, short newValue) {
        short v = getAndSet(owner, newValue);
        unsafe.fullFence();
        return v;
    }

    public final int getAndSetAcquire(Object owner, int newValue) {
        int v = getAndSet(owner, newValue);
        unsafe.fullFence();
        return v;
    }

    public final long getAndSetAcquire(Object owner, long newValue) {
        long v = getAndSet(owner, newValue);
        unsafe.fullFence();
        return v;
    }

    public final float getAndSetAcquire(Object owner, float newValue) {
        float v = getAndSet(owner, newValue);
        unsafe.fullFence();
        return v;
    }

    public final double getAndSetAcquire(Object owner, double newValue) {
        double v = getAndSet(owner, newValue);
        unsafe.fullFence();
        return v;
    }

    public final Object getAndSetAcquire(Object owner, int index, Object newValue) {
        Object v = getAndSet(owner, index, newValue);
        unsafe.fullFence();
        return v;
    }

    public final boolean getAndSetAcquire(Object owner, int index, boolean newValue) {
        boolean v = getAndSet(owner, index, newValue);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndSetAcquire(Object owner, int index, byte newValue) {
        byte v = getAndSet(owner, index, newValue);
        unsafe.fullFence();
        return v;
    }

    public final char getAndSetAcquire(Object owner, int index, char newValue) {
        char v = getAndSet(owner, index, newValue);
        unsafe.fullFence();
        return v;
    }

    public final short getAndSetAcquire(Object owner, int index, short newValue) {
        short v = getAndSet(owner, index, newValue);
        unsafe.fullFence();
        return v;
    }

    public final int getAndSetAcquire(Object owner, int index, int newValue) {
        int v = getAndSet(owner, index, newValue);
        unsafe.fullFence();
        return v;
    }

    public final long getAndSetAcquire(Object owner, int index, long newValue) {
        long v = getAndSet(owner, index, newValue);
        unsafe.fullFence();
        return v;
    }

    public final float getAndSetAcquire(Object owner, int index, float newValue) {
        float v = getAndSet(owner, index, newValue);
        unsafe.fullFence();
        return v;
    }

    public final double getAndSetAcquire(Object owner, int index, double newValue) {
        double v = getAndSet(owner, index, newValue);
        unsafe.fullFence();
        return v;
    }

    public final Object getAndSetRelease(Object newValue) {
        unsafe.fullFence();
        return getAndSet(newValue);
    }

    public final boolean getAndSetRelease(boolean newValue) {
        unsafe.fullFence();
        return getAndSet(newValue);
    }

    public final byte getAndSetRelease(byte newValue) {
        unsafe.fullFence();
        return getAndSet(newValue);
    }

    public final char getAndSetRelease(char newValue) {
        unsafe.fullFence();
        return getAndSet(newValue);
    }

    public final short getAndSetRelease(short newValue) {
        unsafe.fullFence();
        return getAndSet(newValue);
    }

    public final int getAndSetRelease(int newValue) {
        unsafe.fullFence();
        return getAndSet(newValue);
    }

    public final long getAndSetRelease(long newValue) {
        unsafe.fullFence();
        return getAndSet(newValue);
    }

    public final float getAndSetRelease(float newValue) {
        unsafe.fullFence();
        return getAndSet(newValue);
    }

    public final double getAndSetRelease(double newValue) {
        unsafe.fullFence();
        return getAndSet(newValue);
    }

    public final Object getAndSetRelease(Object owner, Object newValue) {
        unsafe.fullFence();
        return getAndSet(owner, newValue);
    }

    public final boolean getAndSetRelease(Object owner, boolean newValue) {
        unsafe.fullFence();
        return getAndSet(owner, newValue);
    }

    public final byte getAndSetRelease(Object owner, byte newValue) {
        unsafe.fullFence();
        return getAndSet(owner, newValue);
    }

    public final char getAndSetRelease(Object owner, char newValue) {
        unsafe.fullFence();
        return getAndSet(owner, newValue);
    }

    public final short getAndSetRelease(Object owner, short newValue) {
        unsafe.fullFence();
        return getAndSet(owner, newValue);
    }

    public final int getAndSetRelease(Object owner, int newValue) {
        unsafe.fullFence();
        return getAndSet(owner, newValue);
    }

    public final long getAndSetRelease(Object owner, long newValue) {
        unsafe.fullFence();
        return getAndSet(owner, newValue);
    }

    public final float getAndSetRelease(Object owner, float newValue) {
        unsafe.fullFence();
        return getAndSet(owner, newValue);
    }

    public final double getAndSetRelease(Object owner, double newValue) {
        unsafe.fullFence();
        return getAndSet(owner, newValue);
    }

    public final Object getAndSetRelease(Object owner, int index, Object newValue) {
        unsafe.fullFence();
        return getAndSet(owner, index, newValue);
    }

    public final boolean getAndSetRelease(Object owner, int index, boolean newValue) {
        unsafe.fullFence();
        return getAndSet(owner, index, newValue);
    }

    public final byte getAndSetRelease(Object owner, int index, byte newValue) {
        unsafe.fullFence();
        return getAndSet(owner, index, newValue);
    }

    public final char getAndSetRelease(Object owner, int index, char newValue) {
        unsafe.fullFence();
        return getAndSet(owner, index, newValue);
    }

    public final short getAndSetRelease(Object owner, int index, short newValue) {
        unsafe.fullFence();
        return getAndSet(owner, index, newValue);
    }

    public final int getAndSetRelease(Object owner, int index, int newValue) {
        unsafe.fullFence();
        return getAndSet(owner, index, newValue);
    }

    public final long getAndSetRelease(Object owner, int index, long newValue) {
        unsafe.fullFence();
        return getAndSet(owner, index, newValue);
    }

    public final float getAndSetRelease(Object owner, int index, float newValue) {
        unsafe.fullFence();
        return getAndSet(owner, index, newValue);
    }

    public final double getAndSetRelease(Object owner, int index, double newValue) {
        unsafe.fullFence();
        return getAndSet(owner, index, newValue);
    }

    public final byte getAndAdd(byte value) {
        checkStatic(byte.class);
        byte v;
        do {
            v = unsafe.getByte(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v + value));
        return v;
    }

    public final char getAndAdd(char value) {
        checkStatic(char.class);
        char v;
        do {
            v = unsafe.getChar(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, value));
        return v;
    }

    public final short getAndAdd(short value) {
        checkStatic(short.class);
        short v;
        do {
            v = unsafe.getShort(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, value));
        return v;
    }

    public final int getAndAdd(int value) {
        checkStatic(int.class);
        return unsafe.getAndAddInt(staticFieldBase, fieldOffset, value);
    }

    public final long getAndAdd(long value) {
        checkStatic(long.class);
        return unsafe.getAndAddLong(staticFieldBase, fieldOffset, value);
    }

    public final float getAndAdd(float value) {
        checkStatic(float.class);
        float v;
        do {
            v = unsafe.getFloat(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, Float.floatToRawIntBits(v), Float.floatToRawIntBits(v + value)));
        return v;
    }

    public final double getAndAdd(double value) {
        checkStatic(double.class);
        double v;
        do {
            v = unsafe.getDouble(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, Double.doubleToRawLongBits(v), Double.doubleToRawLongBits(v + value)));
        return v;
    }

    public final byte getAndAdd(Object owner, byte value) {
        checkOwner(owner, byte.class);
        byte v;
        do {
            v = unsafe.getByte(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v + value));
        return v;
    }

    public final char getAndAdd(Object owner, char value) {
        checkOwner(owner, char.class);
        char v;
        do {
            v = unsafe.getChar(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v + value));
        return v;
    }

    public final short getAndAdd(Object owner, short value) {
        checkOwner(owner, short.class);
        short v;
        do {
            v = unsafe.getShort(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v + value));
        return v;
    }

    public final int getAndAdd(Object owner, int value) {
        checkOwner(owner, int.class);
        return unsafe.getAndAddInt(owner, fieldOffset, value);
    }

    public final long getAndAdd(Object owner, long value) {
        checkOwner(owner, long.class);
        return unsafe.getAndAddLong(owner, fieldOffset, value);
    }

    public final float getAndAdd(Object owner, float value) {
        checkOwner(owner, float.class);
        float v;
        do {
            v = unsafe.getFloat(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, Float.floatToRawIntBits(v), Float.floatToRawIntBits(v + value)));
        return v;
    }

    public final double getAndAdd(Object owner, double value) {
        checkOwner(owner, double.class);
        double v;
        do {
            v = unsafe.getDouble(owner, fieldOffset);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, Double.doubleToRawLongBits(v), Double.doubleToRawLongBits(v + value)));
        return v;
    }

    public final byte getAndAdd(Object owner, int index, byte value) {
        checkArray(owner, index, byte.class);
        byte v;
        do {
            v = unsafe.getByte(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v + value));
        return v;
    }

    public final char getAndAdd(Object owner, int index, char value) {
        checkArray(owner, index, char.class);
        char v;
        do {
            v = unsafe.getChar(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v + value));
        return v;
    }

    public final short getAndAdd(Object owner, int index, short value) {
        checkArray(owner, index, short.class);
        short v;
        do {
            v = unsafe.getShort(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v + value));
        return v;
    }

    public final int getAndAdd(Object owner, int index, int value) {
        checkArray(owner, index, int.class);
        return unsafe.getAndAddInt(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final long getAndAdd(Object owner, int index, long value) {
        checkArray(owner, index, long.class);
        return unsafe.getAndAddLong(owner, fieldOffset + index * arrayIndexScale, value);
    }

    public final float getAndAdd(Object owner, int index, float value) {
        checkArray(owner, index, float.class);
        float v;
        do {
            v = unsafe.getFloat(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, Float.floatToRawIntBits(v), Float.floatToRawIntBits(v + value)));
        return v;
    }

    public final double getAndAdd(Object owner, int index, double value) {
        checkArray(owner, index, double.class);
        double v;
        do {
            v = unsafe.getDouble(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset + index * arrayIndexScale, Double.doubleToRawLongBits(v), Double.doubleToRawLongBits(v + value)));
        return v;
    }

    public final byte getAndAddAcquire(byte value) {
        byte v = getAndAdd(value);
        unsafe.fullFence();
        return v;
    }

    public final char getAndAddAcquire(char value) {
        char v = getAndAdd(value);
        unsafe.fullFence();
        return v;
    }

    public final short getAndAddAcquire(short value) {
        short v = getAndAdd(value);
        unsafe.fullFence();
        return v;
    }

    public final int getAndAddAcquire(int value) {
        int v = getAndAdd(value);
        unsafe.fullFence();
        return v;
    }

    public final long getAndAddAcquire(long value) {
        long v = getAndAdd(value);
        unsafe.fullFence();
        return v;
    }

    public final float getAndAddAcquire(float value) {
        float v = getAndAdd(value);
        unsafe.fullFence();
        return v;
    }

    public final double getAndAddAcquire(double value) {
        double v = getAndAdd(value);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndAddAcquire(Object owner, byte value) {
        byte v = getAndAdd(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final char getAndAddAcquire(Object owner, char value) {
        char v = getAndAdd(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final short getAndAddAcquire(Object owner, short value) {
        short v = getAndAdd(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final int getAndAddAcquire(Object owner, int value) {
        int v = getAndAdd(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final long getAndAddAcquire(Object owner, long value) {
        long v = getAndAdd(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final float getAndAddAcquire(Object owner, float value) {
        float v = getAndAdd(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final double getAndAddAcquire(Object owner, double value) {
        double v = getAndAdd(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndAddAcquire(Object owner, int index, byte value) {
        byte v = getAndAdd(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final char getAndAddAcquire(Object owner, int index, char value) {
        char v = getAndAdd(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final short getAndAddAcquire(Object owner, int index, short value) {
        short v = getAndAdd(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final int getAndAddAcquire(Object owner, int index, int value) {
        int v = getAndAdd(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final long getAndAddAcquire(Object owner, int index, long value) {
        long v = getAndAdd(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final float getAndAddAcquire(Object owner, int index, float value) {
        float v = getAndAdd(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final double getAndAddAcquire(Object owner, int index, double value) {
        double v = getAndAdd(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndAddRelease(byte value) {
        unsafe.fullFence();
        return getAndAdd(value);
    }

    public final char getAndAddRelease(char value) {
        unsafe.fullFence();
        return getAndAdd(value);
    }

    public final short getAndAddRelease(short value) {
        unsafe.fullFence();
        return getAndAdd(value);
    }

    public final int getAndAddRelease(int value) {
        unsafe.fullFence();
        return getAndAdd(value);
    }

    public final long getAndAddRelease(long value) {
        unsafe.fullFence();
        return getAndAdd(value);
    }

    public final float getAndAddRelease(float value) {
        unsafe.fullFence();
        return getAndAdd(value);
    }

    public final double getAndAddRelease(double value) {
        unsafe.fullFence();
        return getAndAdd(value);
    }

    public final byte getAndAddRelease(Object owner, byte value) {
        unsafe.fullFence();
        return getAndAdd(owner, value);
    }

    public final char getAndAddRelease(Object owner, char value) {
        unsafe.fullFence();
        return getAndAdd(owner, value);
    }

    public final short getAndAddRelease(Object owner, short value) {
        unsafe.fullFence();
        return getAndAdd(owner, value);
    }

    public final int getAndAddRelease(Object owner, int value) {
        unsafe.fullFence();
        return getAndAdd(value);
    }

    public final long getAndAddRelease(Object owner, long value) {
        unsafe.fullFence();
        return getAndAdd(value);
    }

    public final float getAndAddRelease(Object owner, float value) {
        unsafe.fullFence();
        return getAndAdd(owner, value);
    }

    public final double getAndAddRelease(Object owner, double value) {
        unsafe.fullFence();
        return getAndAdd(owner, value);
    }

    public final byte getAndAddRelease(Object owner, int index, byte value) {
        unsafe.fullFence();
        return getAndAdd(owner, index, value);
    }

    public final char getAndAddRelease(Object owner, int index, char value) {
        unsafe.fullFence();
        return getAndAdd(owner, index, value);
    }

    public final short getAndAddRelease(Object owner, int index, short value) {
        unsafe.fullFence();
        return getAndAdd(owner, index, value);
    }

    public final int getAndAddRelease(Object owner, int index, int value) {
        unsafe.fullFence();
        return getAndAdd(owner, index, value);
    }

    public final long getAndAddRelease(Object owner, int index, long value) {
        unsafe.fullFence();
        return getAndAdd(owner, index, value);
    }

    public final float getAndAddRelease(Object owner, int index, float value) {
        unsafe.fullFence();
        return getAndAdd(owner, index, value);
    }

    public final double getAndAddRelease(Object owner, int index, double value) {
        unsafe.fullFence();
        return getAndAdd(owner, index, value);
    }


    public final boolean getAndBitwiseOr(boolean value) {
        checkStatic(boolean.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, value || v != 0 ? 1 : 0));
        return v != 0;
    }

    public final byte getAndBitwiseOr(byte value) {
        checkStatic(byte.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v | value));
        return (byte) v;
    }

    public final char getAndBitwiseOr(char value) {
        checkStatic(char.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v | value));
        return (char) v;
    }

    public final short getAndBitwiseOr(short value) {
        checkStatic(short.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v | value));
        return (short) v;
    }

    public final int getAndBitwiseOr(int value) {
        checkStatic(int.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v | value));
        return v;
    }

    public final long getAndBitwiseOr(long value) {
        checkStatic(long.class);
        long v;
        do {
            v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, v | value));
        return v;
    }

    public final boolean getAndBitwiseOr(Object owner, boolean value) {
        checkOwner(owner, boolean.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, value || v != 0 ? 1 : 0));
        return v != 0;
    }

    public final byte getAndBitwiseOr(Object owner, byte value) {
        checkOwner(owner, byte.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v | value));
        return (byte) v;
    }

    public final char getAndBitwiseOr(Object owner, char value) {
        checkOwner(owner, char.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v | value));
        return (char) v;
    }

    public final short getAndBitwiseOr(Object owner, short value) {
        checkOwner(owner, short.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v | value));
        return (short) v;
    }

    public final int getAndBitwiseOr(Object owner, int value) {
        checkOwner(owner, int.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v | value));
        return v;
    }

    public final long getAndBitwiseOr(Object owner, long value) {
        checkOwner(owner, long.class);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, v | value));
        return v;
    }

    public final boolean getAndBitwiseOr(Object owner, int index, boolean value) {
        checkArray(owner, index, boolean.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v != 0 || value ? 1 : 0));
        return v != 0;
    }

    public final byte getAndBitwiseOr(Object owner, int index, byte value) {
        checkArray(owner, index, byte.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v | value));
        return (byte) v;
    }

    public final char getAndBitwiseOr(Object owner, int index, char value) {
        checkArray(owner, index, char.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v | value));
        return (char) v;
    }

    public final short getAndBitwiseOr(Object owner, int index, short value) {
        checkArray(owner, index, short.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v | value));
        return (short) v;
    }

    public final int getAndBitwiseOr(Object owner, int index, int value) {
        checkArray(owner, index, int.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v | value));
        return v;
    }

    public final long getAndBitwiseOr(Object owner, int index, long value) {
        checkArray(owner, index, long.class);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset + index * arrayIndexScale, v, v | value));
        return v;
    }


    public final boolean getAndBitwiseOrAcquire(boolean value) {
        boolean v = getAndBitwiseOr(value);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndBitwiseOrAcquire(byte value) {
        byte v = getAndBitwiseOr(value);
        unsafe.fullFence();
        return v;
    }

    public final char getAndBitwiseOrAcquire(char value) {
        char v = getAndBitwiseOr(value);
        unsafe.fullFence();
        return v;
    }

    public final short getAndBitwiseOrAcquire(short value) {
        short v = getAndBitwiseOr(value);
        unsafe.fullFence();
        return v;
    }

    public final int getAndBitwiseOrAcquire(int value) {
        int v = getAndBitwiseOr(value);
        unsafe.fullFence();
        return v;
    }

    public final long getAndBitwiseOrAcquire(long value) {
        long v = getAndBitwiseOr(value);
        unsafe.fullFence();
        return v;
    }

    public final boolean getAndBitwiseOrAcquire(Object owner, boolean value) {
        boolean v = getAndBitwiseOr(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndBitwiseOrAcquire(Object owner, byte value) {
        byte v = getAndBitwiseOr(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final char getAndBitwiseOrAcquire(Object owner, char value) {
        char v = getAndBitwiseOr(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final short getAndBitwiseOrAcquire(Object owner, short value) {
        short v = getAndBitwiseOr(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final int getAndBitwiseOrAcquire(Object owner, int value) {
        int v = getAndBitwiseOr(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final long getAndBitwiseOrAcquire(Object owner, long value) {
        long v = getAndBitwiseOr(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final boolean getAndBitwiseOrAcquire(Object owner, int index, boolean value) {
        boolean v = getAndBitwiseOr(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndBitwiseOrAcquire(Object owner, int index, byte value) {
        byte v = getAndBitwiseOr(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final char getAndBitwiseOrAcquire(Object owner, int index, char value) {
        char v = getAndBitwiseOr(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final short getAndBitwiseOrAcquire(Object owner, int index, short value) {
        short v = getAndBitwiseOr(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final int getAndBitwiseOrAcquire(Object owner, int index, int value) {
        int v = getAndBitwiseOr(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final long getAndBitwiseOrAcquire(Object owner, int index, long value) {
        long v = getAndBitwiseOr(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final boolean getAndBitwiseOrRelease(boolean value) {
        unsafe.fullFence();
        return getAndBitwiseOr(value);
    }

    public final byte getAndBitwiseOrRelease(byte value) {
        unsafe.fullFence();
        return getAndBitwiseOr(value);
    }

    public final short getAndBitwiseOrRelease(short value) {
        unsafe.fullFence();
        return getAndBitwiseOr(value);
    }

    public final char getAndBitwiseOrRelease(char value) {
        unsafe.fullFence();
        return getAndBitwiseOr(value);
    }

    public final int getAndBitwiseOrRelease(int value) {
        unsafe.fullFence();
        return getAndBitwiseOr(value);
    }

    public final long getAndBitwiseOrRelease(long value) {
        unsafe.fullFence();
        return getAndBitwiseOr(value);
    }

    public final boolean getAndBitwiseOrRelease(Object owner, boolean value) {
        unsafe.fullFence();
        return getAndBitwiseOr(owner, value);
    }

    public final byte getAndBitwiseOrRelease(Object owner, byte value) {
        unsafe.fullFence();
        return getAndBitwiseOr(owner, value);
    }

    public final char getAndBitwiseOrRelease(Object owner, char value) {
        unsafe.fullFence();
        return getAndBitwiseOr(owner, value);
    }

    public final short getAndBitwiseOrRelease(Object owner, short value) {
        unsafe.fullFence();
        return getAndBitwiseOr(owner, value);
    }

    public final int getAndBitwiseOrRelease(Object owner, int value) {
        unsafe.fullFence();
        return getAndBitwiseOr(owner, value);
    }

    public final long getAndBitwiseOrRelease(Object owner, long value) {
        unsafe.fullFence();
        return getAndBitwiseOr(owner, value);
    }

    public final boolean getAndBitwiseOrRelease(Object owner, int index, boolean value) {
        unsafe.fullFence();
        return getAndBitwiseOr(owner, index, value);
    }

    public final byte getAndBitwiseOrRelease(Object owner, int index, byte value) {
        unsafe.fullFence();
        return getAndBitwiseOr(owner, index, value);
    }

    public final char getAndBitwiseOrRelease(Object owner, int index, char value) {
        unsafe.fullFence();
        return getAndBitwiseOr(owner, index, value);
    }

    public final short getAndBitwiseOrRelease(Object owner, int index, short value) {
        unsafe.fullFence();
        return getAndBitwiseOr(owner, index, value);
    }

    public final int getAndBitwiseOrRelease(Object owner, int index, int value) {
        unsafe.fullFence();
        return getAndBitwiseOr(owner, index, value);
    }

    public final long getAndBitwiseOrRelease(Object owner, int index, long value) {
        unsafe.fullFence();
        return getAndBitwiseOr(owner, index, value);
    }

    public final boolean getAndBitwiseAnd(boolean value) {
        checkStatic(boolean.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v != 0 && value ? 1 : 0));
        return v != 0;
    }

    public final byte getAndBitwiseAnd(byte value) {
        checkStatic(byte.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v & value));
        return (byte) v;
    }

    public final char getAndBitwiseAnd(char value) {
        checkStatic(char.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v & value));
        return (char) v;
    }

    public final short getAndBitwiseAnd(short value) {
        checkStatic(short.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v & value));
        return (short) v;
    }

    public final int getAndBitwiseAnd(int value) {
        checkStatic(int.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v & value));
        return v;
    }

    public final long getAndBitwiseAnd(long value) {
        checkStatic(long.class);
        long v;
        do {
            v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, v & value));
        return v;
    }

    public final boolean getAndBitwiseAnd(Object owner, boolean value) {
        checkOwner(owner, boolean.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v != 0 && value ? 1 : 0));
        return v != 0;
    }

    public final byte getAndBitwiseAnd(Object owner, byte value) {
        checkOwner(owner, byte.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v & value));
        return (byte) v;
    }

    public final char getAndBitwiseAnd(Object owner, char value) {
        checkOwner(owner, char.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v & value));
        return (char) v;
    }

    public final short getAndBitwiseAnd(Object owner, short value) {
        checkOwner(owner, short.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v & value));
        return (short) v;
    }

    public final int getAndBitwiseAnd(Object owner, int value) {
        checkOwner(owner, int.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v & value));
        return v;
    }

    public final long getAndBitwiseAnd(Object owner, long value) {
        checkOwner(owner, long.class);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, v & value));
        return v;
    }

    public final boolean getAndBitwiseAnd(Object owner, int index, boolean value) {
        checkArray(owner, index, boolean.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v != 0 && value ? 1 : 0));
        return v != 0;
    }

    public final byte getAndBitwiseAnd(Object owner, int index, byte value) {
        checkArray(owner, index, byte.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v & value));
        return (byte) v;
    }

    public final char getAndBitwiseAnd(Object owner, int index, char value) {
        checkArray(owner, index, char.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v & value));
        return (char) v;
    }

    public final short getAndBitwiseAnd(Object owner, int index, short value) {
        checkArray(owner, index, short.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v & value));
        return (short) v;
    }

    public final int getAndBitwiseAnd(Object owner, int index, int value) {
        checkArray(owner, index, int.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v & value));
        return v;
    }

    public final long getAndBitwiseAnd(Object owner, int index, long value) {
        checkArray(owner, index, long.class);
        long v;
        do {
             v = unsafe.getLongVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset + index * arrayIndexScale, v, v & value));
        return v;
    }

    public final boolean getAndBitwiseAndAcquire(boolean value) {
        boolean v = getAndBitwiseAnd(value);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndBitwiseAndAcquire(byte value) {
        byte v = getAndBitwiseAnd(value);
        unsafe.fullFence();
        return v;
    }

    public final char getAndBitwiseAndAcquire(char value) {
        char v = getAndBitwiseAnd(value);
        unsafe.fullFence();
        return v;
    }

    public final short getAndBitwiseAndAcquire(short value) {
        short v = getAndBitwiseAnd(value);
        unsafe.fullFence();
        return v;
    }

    public final int getAndBitwiseAndAcquire(int value) {
        int v = getAndBitwiseAnd(value);
        unsafe.fullFence();
        return v;
    }

    public final long getAndBitwiseAndAcquire(long value) {
        long v = getAndBitwiseAnd(value);
        unsafe.fullFence();
        return v;
    }

    public final boolean getAndBitwiseAndAcquire(Object owner, boolean value) {
        boolean v = getAndBitwiseAnd(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndBitwiseAndAcquire(Object owner, byte value) {
        byte v = getAndBitwiseAnd(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final char getAndBitwiseAndAcquire(Object owner, char value) {
        char v = getAndBitwiseAnd(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final short getAndBitwiseAndAcquire(Object owner, short value) {
        short v = getAndBitwiseAnd(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final int getAndBitwiseAndAcquire(Object owner, int value) {
        int v = getAndBitwiseAnd(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final long getAndBitwiseAndAcquire(Object owner, long value) {
        long v = getAndBitwiseAnd(owner, value);
        unsafe.fullFence();
        return v;
    }
    
    public final boolean getAndBitwiseAndAcquire(Object owner, int index, boolean value) {
        boolean v = getAndBitwiseAnd(owner, index, value);
        unsafe.fullFence();
        return v;
    }
    
    public final byte getAndBitwiseAndAcquire(Object owner, int index, byte value) {
        byte v = getAndBitwiseAnd(owner, index, value);
        unsafe.fullFence();
        return v;
    }
    
    public final char getAndBitwiseAndAcquire(Object owner, int index, char value) {
        char v = getAndBitwiseAnd(owner, index, value);
        unsafe.fullFence();
        return v;
    }
    
    public final short getAndBitwiseAndAcquire(Object owner, int index, short value) {
        short v = getAndBitwiseAnd(owner, index, value);
        unsafe.fullFence();
        return v;
    }
    
    public final int getAndBitwiseAndAcquire(Object owner, int index, int value) {
        int v = getAndBitwiseAnd(owner, index, value);
        unsafe.fullFence();
        return v;
    }
    
    public final long getAndBitwiseAndAcquire(Object owner, int index, long value) {
        long v = getAndBitwiseAnd(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final boolean getAndBitwiseAndRelease(boolean value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(value);
    }

    public final byte getAndBitwiseAndRelease(byte value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(value);
    }

    public final char getAndBitwiseAndRelease(char value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(value);
    }

    public final short getAndBitwiseAndRelease(short value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(value);
    }

    public final int getAndBitwiseAndRelease(int value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(value);
    }

    public final long getAndBitwiseAndRelease(long value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(value);
    }

    public final boolean getAndBitwiseAndRelease(Object owner, boolean value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(owner, value);
    }

    public final byte getAndBitwiseAndRelease(Object owner, byte value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(owner, value);
    }

    public final char getAndBitwiseAndRelease(Object owner, char value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(owner, value);
    }

    public final short getAndBitwiseAndRelease(Object owner, short value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(owner, value);
    }

    public final int getAndBitwiseAndRelease(Object owner, int value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(owner, value);
    }

    public final long getAndBitwiseAndRelease(Object owner, long value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(owner, value);
    }

    public final boolean getAndBitwiseAndRelease(Object owner, int index, boolean value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(owner, index, value);
    }

    public final byte getAndBitwiseAndRelease(Object owner, int index, byte value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(owner, index, value);
    }

    public final char getAndBitwiseAndRelease(Object owner, int index, char value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(owner, index, value);
    }

    public final short getAndBitwiseAndRelease(Object owner, int index, short value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(owner, index, value);
    }

    public final int getAndBitwiseAndRelease(Object owner, int index, int value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(owner, index, value);
    }

    public final long getAndBitwiseAndRelease(Object owner, int index, long value) {
        unsafe.fullFence();
        return getAndBitwiseAnd(owner, index, value);
    }

    public final boolean getAndBitwiseXor(boolean value) {
        checkStatic(boolean.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, (v != 0) ^ value ? 1 : 0));
        return v != 0;
    }

    public final byte getAndBitwiseXor(byte value) {
        checkStatic(byte.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v ^ value));
        return (byte) v;
    }

    public final char getAndBitwiseXor(char value) {
        checkStatic(char.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v ^ value));
        return (char) v;
    }


    public final short getAndBitwiseXor(short value) {
        checkStatic(short.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v ^ value));
        return (short) v;
    }

    public final int getAndBitwiseXor(int value) {
        checkStatic(int.class);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, v ^ value));
        return v;
    }

    public final long getAndBitwiseXor(long value) {
        checkStatic(long.class);
        long v;
        do {
            v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, v ^ value));
        return v;
    }

    public final boolean getAndBitwiseXor(Object owner, boolean value) {
        checkOwner(owner, boolean.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, (v != 0) ^ value ? 1 : 0));
        return v != 0;
    }

    public final byte getAndBitwiseXor(Object owner, byte value) {
        checkOwner(owner, byte.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v ^ value));
        return (byte) v;
    }

    public final char getAndBitwiseXor(Object owner, char value) {
        checkOwner(owner, char.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v ^ value));
        return (char) v;
    }

    public final short getAndBitwiseXor(Object owner, short value) {
        checkOwner(owner, short.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v ^ value));
        return (short) v;
    }

    public final int getAndBitwiseXor(Object owner, int value) {
        checkOwner(owner, int.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, v ^ value));
        return v;
    }

    public final long getAndBitwiseXor(Object owner, long value) {
        checkOwner(owner, long.class);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, v ^ value));
        return v;
    }

    public final boolean getAndBitwiseXor(Object owner, int index, boolean value) {
        checkArray(owner, index, boolean.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, (v != 0) ^ value ? 1 : 0));
        return v != 0;
    }

    public final byte getAndBitwiseXor(Object owner, int index, byte value) {
        checkArray(owner, index, byte.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v ^ value));
        return (byte) v;
    }

    public final char getAndBitwiseXor(Object owner, int index, char value) {
        checkArray(owner, index, char.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v ^ value));
        return (char) v;
    }

    public final short getAndBitwiseXor(Object owner, int index, short value) {
        checkArray(owner, index, short.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v ^ value));
        return (short) v;
    }

    public final int getAndBitwiseXor(Object owner, int index, int value) {
        checkArray(owner, index, int.class);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset + index * arrayIndexScale, v, v ^ value));
        return v;
    }

    public final long getAndBitwiseXor(Object owner, int index, long value) {
        checkArray(owner, index, long.class);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset + index * arrayIndexScale);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset + index * arrayIndexScale, v, v ^ value));
        return v;
    }


    public final boolean getAndBitwiseXorAcquire(boolean value) {
        boolean v = getAndBitwiseXor(value);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndBitwiseXorAcquire(byte value) {
        byte v = getAndBitwiseXor(value);
        unsafe.fullFence();
        return v;
    }

    public final char getAndBitwiseXorAcquire(char value) {
        char v = getAndBitwiseXor(value);
        unsafe.fullFence();
        return v;
    }

    public final short getAndBitwiseXorAcquire(short value) {
        short v = getAndBitwiseXor(value);
        unsafe.fullFence();
        return v;
    }

    public final int getAndBitwiseXorAcquire(int value) {
        int v = getAndBitwiseXor(value);
        unsafe.fullFence();
        return v;
    }

    public final long getAndBitwiseXorAcquire(long value) {
        long v = getAndBitwiseXor(value);
        unsafe.fullFence();
        return v;
    }

    public final boolean getAndBitwiseXorAcquire(Object owner, boolean value) {
        boolean v = getAndBitwiseXor(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndBitwiseXorAcquire(Object owner, byte value) {
        byte v = getAndBitwiseXor(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final char getAndBitwiseXorAcquire(Object owner, char value) {
        char v = getAndBitwiseXor(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final short getAndBitwiseXorAcquire(Object owner, short value) {
        short v = getAndBitwiseXor(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final int getAndBitwiseXorAcquire(Object owner, int value) {
        int v = getAndBitwiseXor(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final long getAndBitwiseXorAcquire(Object owner, long value) {
        long v = getAndBitwiseXor(owner, value);
        unsafe.fullFence();
        return v;
    }

    public final boolean getAndBitwiseXorAcquire(Object owner, int index, boolean value) {
        boolean v = getAndBitwiseXor(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final byte getAndBitwiseXorAcquire(Object owner, int index, byte value) {
        byte v = getAndBitwiseXor(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final char getAndBitwiseXorAcquire(Object owner, int index, char value) {
        char v = getAndBitwiseXor(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final short getAndBitwiseXorAcquire(Object owner, int index, short value) {
        short v = getAndBitwiseXor(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final int getAndBitwiseXorAcquire(Object owner, int index, int value) {
        int v = getAndBitwiseXor(owner, index, value);
        unsafe.fullFence();
        return v;
    }

    public final long getAndBitwiseXorAcquire(Object owner, int index, long value) {
        long v = getAndBitwiseXor(owner, index, value);
        unsafe.fullFence();
        return v;
    }

     public final boolean getAndBitwiseXorRelease(boolean value) {
        unsafe.fullFence();
        return getAndBitwiseXor(value);
    }

    public final byte getAndBitwiseXorRelease(byte value) {
        unsafe.fullFence();
        return getAndBitwiseXor(value);
    }

    public final short getAndBitwiseXorRelease(short value) {
        unsafe.fullFence();
        return getAndBitwiseXor(value);
    }

    public final char getAndBitwiseXorRelease(char value) {
        unsafe.fullFence();
        return getAndBitwiseXor(value);
    }

    public final int getAndBitwiseXorRelease(int value) {
        unsafe.fullFence();
        return getAndBitwiseXor(value);
    }

    public final long getAndBitwiseXorRelease(long value) {
        unsafe.fullFence();
        return getAndBitwiseXor(value);
    }

    public final boolean getAndBitwiseXorRelease(Object owner, boolean value) {
        unsafe.fullFence();
        return getAndBitwiseXor(owner, value);
    }

    public final byte getAndBitwiseXorRelease(Object owner, byte value) {
        unsafe.fullFence();
        return getAndBitwiseXor(owner, value);
    }

    public final char getAndBitwiseXorRelease(Object owner, char value) {
        unsafe.fullFence();
        return getAndBitwiseXor(owner, value);
    }

    public final short getAndBitwiseXorRelease(Object owner, short value) {
        unsafe.fullFence();
        return getAndBitwiseXor(owner, value);
    }

    public final int getAndBitwiseXorRelease(Object owner, int value) {
        unsafe.fullFence();
        return getAndBitwiseXor(owner, value);
    }

    public final long getAndBitwiseXorRelease(Object owner, long value) {
        unsafe.fullFence();
        return getAndBitwiseXor(owner, value);
    }

    public final boolean getAndBitwiseXorRelease(Object owner, int index, boolean value) {
        unsafe.fullFence();
        return getAndBitwiseXor(owner, index, value);
    }

    public final byte getAndBitwiseXorRelease(Object owner, int index, byte value) {
        unsafe.fullFence();
        return getAndBitwiseXor(owner, index, value);
    }

    public final char getAndBitwiseXorRelease(Object owner, int index, char value) {
        unsafe.fullFence();
        return getAndBitwiseXor(owner, index, value);
    }

    public final short getAndBitwiseXorRelease(Object owner, int index, short value) {
        unsafe.fullFence();
        return getAndBitwiseXor(owner, index, value);
    }

    public final int getAndBitwiseXorRelease(Object owner, int index, int value) {
        unsafe.fullFence();
        return getAndBitwiseXor(owner, index, value);
    }

    public final long getAndBitwiseXorRelease(Object owner, int index, long value) {
        unsafe.fullFence();
        return getAndBitwiseXor(owner, index, value);
    }

    public final MethodHandle toMethodHandle(AccessMode accessMode) throws NoSuchMethodException, IllegalAccessException {
        MethodType mt;
        MethodType vmt;
        Class<?> t;
        Class<?> vt = field.getType();
        if (field.getType().isPrimitive()) {
            t = field.getType();
        } else {
            t = Object.class;
        }
        switch (accessMode.accessType) {
            case GET:
                if (isStatic) {
                    mt = MethodType.methodType(t);
                    vmt = MethodType.methodType(vt);
                } else {
                    mt = MethodType.methodType(t, Object.class);
                    vmt = MethodType.methodType(vt, field.getDeclaringClass());
                }
                break;
            case SET:
                if (isStatic) {
                    mt = MethodType.methodType(void.class, t);
                    vmt = MethodType.methodType(void.class, vt);
                } else {
                    mt = MethodType.methodType(void.class, Object.class, t);
                    vmt = MethodType.methodType(void.class, field.getDeclaringClass(), vt);
                }
                break;
            case COMPARE_AND_SET:
                if (isStatic) {
                    mt = MethodType.methodType(boolean.class, t, t);
                    vmt = MethodType.methodType(boolean.class, vt, vt);
                } else {
                    mt = MethodType.methodType(boolean.class, Object.class, t, t);
                    vmt = MethodType.methodType(boolean.class, field.getDeclaringClass(), vt, vt);
                }
                break;
            case COMPARE_AND_EXCHANGE:
                if (isStatic) {
                    mt = MethodType.methodType(t, t, t);
                    vmt = MethodType.methodType(vt, vt, vt);
                } else {
                    mt = MethodType.methodType(t, Object.class, t, t);
                    vmt = MethodType.methodType(vt, field.getDeclaringClass(), vt, vt);
                }
                break;
            case GET_AND_UPDATE:
                if (isStatic) {
                    mt = MethodType.methodType(t, t);
                    vmt = MethodType.methodType(vt, vt);
                } else {
                    mt = MethodType.methodType(t, Object.class, t);
                    vmt = MethodType.methodType(vt, field.getDeclaringClass(), vt);
                }
                break;
            default:
                throw new IllegalStateException();
        }
        return lookup.findVirtual(J_L_I_VarHandle.class, fixMethodName(accessMode.methodName(), Utils.getDescForClass(field.getType())), mt).bindTo(this).asType(vmt);
    }

    public static void fullFence() {
        unsafe.fullFence();
    }

    public static void acquireFence() {
        unsafe.loadFence();
    }

    public static void releaseFence() {
        unsafe.storeFence();
    }

    public static void loadLoadFence() {
        unsafe.loadFence();
    }

    public static void storeStoreFence() {
        unsafe.storeFence();
    }

    @Adapter("java/lang/invoke/VarHandle$AccessType")
    public enum AccessType {
        GET,
        SET,
        COMPARE_AND_SET,
        COMPARE_AND_EXCHANGE,
        GET_AND_UPDATE;
    }

    @Adapter("java/lang/invoke/VarHandle$AccessMode")
    public enum AccessMode {
        GET("get", AccessType.GET),
        SET("set", AccessType.SET),
        GET_VOLATILE("getVolatile", AccessType.GET),
        SET_VOLATILE("setVolatile", AccessType.SET),
        GET_ACQUIRE("getAcquire", AccessType.GET),
        SET_RELEASE("setRelease", AccessType.SET),
        GET_OPAQUE("getOpaque", AccessType.GET),
        SET_OPAQUE("setOpaque", AccessType.SET),
        COMPARE_AND_SET("compareAndSet", AccessType.COMPARE_AND_SET),
        COMPARE_AND_EXCHANGE("compareAndExchange", AccessType.COMPARE_AND_EXCHANGE),
        COMPARE_AND_EXCHANGE_ACQUIRE("compareAndExchangeAcquire", AccessType.COMPARE_AND_EXCHANGE),
        COMPARE_AND_EXCHANGE_RELEASE("compareAndExchangeRelease", AccessType.COMPARE_AND_EXCHANGE),
        WEAK_COMPARE_AND_SET_PLAIN("compareAndSetPlain", AccessType.COMPARE_AND_SET),
        WEAK_COMPARE_AND_SET("compareAndSet", AccessType.COMPARE_AND_SET),
        WEAK_COMPARE_AND_SET_ACQUIRE("compareAndSetAcquire", AccessType.COMPARE_AND_SET),
        WEAK_COMPARE_AND_SET_RELEASE("compareAndSetRelease", AccessType.COMPARE_AND_SET),
        GET_AND_SET("getAndSet", AccessType.GET_AND_UPDATE),
        GET_AND_SET_ACQUIRE("getAndSetAcquire", AccessType.GET_AND_UPDATE),
        GET_AND_SET_RELEASE("getAndSetRelease", AccessType.GET_AND_UPDATE),
        GET_AND_ADD("getAndAdd", AccessType.GET_AND_UPDATE),
        GET_AND_ADD_ACQUIRE("getAndAddAcquire", AccessType.GET_AND_UPDATE),
        GET_AND_ADD_RELEASE("getAndAddRelease", AccessType.GET_AND_UPDATE),
        GET_AND_BITWISE_OR("getAndBitwiseOr", AccessType.GET_AND_UPDATE),
        GET_AND_BITWISE_OR_RELEASE("getAndBitwiseOrRelease", AccessType.GET_AND_UPDATE),
        GET_AND_BITWISE_OR_ACQUIRE("getAndBitwiseOrAcquire", AccessType.GET_AND_UPDATE),
        GET_AND_BITWISE_XOR("getAndBitwiseXor", AccessType.GET_AND_UPDATE),
        GET_AND_BITWISE_XOR_RELEASE("getAndBitwiseXorRelease", AccessType.GET_AND_UPDATE),
        GET_AND_BITWISE_XOR_ACQUIRE("getAndBitwiseXorAcquire", AccessType.GET_AND_UPDATE)
        ;

        static final Map<String, AccessMode> byName = new HashMap<>();

        static {
            for (AccessMode value : AccessMode.values()) {
                byName.put(value.methodName(), value);
            }
        }

        final String methodName;
        final AccessType accessType;

        AccessMode(String methodName, AccessType at) {
            this.methodName = methodName;
            this.accessType = at;
        }

        public String methodName() {
            return methodName;
        }

        public static AccessMode valueFromMethodName(String methodName) {
            AccessMode am = byName.get(methodName);
            if (am == null) {
                throw new IllegalArgumentException("No AccessMode value for method name " + methodName);
            }
            return am;
        }
    }
}
