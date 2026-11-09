package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.exc.PartialStubError;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JEP(193)
@Adapter("java/lang/invoke/VarHandle")
public class J_L_I_VarHandle {
    private static final Unsafe unsafe = Utils.getUnsafe();

    @Modify(ref = @Ref("xyz/wagyourtail/jvmdg/j9/stub/java_base/J_L_I_VarHandle"))
    public static void modifyPolymorphics(MethodNode mnode, int i) {
        MethodInsnNode min = (MethodInsnNode) mnode.instructions.get(i);
        AccessMode am = AccessMode.byName.get(min.name);
        if (am == null) return;
        Type ret = Type.getReturnType(min.desc);
        min.name = fixMethodName(min.name, ret.getDescriptor());
        Type[] args = Type.getArgumentTypes(min.desc);
        for (int j = 0; j < args.length; j++) {
            if (args[j].getSort() >= 9) args[j] = Type.getObjectType("java/lang/Object");
        }
        if (ret.getSort() >= 9) {
            if (!ret.getInternalName().equals("java/lang/Object")) {
                mnode.instructions.insert(min, new TypeInsnNode(Opcodes.CHECKCAST, ret.getInternalName()));
            }
            ret = Type.getObjectType("java/lang/Object");
        }
        if (ret.getInternalName().equals("java/lang/Object")) {
            switch (am.accessType) {
                case GET:
                    break;
                case SET:
                case COMPARE_AND_SET:
                    throw new IllegalStateException();
                case COMPARE_AND_EXCHANGE:
                case GET_AND_UPDATE:
                    Type lastArg = args[args.length - 1];
                    if (lastArg.getSort() < 9) {
                        ret = lastArg;
                        Handle box = ASMUtils.boxType(lastArg);
                        assert box != null;
                        mnode.instructions.insert(min, new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            box.getOwner(),
                            box.getName(),
                            box.getDesc(),
                            false
                        ));
                    }
            }
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
    private final boolean byteArrayView;
    private final boolean reverseBytes;

    public J_L_I_VarHandle(Field field) {
        this.field = field;
        this.ownerClass = field.getDeclaringClass();
        this.type = field.getType();
        this.isStatic = Modifier.isStatic(field.getModifiers());
        this.isArray = false;
        this.arrayIndexScale = 0;
        this.byteArrayView = false;
        this.reverseBytes = false;
        if (this.isStatic) {
            this.staticFieldBase = unsafe.staticFieldBase(field);
            this.fieldOffset = unsafe.staticFieldOffset(field);
        } else {
            this.staticFieldBase = null;
            this.fieldOffset = unsafe.objectFieldOffset(field);
        }
    }

    public J_L_I_VarHandle(Class<?> arrayClass) {
        this(arrayClass, false, false);
    }

    public J_L_I_VarHandle(Class<?> arrayClass, boolean byteArrayView, boolean reverseBytes) {
        this.field = null;
        this.ownerClass = arrayClass;
        this.type = arrayClass.getComponentType();
        this.isStatic = false;
        this.isArray = true;
        this.staticFieldBase = null;
        this.fieldOffset = byteArrayView ? unsafe.arrayBaseOffset(byte[].class) : unsafe.arrayBaseOffset(arrayClass);
        this.arrayIndexScale = unsafe.arrayIndexScale(arrayClass);
        this.byteArrayView = byteArrayView;
        this.reverseBytes = reverseBytes;
    }

    private void checkStatic(Class<?> type, boolean read, boolean write) {
        if (isArray || !isStatic || (read && !type.isAssignableFrom(this.type)) || (write && (type == null ? this.type.isPrimitive() : !this.type.isAssignableFrom(type)))) {
            throw new WrongMethodTypeException();
        }
    }

    private void checkOwner(Object owner, Class<?> type, boolean read, boolean write) {
        Objects.requireNonNull(owner);
        if (isArray || isStatic || !this.ownerClass.isAssignableFrom(owner.getClass())|| (read && !type.isAssignableFrom(this.type)) || (write && (type == null ? this.type.isPrimitive() : !this.type.isAssignableFrom(type)))) {
            throw new WrongMethodTypeException();
        }
    }

    private void checkArray(Object array, int index, Class<?> type, boolean read, boolean write) {
        Objects.requireNonNull(array);
        if (!isArray) throw new WrongMethodTypeException();
        if (byteArrayView) {
            if (type != this.type || array.getClass() != byte[].class) {
                throw new WrongMethodTypeException();
            }
            if (index < 0 || index > ((byte[]) array).length - arrayIndexScale) {
                throw new IndexOutOfBoundsException();
            }
        } else {
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
    }

    private Object arrayView(Object owner) {
        if (byteArrayView && owner instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer) owner;
            if (!bb.hasArray()) {
                throw PartialStubError.create();
            }
            return bb.array();
        }
        return owner;
    }

    private int arrayIndexOffset(Object owner) {
        if (byteArrayView && owner instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer) owner;
            return bb.hasArray() ? bb.arrayOffset() : 0;
        }
        return 0;
    }

    private long indexOffset(int index) {
        return fieldOffset + (byteArrayView ? index : index * arrayIndexScale);
    }

    private char rev(char v) {
        return reverseBytes ? Character.reverseBytes(v) : v;
    }

    private short rev(short v) {
        return reverseBytes ? Short.reverseBytes(v) : v;
    }

    private int rev(int v) {
        return reverseBytes ? Integer.reverseBytes(v) : v;
    }

    private long rev(long v) {
        return reverseBytes ? Long.reverseBytes(v) : v;
    }

    private float rev(float v) {
        return reverseBytes ? Float.intBitsToFloat(Integer.reverseBytes(Float.floatToRawIntBits(v))) : v;
    }

    private double rev(double v) {
        return reverseBytes ? Double.longBitsToDouble(Long.reverseBytes(Double.doubleToRawLongBits(v))) : v;
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
                if (reverseBytes) {
                    return Character.reverseBytes(c);
                } else {
                    return c;
                }
            } else if (type == short.class) {
                checkStatic(short.class, true, false);
                return rev(unsafe.getShort(staticFieldBase, fieldOffset));
            } else if (type == int.class) {
                checkStatic(int.class, true, false);
                return rev(unsafe.getInt(staticFieldBase, fieldOffset));
            } else if (type == long.class) {
                checkStatic(long.class, true, false);
                return rev(unsafe.getLong(staticFieldBase, fieldOffset));
            } else if (type == float.class) {
                checkStatic(float.class, true, false);
                return rev(unsafe.getFloat(staticFieldBase, fieldOffset));
            } else if (type == double.class) {
                checkStatic(double.class, true, false);
                return rev(unsafe.getDouble(staticFieldBase, fieldOffset));
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
        return rev(unsafe.getChar(staticFieldBase, fieldOffset));
    }

    public final short getS() {
        checkStatic(short.class, true, false);
        return rev(unsafe.getShort(staticFieldBase, fieldOffset));
    }

    public final int getI() {
        checkStatic(int.class, true, false);
        return rev(unsafe.getInt(staticFieldBase, fieldOffset));
    }

    public final long getJ() {
        checkStatic(long.class, true, false);
        return rev(unsafe.getLong(staticFieldBase, fieldOffset));
    }

    public final float getF() {
        checkStatic(float.class, true, false);
        return rev(unsafe.getFloat(staticFieldBase, fieldOffset));
    }

    public final double getD() {
        checkStatic(double.class, true, false);
        return rev(unsafe.getDouble(staticFieldBase, fieldOffset));
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
                return rev(unsafe.getChar(owner, fieldOffset));
            } else if (type == short.class) {
                checkOwner(owner, short.class, true, false);
                return rev(unsafe.getShort(owner, fieldOffset));
            } else if (type == int.class) {
                checkOwner(owner, int.class, true, false);
                return rev(unsafe.getInt(owner, fieldOffset));
            } else if (type == long.class) {
                checkOwner(owner, long.class, true, false);
                return rev(unsafe.getLong(owner, fieldOffset));
            } else if (type == float.class) {
                checkOwner(owner, float.class, true, false);
                return rev(unsafe.getFloat(owner, fieldOffset));
            } else if (type == double.class) {
                checkOwner(owner, double.class, true, false);
                return rev(unsafe.getDouble(owner, fieldOffset));
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
        return rev(unsafe.getChar(owner, fieldOffset));
    }

    public final short getS(Object owner) {
        checkOwner(owner, short.class, true, false);
        return rev(unsafe.getShort(owner, fieldOffset));
    }

    public final int getI(Object owner) {
        checkOwner(owner, int.class, true, false);
        return rev(unsafe.getInt(owner, fieldOffset));
    }

    public final long getJ(Object owner) {
        checkOwner(owner, long.class, true, false);
        return rev(unsafe.getLong(owner, fieldOffset));
    }

    public final float getF(Object owner) {
        checkOwner(owner, float.class, true, false);
        return rev(unsafe.getFloat(owner, fieldOffset));
    }

    public final double getD(Object owner) {
        checkOwner(owner, double.class, true, false);
        return rev(unsafe.getDouble(owner, fieldOffset));
    }

    public final Object get(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        if (byteArrayView && owner instanceof ByteBuffer) {
            if (!((ByteBuffer) owner).hasArray()) {
                throw PartialStubError.create();
            }
            owner = ((ByteBuffer) owner).array();
        }
        if (Object.class.isAssignableFrom(type)) {
            checkArray(owner, index, Object.class, true, false);
            return unsafe.getObject(owner, indexOffset(index));
        } else {
            if (type == boolean.class) {
                checkArray(owner, index, boolean.class, true, false);
                return unsafe.getBoolean(owner, indexOffset(index));
            } else if (type == byte.class) {
                checkArray(owner, index, byte.class, true, false);
                return unsafe.getByte(owner, indexOffset(index));
            } else if (type == char.class) {
                checkArray(owner, index, char.class, true, false);
                return rev(unsafe.getChar(owner, indexOffset(index)));
            } else if (type == short.class) {
                checkArray(owner, index, short.class, true, false);
                return rev(unsafe.getShort(owner, indexOffset(index)));
            } else if (type == int.class) {
                checkArray(owner, index, int.class, true, false);
                return rev(unsafe.getInt(owner, indexOffset(index)));
            } else if (type == long.class) {
                checkArray(owner, index, long.class, true, false);
                return rev(unsafe.getLong(owner, indexOffset(index)));
            } else if (type == float.class) {
                checkArray(owner, index, float.class, true, false);
                return rev(unsafe.getFloat(owner, indexOffset(index)));
            } else if (type == double.class) {
                checkArray(owner, index, double.class, true, false);
                return rev(unsafe.getDouble(owner, indexOffset(index)));
            }
            throw new WrongMethodTypeException();
        }
    }

    public final boolean getZ(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, true, false);
        return unsafe.getBoolean(owner, indexOffset(index));
    }

    public final byte getB(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, true, false);
        return unsafe.getByte(owner, indexOffset(index));
    }

    public final char getC(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, true, false);
        return rev(unsafe.getChar(owner, indexOffset(index)));
    }

    public final short getS(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, true, false);
        return rev(unsafe.getShort(owner, indexOffset(index)));
    }

    public final int getI(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, true, false);
        return rev(unsafe.getInt(owner, indexOffset(index)));
    }

    public final long getJ(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, true, false);
        return rev(unsafe.getLong(owner, indexOffset(index)));
    }

    public final float getF(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, float.class, true, false);
        return rev(unsafe.getFloat(owner, indexOffset(index)));
    }

    public final double getD(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, double.class, true, false);
        return rev(unsafe.getDouble(owner, indexOffset(index)));
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
        unsafe.putChar(staticFieldBase, fieldOffset, rev(value));
    }

    public final void set(short value) {
        checkStatic(short.class, false, true);
        unsafe.putShort(staticFieldBase, fieldOffset, rev(value));
    }

    public final void set(int value) {
        checkStatic(int.class, false, true);
        unsafe.putInt(staticFieldBase, fieldOffset, rev(value));
    }

    public final void set(long value) {
        checkStatic(long.class, false, true);
        unsafe.putLong(staticFieldBase, fieldOffset, rev(value));
    }

    public final void set(float value) {
        checkStatic(float.class, false, true);
        unsafe.putFloat(staticFieldBase, fieldOffset, rev(value));
    }

    public final void set(double value) {
        checkStatic(double.class, false, true);
        unsafe.putDouble(staticFieldBase, fieldOffset, rev(value));
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
        unsafe.putChar(owner, fieldOffset, rev(value));
    }

    public final void set(Object owner, short value) {
        checkOwner(owner, short.class, false, true);
        unsafe.putShort(owner, fieldOffset, rev(value));
    }

    public final void set(Object owner, int value) {
        checkOwner(owner, int.class, false, true);
        unsafe.putInt(owner, fieldOffset, rev(value));
    }

    public final void set(Object owner, long value) {
        checkOwner(owner, long.class, false, true);
        unsafe.putLong(owner, fieldOffset, rev(value));
    }

    public final void set(Object owner, float value) {
        checkOwner(owner, float.class, false, true);
        unsafe.putFloat(owner, fieldOffset, rev(value));
    }

    public final void set(Object owner, double value) {
        checkOwner(owner, double.class, false, true);
        unsafe.putDouble(owner, fieldOffset, rev(value));
    }

    public final void set(Object owner, int index, Object value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, value == null ? null : value.getClass(), false, true);
        unsafe.putObject(owner, indexOffset(index), value);
    }

    public final void set(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, false, true);
        unsafe.putBoolean(owner, indexOffset(index), value);
    }

    public final void set(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, false, true);
        unsafe.putByte(owner, indexOffset(index), value);
    }

    public final void set(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, false, true);
        unsafe.putChar(owner, indexOffset(index), rev(value));
    }

    public final void set(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, false, true);
        unsafe.putShort(owner, indexOffset(index), rev(value));
    }

    public final void set(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, false, true);
        unsafe.putInt(owner, indexOffset(index), rev(value));
    }

    public final void set(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, false, true);
        unsafe.putLong(owner, indexOffset(index), rev(value));
    }

    public final void set(Object owner, int index, float value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, float.class, false, true);
        unsafe.putFloat(owner, indexOffset(index), rev(value));
    }

    public final void set(Object owner, int index, double value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, double.class, false, true);
        unsafe.putDouble(owner, indexOffset(index), rev(value));
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
                return rev(unsafe.getCharVolatile(staticFieldBase, fieldOffset));
            } else if (type == short.class) {
                checkStatic(short.class, true, false);
                return rev(unsafe.getShortVolatile(staticFieldBase, fieldOffset));
            } else if (type == int.class) {
                checkStatic(int.class, true, false);
                return rev(unsafe.getIntVolatile(staticFieldBase, fieldOffset));
            } else if (type == long.class) {
                checkStatic(long.class, true, false);
                return rev(unsafe.getLongVolatile(staticFieldBase, fieldOffset));
            } else if (type == float.class) {
                checkStatic(float.class, true, false);
                return rev(unsafe.getFloatVolatile(staticFieldBase, fieldOffset));
            } else if (type == double.class) {
                checkStatic(double.class, true, false);
                return rev(unsafe.getDoubleVolatile(staticFieldBase, fieldOffset));
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
        return rev(unsafe.getCharVolatile(staticFieldBase, fieldOffset));
    }

    public final short getVolatileS() {
        checkStatic(short.class, true, false);
        return rev(unsafe.getShortVolatile(staticFieldBase, fieldOffset));
    }

    public final int getVolatileI() {
        checkStatic(int.class, true, false);
        return rev(unsafe.getIntVolatile(staticFieldBase, fieldOffset));
    }

    public final long getVolatileJ() {
        checkStatic(long.class, true, false);
        return rev(unsafe.getLongVolatile(staticFieldBase, fieldOffset));
    }

    public final float getVolatileF() {
        checkStatic(float.class, true, false);
        return rev(unsafe.getFloatVolatile(staticFieldBase, fieldOffset));
    }

    public final double getVolatileD() {
        checkStatic(double.class, true, false);
        return rev(unsafe.getDoubleVolatile(staticFieldBase, fieldOffset));
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
                return rev(unsafe.getCharVolatile(owner, fieldOffset));
            } else if (type == short.class) {
                checkOwner(owner, short.class, true, false);
                return rev(unsafe.getShortVolatile(owner, fieldOffset));
            } else if (type == int.class) {
                checkOwner(owner, int.class, true, false);
                return rev(unsafe.getIntVolatile(owner, fieldOffset));
            } else if (type == long.class) {
                checkOwner(owner, long.class, true, false);
                return rev(unsafe.getLongVolatile(owner, fieldOffset));
            } else if (type == float.class) {
                checkOwner(owner, float.class, true, false);
                return rev(unsafe.getFloatVolatile(owner, fieldOffset));
            } else if (type == double.class) {
                checkOwner(owner, double.class, true, false);
                return rev(unsafe.getDoubleVolatile(owner, fieldOffset));
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
        return rev(unsafe.getCharVolatile(owner, fieldOffset));
    }

    public final short getVolatileS(Object owner) {
        checkOwner(owner, short.class, true, false);
        return rev(unsafe.getShortVolatile(owner, fieldOffset));
    }

    public final int getVolatileI(Object owner) {
        checkOwner(owner, int.class, true, false);
        return rev(unsafe.getIntVolatile(owner, fieldOffset));
    }

    public final long getVolatileJ(Object owner) {
        checkOwner(owner, long.class, true, false);
        return rev(unsafe.getLongVolatile(owner, fieldOffset));
    }

    public final float getVolatileF(Object owner) {
        checkOwner(owner, float.class, true, false);
        return rev(unsafe.getFloatVolatile(owner, fieldOffset));
    }

    public final double getVolatileD(Object owner) {
        checkOwner(owner, double.class, true, false);
        return rev(unsafe.getDoubleVolatile(owner, fieldOffset));
    }

    public final Object getVolatile(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        if (Object.class.isAssignableFrom(type)) {
            checkArray(owner, index, Object.class, true, false);
            return unsafe.getObjectVolatile(owner, indexOffset(index));
        } else {
            if (type == boolean.class) {
                checkArray(owner, index, boolean.class, true, false);
                return unsafe.getBooleanVolatile(owner, indexOffset(index));
            } else if (type == byte.class) {
                checkArray(owner, index, byte.class, true, false);
                return unsafe.getByteVolatile(owner, indexOffset(index));
            } else if (type == char.class) {
                checkArray(owner, index, char.class, true, false);
                return rev(unsafe.getCharVolatile(owner, indexOffset(index)));
            } else if (type == short.class) {
                checkArray(owner, index, short.class, true, false);
                return rev(unsafe.getShortVolatile(owner, indexOffset(index)));
            } else if (type == int.class) {
                checkArray(owner, index, int.class, true, false);
                return rev(unsafe.getIntVolatile(owner, indexOffset(index)));
            } else if (type == long.class) {
                checkArray(owner, index, long.class, true, false);
                return rev(unsafe.getLongVolatile(owner, indexOffset(index)));
            } else if (type == float.class) {
                checkArray(owner, index, float.class, true, false);
                return rev(unsafe.getFloatVolatile(owner, indexOffset(index)));
            } else if (type == double.class) {
                checkArray(owner, index, double.class, true, false);
                return rev(unsafe.getDoubleVolatile(owner, indexOffset(index)));
            }
            throw new WrongMethodTypeException();
        }
    }

    public final boolean getVolatileZ(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, true, false);
        return unsafe.getBooleanVolatile(owner, indexOffset(index));
    }

    public final byte getVolatileB(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, true, false);
        return unsafe.getByteVolatile(owner, indexOffset(index));
    }

    public final char getVolatileC(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, true, false);
        return rev(unsafe.getCharVolatile(owner, indexOffset(index)));
    }

    public final short getVolatileS(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, true, false);
        return rev(unsafe.getShortVolatile(owner, indexOffset(index)));
    }

    public final int getVolatileI(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, true, false);
        return rev(unsafe.getIntVolatile(owner, indexOffset(index)));
    }

    public final long getVolatileJ(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, true, false);
        return rev(unsafe.getLongVolatile(owner, indexOffset(index)));
    }

    public final float getVolatileF(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, float.class, true, false);
        return rev(unsafe.getFloatVolatile(owner, indexOffset(index)));
    }

    public final double getVolatileD(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, double.class, true, false);
        return rev(unsafe.getDoubleVolatile(owner, indexOffset(index)));
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
        unsafe.putCharVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setVolatile(short value) {
        checkStatic(short.class, false, true);
        unsafe.putShortVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setVolatile(int value) {
        checkStatic(int.class, false, true);
        unsafe.putIntVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setVolatile(long value) {
        checkStatic(long.class, false, true);
        unsafe.putLongVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setVolatile(float value) {
        checkStatic(float.class, false, true);
        unsafe.putFloatVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setVolatile(double value) {
        checkStatic(double.class, false, true);
        unsafe.putDoubleVolatile(staticFieldBase, fieldOffset, rev(value));
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
        unsafe.putCharVolatile(owner, fieldOffset, rev(value));
    }

    public final void setVolatile(Object owner, short value) {
        checkOwner(owner, short.class, false, true);
        unsafe.putShortVolatile(owner, fieldOffset, rev(value));
    }

    public final void setVolatile(Object owner, int value) {
        checkOwner(owner, int.class, false, true);
        unsafe.putIntVolatile(owner, fieldOffset, rev(value));
    }

    public final void setVolatile(Object owner, long value) {
        checkOwner(owner, long.class, false, true);
        unsafe.putLongVolatile(owner, fieldOffset, rev(value));
    }

    public final void setVolatile(Object owner, float value) {
        checkOwner(owner, float.class, false, true);
        unsafe.putFloatVolatile(owner, fieldOffset, rev(value));
    }

    public final void setVolatile(Object owner, double value) {
        checkOwner(owner, double.class, false, true);
        unsafe.putDoubleVolatile(owner, fieldOffset, rev(value));
    }

    public final void setVolatile(Object owner, int index, Object value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, value == null ? null : value.getClass(), false, true);
        unsafe.putObjectVolatile(owner, indexOffset(index), value);
    }

    public final void setVolatile(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, false, true);
        unsafe.putBooleanVolatile(owner, indexOffset(index), value);
    }

    public final void setVolatile(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, false, true);
        unsafe.putByteVolatile(owner, indexOffset(index), value);
    }

    public final void setVolatile(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, false, true);
        unsafe.putCharVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setVolatile(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, false, true);
        unsafe.putShortVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setVolatile(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, false, true);
        unsafe.putIntVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setVolatile(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, false, true);
        unsafe.putLongVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setVolatile(Object owner, int index, float value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, float.class, false, true);
        unsafe.putFloatVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setVolatile(Object owner, int index, double value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, double.class, false, true);
        unsafe.putDoubleVolatile(owner, indexOffset(index), rev(value));
    }

    public final Object getOpaque() {
        return getVolatile();
    }

    public final boolean getOpaqueZ() {
        checkStatic(boolean.class, true, false);
        return unsafe.getBooleanVolatile(staticFieldBase, fieldOffset);
    }

    public final byte getOpaqueB() {
        checkStatic(byte.class, true, false);
        return unsafe.getByteVolatile(staticFieldBase, fieldOffset);
    }

    public final char getOpaqueC() {
        checkStatic(char.class, true, false);
        return rev(unsafe.getCharVolatile(staticFieldBase, fieldOffset));
    }

    public final short getOpaqueS() {
        checkStatic(short.class, true, false);
        return rev(unsafe.getShortVolatile(staticFieldBase, fieldOffset));
    }

    public final int getOpaqueI() {
        checkStatic(int.class, true, false);
        return rev(unsafe.getIntVolatile(staticFieldBase, fieldOffset));
    }

    public final long getOpaqueJ() {
        checkStatic(long.class, true, false);
        return rev(unsafe.getLongVolatile(staticFieldBase, fieldOffset));
    }

    public final float getOpaqueF() {
        checkStatic(float.class, true, false);
        return rev(unsafe.getFloatVolatile(staticFieldBase, fieldOffset));
    }

    public final double getOpaqueD() {
        checkStatic(double.class, true, false);
        return rev(unsafe.getDoubleVolatile(staticFieldBase, fieldOffset));
    }

    public final Object getOpaque(Object owner) {
        return getVolatile(owner);
    }

    public final boolean getOpaqueZ(Object owner) {
        checkOwner(owner, boolean.class, true, false);
        return unsafe.getBooleanVolatile(owner, fieldOffset);
    }

    public final byte getOpaqueB(Object owner) {
        checkOwner(owner, byte.class, true, false);
        return unsafe.getByteVolatile(owner, fieldOffset);
    }

    public final char getOpaqueC(Object owner) {
        checkOwner(owner, char.class, true, false);
        return rev(unsafe.getCharVolatile(owner, fieldOffset));
    }

    public final short getOpaqueS(Object owner) {
        checkOwner(owner, short.class, true, false);
        return rev(unsafe.getShortVolatile(owner, fieldOffset));
    }

    public final int getOpaqueI(Object owner) {
        checkOwner(owner, int.class, true, false);
        return rev(unsafe.getIntVolatile(owner, fieldOffset));
    }

    public final long getOpaqueJ(Object owner) {
        checkOwner(owner, long.class, true, false);
        return rev(unsafe.getLongVolatile(owner, fieldOffset));
    }

    public final float getOpaqueF(Object owner) {
        checkOwner(owner, float.class, true, false);
        return rev(unsafe.getFloatVolatile(owner, fieldOffset));
    }

    public final double getOpaqueD(Object owner) {
        checkOwner(owner, double.class, true, false);
        return rev(unsafe.getDoubleVolatile(owner, fieldOffset));
    }

    public final Object getOpaque(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getVolatile(owner, index);
    }

    public final boolean getOpaqueZ(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, true, false);
        return unsafe.getBooleanVolatile(owner, indexOffset(index));
    }

    public final byte getOpaqueB(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, true, false);
        return unsafe.getByteVolatile(owner, indexOffset(index));
    }

    public final char getOpaqueC(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, true, false);
        return rev(unsafe.getCharVolatile(owner, indexOffset(index)));
    }

    public final short getOpaqueS(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, true, false);
        return rev(unsafe.getShortVolatile(owner, indexOffset(index)));
    }

    public final int getOpaqueI(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, true, false);
        return rev(unsafe.getIntVolatile(owner, indexOffset(index)));
    }

    public final long getOpaqueJ(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, true, false);
        return rev(unsafe.getLongVolatile(owner, indexOffset(index)));
    }

    public final float getOpaqueF(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, float.class, true, false);
        return rev(unsafe.getFloatVolatile(owner, indexOffset(index)));
    }

    public final double getOpaqueD(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, double.class, true, false);
        return rev(unsafe.getDoubleVolatile(owner, indexOffset(index)));
    }

    public final void setOpaque(Object value) {
        checkStatic(value == null ? null : value.getClass(), false, true);
        unsafe.putObjectVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setOpaque(boolean value) {
        checkStatic(boolean.class, false, true);
        unsafe.putBooleanVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setOpaque(byte value) {
        checkStatic(byte.class, false, true);
        unsafe.putByteVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setOpaque(char value) {
        checkStatic(char.class, false, true);
        unsafe.putCharVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setOpaque(short value) {
        checkStatic(short.class, false, true);
        unsafe.putShortVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setOpaque(int value) {
        checkStatic(int.class, false, true);
        unsafe.putIntVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setOpaque(long value) {
        checkStatic(long.class, false, true);
        unsafe.putLongVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setOpaque(float value) {
        checkStatic(float.class, false, true);
        unsafe.putFloatVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setOpaque(double value) {
        checkStatic(double.class, false, true);
        unsafe.putDoubleVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setOpaque(Object owner, Object value) {
        checkOwner(owner, value == null ? null : value.getClass(), false, true);
        unsafe.putObjectVolatile(owner, fieldOffset, value);
    }

    public final void setOpaque(Object owner, boolean value) {
        checkOwner(owner, boolean.class, false, true);
        unsafe.putBooleanVolatile(owner, fieldOffset, value);
    }

    public final void setOpaque(Object owner, byte value) {
        checkOwner(owner, byte.class, false, true);
        unsafe.putByteVolatile(owner, fieldOffset, value);
    }

    public final void setOpaque(Object owner, char value) {
        checkOwner(owner, char.class, false, true);
        unsafe.putCharVolatile(owner, fieldOffset, rev(value));
    }

    public final void setOpaque(Object owner, short value) {
        checkOwner(owner, short.class, false, true);
        unsafe.putShortVolatile(owner, fieldOffset, rev(value));
    }

    public final void setOpaque(Object owner, int value) {
        checkOwner(owner, int.class, false, true);
        unsafe.putIntVolatile(owner, fieldOffset, rev(value));
    }

    public final void setOpaque(Object owner, long value) {
        checkOwner(owner, long.class, false, true);
        unsafe.putLongVolatile(owner, fieldOffset, rev(value));
    }

    public final void setOpaque(Object owner, float value) {
        checkOwner(owner, float.class, false, true);
        unsafe.putFloatVolatile(owner, fieldOffset, rev(value));
    }

    public final void setOpaque(Object owner, double value) {
        checkOwner(owner, double.class, false, true);
        unsafe.putDoubleVolatile(owner, fieldOffset, rev(value));
    }

    public final void setOpaque(Object owner, int index, Object value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, value == null ? null : value.getClass(), false, true);
        unsafe.putObjectVolatile(owner, indexOffset(index), value);
    }

    public final void setOpaque(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, false, true);
        unsafe.putBooleanVolatile(owner, indexOffset(index), value);
    }

    public final void setOpaque(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, false, true);
        unsafe.putByteVolatile(owner, indexOffset(index), value);
    }

    public final void setOpaque(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, false, true);
        unsafe.putCharVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setOpaque(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, false, true);
        unsafe.putShortVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setOpaque(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, false, true);
        unsafe.putIntVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setOpaque(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, false, true);
        unsafe.putLongVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setOpaque(Object owner, int index, float value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, float.class, false, true);
        unsafe.putFloatVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setOpaque(Object owner, int index, double value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, double.class, false, true);
        unsafe.putDoubleVolatile(owner, indexOffset(index), rev(value));
    }

    public final Object getAcquire() {
        checkStatic(Object.class, true, false);
        Object obj = getVolatile();
        return obj;
    }

    public final boolean getAcquireZ() {
        checkStatic(boolean.class, true, false);
        boolean v = unsafe.getBooleanVolatile(staticFieldBase, fieldOffset);
        return v;
    }

    public final byte getAcquireB() {
        checkStatic(byte.class, true, false);
        byte v = unsafe.getByteVolatile(staticFieldBase, fieldOffset);
        return v;
    }

    public final char getAcquireC() {
        checkStatic(char.class, true, false);
        char v = rev(unsafe.getCharVolatile(staticFieldBase, fieldOffset));
        return v;
    }

    public final short getAcquireS() {
        checkStatic(short.class, true, false);
        short v = rev(unsafe.getShortVolatile(staticFieldBase, fieldOffset));
        return v;
    }

    public final int getAcquireI() {
        checkStatic(int.class, true, false);
        int v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        return rev(v);
    }

    public final long getAcquireJ() {
        checkStatic(long.class, true, false);
        long v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
        return rev(v);
    }

    public final float getAcquireF() {
        checkStatic(float.class, true, false);
        float v = rev(unsafe.getFloatVolatile(staticFieldBase, fieldOffset));
        return v;
    }

    public final double getAcquireD() {
        checkStatic(double.class, true, false);
        double v = rev(unsafe.getDoubleVolatile(staticFieldBase, fieldOffset));
        return v;
    }

    public final Object getAcquire(Object owner) {
        checkOwner(owner, Object.class, true, false);
        Object obj = getVolatile(owner);
        return obj;
    }

    public final boolean getAcquireZ(Object owner) {
        checkOwner(owner, boolean.class, true, false);
        boolean v = unsafe.getBooleanVolatile(owner, fieldOffset);
        return v;
    }

    public final byte getAcquireB(Object owner) {
        checkOwner(owner, byte.class, true, false);
        byte v = unsafe.getByteVolatile(owner, fieldOffset);
        return v;
    }

    public final char getAcquireC(Object owner) {
        checkOwner(owner, char.class, true, false);
        char v = rev(unsafe.getCharVolatile(owner, fieldOffset));
        return v;
    }

    public final short getAcquireS(Object owner) {
        checkOwner(owner, short.class, true, false);
        short v = rev(unsafe.getShortVolatile(owner, fieldOffset));
        return v;
    }

    public final int getAcquireI(Object owner) {
        checkOwner(owner, int.class, true, false);
        int v = unsafe.getIntVolatile(owner, fieldOffset);
        return rev(v);
    }

    public final long getAcquireJ(Object owner) {
        checkOwner(owner, long.class, true, false);
        long v = unsafe.getLongVolatile(owner, fieldOffset);
        return rev(v);
    }

    public final float getAcquireF(Object owner) {
        checkOwner(owner, float.class, true, false);
        float v = rev(unsafe.getFloatVolatile(owner, fieldOffset));
        return v;
    }

    public final double getAcquireD(Object owner) {
        checkOwner(owner, double.class, true, false);
        double v = rev(unsafe.getDoubleVolatile(owner, fieldOffset));
        return v;
    }

    public final Object getAcquire(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, Object.class, true, false);
        Object obj = getVolatile(owner, index);
        return obj;
    }

    public final boolean getAcquireZ(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, true, false);
        boolean v = unsafe.getBooleanVolatile(owner, indexOffset(index));
        return v;
    }

    public final byte getAcquireB(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, true, false);
        byte v = unsafe.getByteVolatile(owner, indexOffset(index));
        return v;
    }

    public final char getAcquireC(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, true, false);
        char v = rev(unsafe.getCharVolatile(owner, indexOffset(index)));
        return v;
    }

    public final short getAcquireS(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, true, false);
        short v = rev(unsafe.getShortVolatile(owner, indexOffset(index)));
        return v;
    }

    public final int getAcquireI(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, true, false);
        int v = unsafe.getIntVolatile(owner, indexOffset(index));
        return rev(v);
    }

    public final long getAcquireJ(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, true, false);
        long v = unsafe.getLongVolatile(owner, indexOffset(index));
        return rev(v);
    }

    public final float getAcquireF(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, float.class, true, false);
        float v = rev(unsafe.getFloatVolatile(owner, indexOffset(index)));
        return v;
    }

    public final double getAcquireD(Object owner, int index) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, double.class, true, false);
        double v = rev(unsafe.getDoubleVolatile(owner, indexOffset(index)));
        return v;
    }

    public final void setRelease(Object value) {
        checkStatic(value == null ? null : value.getClass(), false, true);
        unsafe.putObjectVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setRelease(boolean value) {
        checkStatic(boolean.class, false, true);
        unsafe.putBooleanVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setRelease(byte value) {
        checkStatic(byte.class, false, true);
        unsafe.putByteVolatile(staticFieldBase, fieldOffset, value);
    }

    public final void setRelease(char value) {
        checkStatic(char.class, false, true);
        unsafe.putCharVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setRelease(short value) {
        checkStatic(short.class, false, true);
        unsafe.putShortVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setRelease(int value) {
        checkStatic(int.class, false, true);
        unsafe.putIntVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setRelease(long value) {
        checkStatic(long.class, false, true);
        unsafe.putLongVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setRelease(float value) {
        checkStatic(float.class, false, true);
        unsafe.putFloatVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setRelease(double value) {
        checkStatic(double.class, false, true);
        unsafe.putDoubleVolatile(staticFieldBase, fieldOffset, rev(value));
    }

    public final void setRelease(Object owner, Object value) {
        checkOwner(owner, value == null ? null : value.getClass(), false, true);
        unsafe.putObjectVolatile(owner, fieldOffset, value);
    }

    public final void setRelease(Object owner, boolean value) {
        checkOwner(owner, boolean.class, false, true);
        unsafe.putBooleanVolatile(owner, fieldOffset, value);
    }

    public final void setRelease(Object owner, byte value) {
        checkOwner(owner, byte.class, false, true);
        unsafe.putByteVolatile(owner, fieldOffset, value);
    }

    public final void setRelease(Object owner, char value) {
        checkOwner(owner, char.class, false, true);
        unsafe.putCharVolatile(owner, fieldOffset, rev(value));
    }

    public final void setRelease(Object owner, short value) {
        checkOwner(owner, short.class, false, true);
        unsafe.putShortVolatile(owner, fieldOffset, rev(value));
    }

    public final void setRelease(Object owner, int value) {
        checkOwner(owner, int.class, false, true);
        unsafe.putIntVolatile(owner, fieldOffset, rev(value));
    }

    public final void setRelease(Object owner, long value) {
        checkOwner(owner, long.class, false, true);
        unsafe.putLongVolatile(owner, fieldOffset, rev(value));
    }

    public final void setRelease(Object owner, float value) {
        checkOwner(owner, float.class, false, true);
        unsafe.putFloatVolatile(owner, fieldOffset, rev(value));
    }

    public final void setRelease(Object owner, double value) {
        checkOwner(owner, double.class, false, true);
        unsafe.putDoubleVolatile(owner, fieldOffset, rev(value));
    }

    public final void setRelease(Object owner, int index, Object value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, value == null ? null : value.getClass(), false, true);
        unsafe.putObjectVolatile(owner, indexOffset(index), value);
    }

    public final void setRelease(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, false, true);
        unsafe.putBooleanVolatile(owner, indexOffset(index), value);
    }

    public final void setRelease(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, false, true);
        unsafe.putByteVolatile(owner, indexOffset(index), value);
    }

    public final void setRelease(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, false, true);
        unsafe.putCharVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setRelease(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, false, true);
        unsafe.putShortVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setRelease(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, false, true);
        unsafe.putIntVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setRelease(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, false, true);
        unsafe.putLongVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setRelease(Object owner, int index, float value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, float.class, false, true);
        unsafe.putFloatVolatile(owner, indexOffset(index), rev(value));
    }

    public final void setRelease(Object owner, int index, double value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, double.class, false, true);
        unsafe.putDoubleVolatile(owner, indexOffset(index), rev(value));
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
            if (rev((char) i) != expected) return false;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, i, rev(newValue)));
        return true;
    }

    public final boolean compareAndSet(short expected, short newValue) {
        checkStatic(short.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if (rev((short) i) != expected) return false;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, i, rev(newValue)));
        return true;
    }

    public final boolean compareAndSet(int expected, int newValue) {
        checkStatic(int.class, false, true);
        return unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, rev(expected), rev(newValue));
    }

    public final boolean compareAndSet(long expected, long newValue) {
        checkStatic(long.class, false, true);
        return unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, rev(expected), rev(newValue));
    }

    public final boolean compareAndSet(float expected, float newValue) {
        checkStatic(float.class, false, true);
        return unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, Float.floatToRawIntBits(rev(expected)), Float.floatToRawIntBits(rev(newValue)));
    }

    public final boolean compareAndSet(double expected, double newValue) {
        checkStatic(double.class, false, true);
        return unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, Double.doubleToRawLongBits(rev(expected)), Double.doubleToRawLongBits(rev(newValue)));
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
            if (rev((char) i) != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, i, rev(newValue)));
        return true;
    }

    public final boolean compareAndSet(Object owner, short expected, short newValue) {
        checkOwner(owner, short.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, fieldOffset);
            if (rev((short) i) != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, i, rev(newValue)));
        return true;
    }

    public final boolean compareAndSet(Object owner, int expected, int newValue) {
        checkOwner(owner, int.class, false, true);
        return unsafe.compareAndSwapInt(owner, fieldOffset, rev(expected), rev(newValue));
    }

    public final boolean compareAndSet(Object owner, long expected, long newValue) {
        checkOwner(owner, long.class, false, true);
        return unsafe.compareAndSwapLong(owner, fieldOffset, rev(expected), rev(newValue));
    }

    public final boolean compareAndSet(Object owner, float expected, float newValue) {
        checkOwner(owner, float.class, false, true);
        return unsafe.compareAndSwapInt(owner, fieldOffset, Float.floatToRawIntBits(rev(expected)), Float.floatToRawIntBits(rev(newValue)));
    }

    public final boolean compareAndSet(Object owner, double expected, double newValue) {
        checkOwner(owner, double.class, false, true);
        return unsafe.compareAndSwapLong(owner, fieldOffset, Double.doubleToRawLongBits(rev(expected)), Double.doubleToRawLongBits(rev(newValue)));
    }

    public final boolean compareAndSet(Object owner, int index, Object expected, Object newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, Object.class, false, true);
        return unsafe.compareAndSwapObject(owner, indexOffset(index), expected, newValue);
    }

    public final boolean compareAndSet(Object owner, int index, boolean expected, boolean newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, indexOffset(index));
            if (((byte) i != 0) != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), i, newValue ? 1 : 0));
        return true;
    }

    public final boolean compareAndSet(Object owner, int index, byte expected, byte newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, indexOffset(index));
            if ((byte) i != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), i, newValue));
        return true;
    }

    public final boolean compareAndSet(Object owner, int index, char expected, char newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, indexOffset(index));
            if (rev((char) i) != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), i, rev(newValue)));
        return true;
    }

    public final boolean compareAndSet(Object owner, int index, short expected, short newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, false, true);
        int i;
        do {
            i = unsafe.getIntVolatile(owner, indexOffset(index));
            if (rev((short) i) != expected) return false;
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), i, rev(newValue)));
        return true;
    }

    public final boolean compareAndSet(Object owner, int index, int expected, int newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, false, true);
        return unsafe.compareAndSwapInt(owner, indexOffset(index), rev(expected), rev(newValue));
    }

    public final boolean compareAndSet(Object owner, int index, long expected, long newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, false, true);
        return unsafe.compareAndSwapLong(owner, indexOffset(index), rev(expected), rev(newValue));
    }

    public final boolean compareAndSet(Object owner, int index, float expected, float newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, float.class, false, true);
        return unsafe.compareAndSwapInt(owner, indexOffset(index), Float.floatToRawIntBits(rev(expected)), Float.floatToRawIntBits(rev(newValue)));
    }

    public final boolean compareAndSet(Object owner, int index, double expected, double newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, double.class, false, true);
        return unsafe.compareAndSwapLong(owner, indexOffset(index), Double.doubleToRawLongBits(rev(expected)), Double.doubleToRawLongBits(rev(newValue)));
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
            if (rev((char) v) != expected) return rev((char) v);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev(newValue)));
        return expected;
    }

    public final short compareAndExchange(short expected, short newValue) {
        checkStatic(short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if (rev((short) v) != expected) return rev((short) v);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev(newValue)));
        return expected;
    }

    public final int compareAndExchange(int expected, int newValue) {
        checkStatic(int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            if (rev(v) != expected) return rev(v);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev(newValue)));
        return expected;
    }

    public final long compareAndExchange(long expected, long newValue) {
        checkStatic(long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
            if (rev(v) != expected) return rev(v);
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, rev(newValue)));
        return expected;
    }

    public final float compareAndExchange(float expected, float newValue) {
        checkStatic(float.class, true, true);
        float v;
        do {
            v = rev(unsafe.getFloatVolatile(staticFieldBase, fieldOffset));
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, Float.floatToRawIntBits(rev(v)), Float.floatToRawIntBits(rev(newValue))));
        return expected;
    }

    public final double compareAndExchange(double expected, double newValue) {
        checkStatic(double.class, true, true);
        double v;
        do {
            v = rev(unsafe.getDoubleVolatile(staticFieldBase, fieldOffset));
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, Double.doubleToRawLongBits(rev(v)), Double.doubleToRawLongBits(rev(newValue))));
        return expected;
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
            if (rev((char) v) != expected) return rev((char) v);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev(newValue)));
        return expected;
    }

    public final short compareAndExchange(Object owner, short expected, short newValue) {
        checkOwner(owner, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
            if (rev((short) v) != expected) return rev((short) v);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev(newValue)));
        return expected;
    }

    public final int compareAndExchange(Object owner, int expected, int newValue) {
        checkOwner(owner, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
            if (rev(v) != expected) return rev(v);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev(newValue)));
        return expected;
    }

    public final long compareAndExchange(Object owner, long expected, long newValue) {
        checkOwner(owner, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset);
            if (rev(v) != expected) return rev(v);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, rev(newValue)));
        return expected;
    }

    public final float compareAndExchange(Object owner, float expected, float newValue) {
        checkOwner(owner, float.class, true, true);
        float v;
        do {
            v = rev(unsafe.getFloatVolatile(owner, fieldOffset));
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, Float.floatToRawIntBits(rev(v)), Float.floatToRawIntBits(rev(newValue))));
        return expected;
    }

    public final double compareAndExchange(Object owner, double expected, double newValue) {
        checkOwner(owner, double.class, true, true);
        double v;
        do {
            v = rev(unsafe.getDoubleVolatile(owner, fieldOffset));
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, Double.doubleToRawLongBits(rev(v)), Double.doubleToRawLongBits(rev(newValue))));
        return expected;
    }

    public final Object compareAndExchange(Object owner, int index, Object expected, Object newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, Object.class, true, true);
        Object v;
        do {
            v = unsafe.getObjectVolatile(owner, indexOffset(index));
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapObject(owner, indexOffset(index), v, newValue));
        return expected;
    }

    public final boolean compareAndExchange(Object owner, int index, boolean expected, boolean newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
            if (((byte) v != 0) != expected) return (byte) v != 0;
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, newValue ? 1 : 0));
        return expected;
    }

    public final byte compareAndExchange(Object owner, int index, byte expected, byte newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
            if ((byte) v != expected) return (byte) v;
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, newValue));
        return expected;
    }

    public final char compareAndExchange(Object owner, int index, char expected, char newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
            if (rev((char) v) != expected) return rev((char) v);
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev(newValue)));
        return expected;
    }

    public final short compareAndExchange(Object owner, int index, short expected, short newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
            if (rev((short) v) != expected) return rev((short) v);
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev(newValue)));
        return expected;
    }

    public final int compareAndExchange(Object owner, int index, int expected, int newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
            if (rev(v) != expected) return rev(v);
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev(newValue)));
        return expected;
    }

    public final long compareAndExchange(Object owner, int index, long expected, long newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, indexOffset(index));
            if (rev(v) != expected) return rev(v);
        } while (!unsafe.compareAndSwapLong(owner, indexOffset(index), v, rev(newValue)));
        return expected;
    }

    public final float compareAndExchange(Object owner, int index, float expected, float newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, float.class, true, true);
        float v;
        do {
            v = rev(unsafe.getFloatVolatile(owner, indexOffset(index)));
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), Float.floatToRawIntBits(rev(v)), Float.floatToRawIntBits(rev(newValue))));
        return expected;
    }

    public final double compareAndExchange(Object owner, int index, double expected, double newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, double.class, true, true);
        double v;
        do {
            v = rev(unsafe.getDoubleVolatile(owner, indexOffset(index)));
            if (v != expected) return v;
        } while (!unsafe.compareAndSwapLong(owner, indexOffset(index), Double.doubleToRawLongBits(rev(v)), Double.doubleToRawLongBits(rev(newValue))));
        return expected;
    }

    public final Object compareAndExchangeAcquire(Object expected, Object newValue) {
        Object v = compareAndExchange(expected, newValue);
        return v;
    }

    public final boolean compareAndExchangeAcquire(boolean expected, boolean newValue) {
        boolean v = compareAndExchange(expected, newValue);
        return v;
    }

    public final byte compareAndExchangeAcquire(byte expected, byte newValue) {
        byte v = compareAndExchange(expected, newValue);
        return v;
    }

    public final char compareAndExchangeAcquire(char expected, char newValue) {
        char v = compareAndExchange(expected, newValue);
        return v;
    }

    public final short compareAndExchangeAcquire(short expected, short newValue) {
        short v = compareAndExchange(expected, newValue);
        return v;
    }

    public final int compareAndExchangeAcquire(int expected, int newValue) {
        int v = compareAndExchange(expected, newValue);
        return rev(v);
    }

    public final long compareAndExchangeAcquire(long expected, long newValue) {
        long v = compareAndExchange(expected, newValue);
        return rev(v);
    }

    public final float compareAndExchangeAcquire(float expected, float newValue) {
        float v = compareAndExchange(expected, newValue);
        return v;
    }

    public final double compareAndExchangeAcquire(double expected, double newValue) {
        double v = compareAndExchange(expected, newValue);
        return v;
    }

    public final Object compareAndExchangeAcquire(Object owner, Object expected, Object newValue) {
        Object v = compareAndExchange(owner, expected, newValue);
        return v;
    }

    public final boolean compareAndExchangeAcquire(Object owner, boolean expected, boolean newValue) {
        boolean v = compareAndExchange(owner, expected, newValue);
        return v;
    }

    public final byte compareAndExchangeAcquire(Object owner, byte expected, byte newValue) {
        byte v = compareAndExchange(owner, expected, newValue);
        return v;
    }

    public final char compareAndExchangeAcquire(Object owner, char expected, char newValue) {
        char v = compareAndExchange(owner, expected, newValue);
        return v;
    }

    public final short compareAndExchangeAcquire(Object owner, short expected, short newValue) {
        short v = compareAndExchange(owner, expected, newValue);
        return v;
    }

    public final int compareAndExchangeAcquire(Object owner, int expected, int newValue) {
        int v = compareAndExchange(owner, expected, newValue);
        return rev(v);
    }

    public final long compareAndExchangeAcquire(Object owner, long expected, long newValue) {
        long v = compareAndExchange(owner, expected, newValue);
        return rev(v);
    }

    public final float compareAndExchangeAcquire(Object owner, float expected, float newValue) {
        float v = compareAndExchange(owner, expected, newValue);
        return v;
    }

    public final double compareAndExchangeAcquire(Object owner, double expected, double newValue) {
        double v = compareAndExchange(owner, expected, newValue);
        return v;
    }

    public final Object compareAndExchangeAcquire(Object owner, int index, Object expected, Object newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        Object v = compareAndExchange(owner, index, expected, newValue);
        return v;
    }

    public final boolean compareAndExchangeAcquire(Object owner, int index, boolean expected, boolean newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean v = compareAndExchange(owner, index, expected, newValue);
        return v;
    }

    public final byte compareAndExchangeAcquire(Object owner, int index, byte expected, byte newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        byte v = compareAndExchange(owner, index, expected, newValue);
        return v;
    }

    public final char compareAndExchangeAcquire(Object owner, int index, char expected, char newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        char v = compareAndExchange(owner, index, expected, newValue);
        return v;
    }

    public final short compareAndExchangeAcquire(Object owner, int index, short expected, short newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        short v = compareAndExchange(owner, index, expected, newValue);
        return v;
    }

    public final int compareAndExchangeAcquire(Object owner, int index, int expected, int newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        int v = compareAndExchange(owner, index, expected, newValue);
        return rev(v);
    }

    public final long compareAndExchangeAcquire(Object owner, int index, long expected, long newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        long v = compareAndExchange(owner, index, expected, newValue);
        return rev(v);
    }

    public final float compareAndExchangeAcquire(Object owner, int index, float expected, float newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        float v = compareAndExchange(owner, index, expected, newValue);
        return v;
    }

    public final double compareAndExchangeAcquire(Object owner, int index, double expected, double newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        double v = compareAndExchange(owner, index, expected, newValue);
        return v;
    }

    public final Object compareAndExchangeRelease(Object expected, Object newValue) {
        return compareAndExchange(expected, newValue);
    }

    public final boolean compareAndExchangeRelease(boolean expected, boolean newValue) {
        return compareAndExchange(expected, newValue);
    }

    public final byte compareAndExchangeRelease(byte expected, byte newValue) {
        return compareAndExchange(expected, newValue);
    }

    public final char compareAndExchangeRelease(char expected, char newValue) {
        return compareAndExchange(expected, newValue);
    }

    public final short compareAndExchangeRelease(short expected, short newValue) {
        return compareAndExchange(expected, newValue);
    }

    public final int compareAndExchangeRelease(int expected, int newValue) {
        return compareAndExchange(expected, newValue);
    }

    public final long compareAndExchangeRelease(long expected, long newValue) {
        return compareAndExchange(expected, newValue);
    }

    public final float compareAndExchangeRelease(float expected, float newValue) {
        return compareAndExchange(expected, newValue);
    }

    public final double compareAndExchangeRelease(double expected, double newValue) {
        return compareAndExchange(expected, newValue);
    }

    public final Object compareAndExchangeRelease(Object owner, Object expected, Object newValue) {
        return compareAndExchange(owner, expected, newValue);
    }

    public final boolean compareAndExchangeRelease(Object owner, boolean expected, boolean newValue) {
        return compareAndExchange(owner, expected, newValue);
    }

    public final byte compareAndExchangeRelease(Object owner, byte expected, byte newValue) {
        return compareAndExchange(owner, expected, newValue);
    }

    public final char compareAndExchangeRelease(Object owner, char expected, char newValue) {
        return compareAndExchange(owner, expected, newValue);
    }

    public final short compareAndExchangeRelease(Object owner, short expected, short newValue) {
        return compareAndExchange(owner, expected, newValue);
    }

    public final int compareAndExchangeRelease(Object owner, int expected, int newValue) {
        return compareAndExchange(owner, expected, newValue);
    }

    public final long compareAndExchangeRelease(Object owner, long expected, long newValue) {
        return compareAndExchange(owner, expected, newValue);
    }

    public final float compareAndExchangeRelease(Object owner, float expected, float newValue) {
        return compareAndExchange(owner, expected, newValue);
    }

    public final double compareAndExchangeRelease(Object owner, double expected, double newValue) {
        return compareAndExchange(owner, expected, newValue);
    }

    public final Object compareAndExchangeRelease(Object owner, int index, Object expected, Object newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndExchange(owner, index, expected, newValue);
    }

    public final boolean compareAndExchangeRelease(Object owner, int index, boolean expected, boolean newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndExchange(owner, index, expected, newValue);
    }

    public final byte compareAndExchangeRelease(Object owner, int index, byte expected, byte newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndExchange(owner, index, expected, newValue);
    }

    public final char compareAndExchangeRelease(Object owner, int index, char expected, char newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndExchange(owner, index, expected, newValue);
    }

    public final short compareAndExchangeRelease(Object owner, int index, short expected, short newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndExchange(owner, index, expected, newValue);
    }

    public final int compareAndExchangeRelease(Object owner, int index, int expected, int newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndExchange(owner, index, expected, newValue);
    }

    public final long compareAndExchangeRelease(Object owner, int index, long expected, long newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndExchange(owner, index, expected, newValue);
    }

    public final float compareAndExchangeRelease(Object owner, int index, float expected, float newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndExchange(owner, index, expected, newValue);
    }

    public final double compareAndExchangeRelease(Object owner, int index, double expected, double newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

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
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, boolean expected, boolean newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, byte expected, byte newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, char expected, char newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, short expected, short newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, int expected, int newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, long expected, long newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, float expected, float newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetPlain(Object owner, int index, double expected, double newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

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
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, boolean expected, boolean newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, byte expected, byte newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, char expected, char newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, short expected, short newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, int expected, int newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, long expected, long newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, float expected, float newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSet(Object owner, int index, double expected, double newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetAcquire(Object expected, Object newValue) {
        boolean b = compareAndSet(expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(boolean expected, boolean newValue) {
        boolean b = compareAndSet(expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(byte expected, byte newValue) {
        boolean b = compareAndSet(expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(char expected, char newValue) {
        boolean b = compareAndSet(expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(short expected, short newValue) {
        boolean b = compareAndSet(expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(int expected, int newValue) {
        boolean b = compareAndSet(expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(long expected, long newValue) {
        boolean b = compareAndSet(expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(float expected, float newValue) {
        boolean b = compareAndSet(expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(double expected, double newValue) {
        boolean b = compareAndSet(expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, Object expected, Object newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, boolean expected, boolean newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, byte expected, byte newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, char expected, char newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, short expected, short newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int expected, int newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, long expected, long newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, float expected, float newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, double expected, double newValue) {
        boolean b = compareAndSet(owner, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, Object expected, Object newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean b = compareAndSet(owner, index, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, boolean expected, boolean newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean b = compareAndSet(owner, index, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, byte expected, byte newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean b = compareAndSet(owner, index, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, char expected, char newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean b = compareAndSet(owner, index, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, short expected, short newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean b = compareAndSet(owner, index, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, int expected, int newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean b = compareAndSet(owner, index, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, long expected, long newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean b = compareAndSet(owner, index, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, float expected, float newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean b = compareAndSet(owner, index, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetAcquire(Object owner, int index, double expected, double newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean b = compareAndSet(owner, index, expected, newValue);
        return b;
    }

    public final boolean weakCompareAndSetRelease(Object expected, Object newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(boolean expected, boolean newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(byte expected, byte newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(char expected, char newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(short expected, short newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(int expected, int newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(long expected, long newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(float expected, float newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(double expected, double newValue) {
        return compareAndSet(expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, Object expected, Object newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, boolean expected, boolean newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, byte expected, byte newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, char expected, char newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, short expected, short newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int expected, int newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, long expected, long newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, float expected, float newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, double expected, double newValue) {
        return compareAndSet(owner, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, Object expected, Object newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, boolean expected, boolean newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, byte expected, byte newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, char expected, char newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, short expected, short newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, int expected, int newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, long expected, long newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, float expected, float newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
    }

    public final boolean weakCompareAndSetRelease(Object owner, int index, double expected, double newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return compareAndSet(owner, index, expected, newValue);
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
        return rev((char) unsafe.getAndSetInt(staticFieldBase, fieldOffset, rev(newValue)));
    }

    public final short getAndSet(short newValue) {
        checkStatic(short.class, true, true);
        return rev((short) unsafe.getAndSetInt(staticFieldBase, fieldOffset, rev(newValue)));
    }

    public final int getAndSet(int newValue) {
        checkStatic(int.class, true, true);
        return rev(unsafe.getAndSetInt(staticFieldBase, fieldOffset, rev(newValue)));
    }

    public final long getAndSet(long newValue) {
        checkStatic(long.class, true, true);
        return rev(unsafe.getAndSetLong(staticFieldBase, fieldOffset, rev(newValue)));
    }

    public final float getAndSet(float newValue) {
        checkStatic(float.class, true, true);
        return Float.intBitsToFloat(rev(unsafe.getAndSetInt(staticFieldBase, fieldOffset, Float.floatToRawIntBits(rev(newValue)))));
    }

    public final double getAndSet(double newValue) {
        checkStatic(double.class, true, true);
        return Double.longBitsToDouble(rev(unsafe.getAndSetLong(staticFieldBase, fieldOffset, Double.doubleToRawLongBits(rev(newValue)))));
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
        return rev((char) unsafe.getAndSetInt(owner, fieldOffset, rev(newValue)));
    }

    public final short getAndSet(Object owner, short newValue) {
        checkOwner(owner, short.class, true, true);
        return rev((short) unsafe.getAndSetInt(owner, fieldOffset, rev(newValue)));
    }

    public final int getAndSet(Object owner, int newValue) {
        checkOwner(owner, int.class, true, true);
        return rev(unsafe.getAndSetInt(owner, fieldOffset, rev(newValue)));
    }

    public final long getAndSet(Object owner, long newValue) {
        checkOwner(owner, long.class, true, true);
        return rev(unsafe.getAndSetLong(owner, fieldOffset, rev(newValue)));
    }

    public final float getAndSet(Object owner, float newValue) {
        checkOwner(owner, float.class, true, true);
        return Float.intBitsToFloat(rev(unsafe.getAndSetInt(owner, fieldOffset, Float.floatToRawIntBits(rev(newValue)))));
    }

    public final double getAndSet(Object owner, double newValue) {
        checkOwner(owner, double.class, true, true);
        return Double.longBitsToDouble(rev(unsafe.getAndSetLong(owner, fieldOffset, Double.doubleToRawLongBits(rev(newValue)))));
    }

    public final Object getAndSet(Object owner, int index, Object newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, newValue == null ? null : newValue.getClass(), false, true);
        return unsafe.getAndSetObject(owner, indexOffset(index), index);
    }

    public final boolean getAndSet(Object owner, int index, boolean newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, true, true);
        return (byte) unsafe.getAndSetInt(owner, indexOffset(index), newValue ? 1 : 0) != 0;
    }

    public final byte getAndSet(Object owner, int index, byte newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, true, true);
        return (byte) unsafe.getAndSetInt(owner, indexOffset(index), newValue);
    }

    public final char getAndSet(Object owner, int index, char newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, true, true);
        return rev((char) unsafe.getAndSetInt(owner, indexOffset(index), rev(newValue)));
    }

    public final short getAndSet(Object owner, int index, short newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, true, true);
        return rev((short) unsafe.getAndSetInt(owner, indexOffset(index), rev(newValue)));
    }

    public final int getAndSet(Object owner, int index, int newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, true, true);
        return rev(unsafe.getAndSetInt(owner, indexOffset(index), rev(newValue)));
    }

    public final long getAndSet(Object owner, int index, long newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, true, true);
        return rev(unsafe.getAndSetLong(owner, indexOffset(index), rev(newValue)));
    }

    public final float getAndSet(Object owner, int index, float newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, float.class, true, true);
        return Float.intBitsToFloat(rev(unsafe.getAndSetInt(owner, indexOffset(index), Float.floatToRawIntBits(rev(newValue)))));
    }

    public final double getAndSet(Object owner, int index, double newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, double.class, true, true);
        return Double.longBitsToDouble(rev(unsafe.getAndSetLong(owner, indexOffset(index), Double.doubleToRawLongBits(rev(newValue)))));
    }

    public final Object getAndSetAcquire(Object newValue) {
        Object v = getAndSet(newValue);
        return v;
    }

    public final boolean getAndSetAcquire(boolean newValue) {
        boolean v = getAndSet(newValue);
        return v;
    }

    public final byte getAndSetAcquire(byte newValue) {
        byte v = getAndSet(newValue);
        return v;
    }

    public final char getAndSetAcquire(char newValue) {
        char v = getAndSet(newValue);
        return v;
    }

    public final short getAndSetAcquire(short newValue) {
        short v = getAndSet(newValue);
        return v;
    }

    public final int getAndSetAcquire(int newValue) {
        int v = getAndSet(newValue);
        return rev(v);
    }

    public final long getAndSetAcquire(long newValue) {
        long v = getAndSet(newValue);
        return rev(v);
    }

    public final float getAndSetAcquire(float newValue) {
        float v = getAndSet(newValue);
        return v;
    }

    public final double getAndSetAcquire(double newValue) {
        double v = getAndSet(newValue);
        return v;
    }

    public final Object getAndSetAcquire(Object owner, Object newValue) {
        Object v = getAndSet(owner, newValue);
        return v;
    }

    public final boolean getAndSetAcquire(Object owner, boolean newValue) {
        boolean v = getAndSet(owner, newValue);
        return v;
    }

    public final byte getAndSetAcquire(Object owner, byte newValue) {
        byte v = getAndSet(owner, newValue);
        return v;
    }

    public final char getAndSetAcquire(Object owner, char newValue) {
        char v = getAndSet(owner, newValue);
        return v;
    }

    public final short getAndSetAcquire(Object owner, short newValue) {
        short v = getAndSet(owner, newValue);
        return v;
    }

    public final int getAndSetAcquire(Object owner, int newValue) {
        int v = getAndSet(owner, newValue);
        return rev(v);
    }

    public final long getAndSetAcquire(Object owner, long newValue) {
        long v = getAndSet(owner, newValue);
        return rev(v);
    }

    public final float getAndSetAcquire(Object owner, float newValue) {
        float v = getAndSet(owner, newValue);
        return v;
    }

    public final double getAndSetAcquire(Object owner, double newValue) {
        double v = getAndSet(owner, newValue);
        return v;
    }

    public final Object getAndSetAcquire(Object owner, int index, Object newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        Object v = getAndSet(owner, index, newValue);
        return v;
    }

    public final boolean getAndSetAcquire(Object owner, int index, boolean newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean v = getAndSet(owner, index, newValue);
        return v;
    }

    public final byte getAndSetAcquire(Object owner, int index, byte newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        byte v = getAndSet(owner, index, newValue);
        return v;
    }

    public final char getAndSetAcquire(Object owner, int index, char newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        char v = getAndSet(owner, index, newValue);
        return v;
    }

    public final short getAndSetAcquire(Object owner, int index, short newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        short v = getAndSet(owner, index, newValue);
        return v;
    }

    public final int getAndSetAcquire(Object owner, int index, int newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        int v = getAndSet(owner, index, newValue);
        return rev(v);
    }

    public final long getAndSetAcquire(Object owner, int index, long newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        long v = getAndSet(owner, index, newValue);
        return rev(v);
    }

    public final float getAndSetAcquire(Object owner, int index, float newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        float v = getAndSet(owner, index, newValue);
        return v;
    }

    public final double getAndSetAcquire(Object owner, int index, double newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        double v = getAndSet(owner, index, newValue);
        return v;
    }

    public final Object getAndSetRelease(Object newValue) {
        return getAndSet(newValue);
    }

    public final boolean getAndSetRelease(boolean newValue) {
        return getAndSet(newValue);
    }

    public final byte getAndSetRelease(byte newValue) {
        return getAndSet(newValue);
    }

    public final char getAndSetRelease(char newValue) {
        return getAndSet(newValue);
    }

    public final short getAndSetRelease(short newValue) {
        return getAndSet(newValue);
    }

    public final int getAndSetRelease(int newValue) {
        return getAndSet(newValue);
    }

    public final long getAndSetRelease(long newValue) {
        return getAndSet(newValue);
    }

    public final float getAndSetRelease(float newValue) {
        return getAndSet(newValue);
    }

    public final double getAndSetRelease(double newValue) {
        return getAndSet(newValue);
    }

    public final Object getAndSetRelease(Object owner, Object newValue) {
        return getAndSet(owner, newValue);
    }

    public final boolean getAndSetRelease(Object owner, boolean newValue) {
        return getAndSet(owner, newValue);
    }

    public final byte getAndSetRelease(Object owner, byte newValue) {
        return getAndSet(owner, newValue);
    }

    public final char getAndSetRelease(Object owner, char newValue) {
        return getAndSet(owner, newValue);
    }

    public final short getAndSetRelease(Object owner, short newValue) {
        return getAndSet(owner, newValue);
    }

    public final int getAndSetRelease(Object owner, int newValue) {
        return getAndSet(owner, newValue);
    }

    public final long getAndSetRelease(Object owner, long newValue) {
        return getAndSet(owner, newValue);
    }

    public final float getAndSetRelease(Object owner, float newValue) {
        return getAndSet(owner, newValue);
    }

    public final double getAndSetRelease(Object owner, double newValue) {
        return getAndSet(owner, newValue);
    }

    public final Object getAndSetRelease(Object owner, int index, Object newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndSet(owner, index, newValue);
    }

    public final boolean getAndSetRelease(Object owner, int index, boolean newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndSet(owner, index, newValue);
    }

    public final byte getAndSetRelease(Object owner, int index, byte newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndSet(owner, index, newValue);
    }

    public final char getAndSetRelease(Object owner, int index, char newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndSet(owner, index, newValue);
    }

    public final short getAndSetRelease(Object owner, int index, short newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndSet(owner, index, newValue);
    }

    public final int getAndSetRelease(Object owner, int index, int newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndSet(owner, index, newValue);
    }

    public final long getAndSetRelease(Object owner, int index, long newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndSet(owner, index, newValue);
    }

    public final float getAndSetRelease(Object owner, int index, float newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndSet(owner, index, newValue);
    }

    public final double getAndSetRelease(Object owner, int index, double newValue) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndSet(owner, index, newValue);
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
            v = rev(unsafe.getChar(staticFieldBase, fieldOffset));
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, rev(v), rev(value)));
        return v;
    }

    public final short getAndAdd(short value) {
        checkStatic(short.class, true, true);
        short v;
        do {
            v = rev(unsafe.getShort(staticFieldBase, fieldOffset));
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, rev(v), rev(value)));
        return v;
    }

    public final int getAndAdd(int value) {
        checkStatic(int.class, true, true);
        if (reverseBytes) {
            int v;
            do {
                v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
            } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev(rev(v) + value)));
            return rev(v);
        }
        return unsafe.getAndAddInt(staticFieldBase, fieldOffset, value);
    }

    public final long getAndAdd(long value) {
        checkStatic(long.class, true, true);
        if (reverseBytes) {
            long v;
            do {
                v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
            } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, rev(rev(v) + value)));
            return rev(v);
        }
        return unsafe.getAndAddLong(staticFieldBase, fieldOffset, value);
    }

    public final float getAndAdd(float value) {
        checkStatic(float.class, true, true);
        float v;
        do {
            v = rev(unsafe.getFloat(staticFieldBase, fieldOffset));
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, Float.floatToRawIntBits(rev(v)), Float.floatToRawIntBits(rev(v + value))));
        return v;
    }

    public final double getAndAdd(double value) {
        checkStatic(double.class, true, true);
        double v;
        do {
            v = rev(unsafe.getDouble(staticFieldBase, fieldOffset));
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, Double.doubleToRawLongBits(rev(v)), Double.doubleToRawLongBits(rev(v + value))));
        return v;
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
            v = rev(unsafe.getChar(owner, fieldOffset));
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, rev(v), rev((char) (v + value))));
        return v;
    }

    public final short getAndAdd(Object owner, short value) {
        checkOwner(owner, short.class, true, true);
        short v;
        do {
            v = rev(unsafe.getShort(owner, fieldOffset));
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, rev(v), rev((short) (v + value))));
        return v;
    }

    public final int getAndAdd(Object owner, int value) {
        checkOwner(owner, int.class, true, true);
        if (reverseBytes) {
            int v;
            do {
                v = unsafe.getIntVolatile(owner, fieldOffset);
            } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev(rev(v) + value)));
            return rev(v);
        }
        return unsafe.getAndAddInt(owner, fieldOffset, value);
    }

    public final long getAndAdd(Object owner, long value) {
        checkOwner(owner, long.class, true, true);
        if (reverseBytes) {
            long v;
            do {
                v = unsafe.getLongVolatile(owner, fieldOffset);
            } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, rev(rev(v) + value)));
            return rev(v);
        }
        return unsafe.getAndAddLong(owner, fieldOffset, value);
    }

    public final float getAndAdd(Object owner, float value) {
        checkOwner(owner, float.class, true, true);
        float v;
        do {
            v = rev(unsafe.getFloat(owner, fieldOffset));
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, Float.floatToRawIntBits(rev(v)), Float.floatToRawIntBits(rev(v + value))));
        return v;
    }

    public final double getAndAdd(Object owner, double value) {
        checkOwner(owner, double.class, true, true);
        double v;
        do {
            v = rev(unsafe.getDouble(owner, fieldOffset));
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, Double.doubleToRawLongBits(rev(v)), Double.doubleToRawLongBits(rev(v + value))));
        return v;
    }

    public final byte getAndAdd(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, true, true);
        byte v;
        do {
            v = unsafe.getByte(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, v + value));
        return v;
    }

    public final char getAndAdd(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, true, true);
        char v;
        do {
            v = rev(unsafe.getChar(owner, indexOffset(index)));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), rev(v), rev((char) (v + value))));
        return v;
    }

    public final short getAndAdd(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, true, true);
        short v;
        do {
            v = rev(unsafe.getShort(owner, indexOffset(index)));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), rev(v), rev((short) (v + value))));
        return v;
    }

    public final int getAndAdd(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, true, true);
        if (reverseBytes) {
            int v;
            do {
                v = unsafe.getIntVolatile(owner, indexOffset(index));
            } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev(rev(v) + value)));
            return rev(v);
        }
        return unsafe.getAndAddInt(owner, indexOffset(index), value);
    }

    public final long getAndAdd(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, true, true);
        if (reverseBytes) {
            long v;
            do {
                v = unsafe.getLongVolatile(owner, indexOffset(index));
            } while (!unsafe.compareAndSwapLong(owner, indexOffset(index), v, rev(rev(v) + value)));
            return rev(v);
        }
        return unsafe.getAndAddLong(owner, indexOffset(index), value);
    }

    public final float getAndAdd(Object owner, int index, float value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, float.class, true, true);
        float v;
        do {
            v = rev(unsafe.getFloat(owner, indexOffset(index)));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), Float.floatToRawIntBits(rev(v)), Float.floatToRawIntBits(rev(v + value))));
        return v;
    }

    public final double getAndAdd(Object owner, int index, double value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, double.class, true, true);
        double v;
        do {
            v = rev(unsafe.getDouble(owner, indexOffset(index)));
        } while (!unsafe.compareAndSwapLong(owner, indexOffset(index), Double.doubleToRawLongBits(rev(v)), Double.doubleToRawLongBits(rev(v + value))));
        return v;
    }

    public final byte getAndAddAcquire(byte value) {
        byte v = getAndAdd(value);
        return v;
    }

    public final char getAndAddAcquire(char value) {
        char v = getAndAdd(value);
        return v;
    }

    public final short getAndAddAcquire(short value) {
        short v = getAndAdd(value);
        return v;
    }

    public final int getAndAddAcquire(int value) {
        int v = getAndAdd(value);
        return rev(v);
    }

    public final long getAndAddAcquire(long value) {
        long v = getAndAdd(value);
        return rev(v);
    }

    public final float getAndAddAcquire(float value) {
        float v = getAndAdd(value);
        return v;
    }

    public final double getAndAddAcquire(double value) {
        double v = getAndAdd(value);
        return v;
    }

    public final byte getAndAddAcquire(Object owner, byte value) {
        byte v = getAndAdd(owner, value);
        return v;
    }

    public final char getAndAddAcquire(Object owner, char value) {
        char v = getAndAdd(owner, value);
        return v;
    }

    public final short getAndAddAcquire(Object owner, short value) {
        short v = getAndAdd(owner, value);
        return v;
    }

    public final int getAndAddAcquire(Object owner, int value) {
        int v = getAndAdd(owner, value);
        return rev(v);
    }

    public final long getAndAddAcquire(Object owner, long value) {
        long v = getAndAdd(owner, value);
        return rev(v);
    }

    public final float getAndAddAcquire(Object owner, float value) {
        float v = getAndAdd(owner, value);
        return v;
    }

    public final double getAndAddAcquire(Object owner, double value) {
        double v = getAndAdd(owner, value);
        return v;
    }

    public final byte getAndAddAcquire(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        byte v = getAndAdd(owner, index, value);
        return v;
    }

    public final char getAndAddAcquire(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        char v = getAndAdd(owner, index, value);
        return v;
    }

    public final short getAndAddAcquire(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        short v = getAndAdd(owner, index, value);
        return v;
    }

    public final int getAndAddAcquire(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        int v = getAndAdd(owner, index, value);
        return rev(v);
    }

    public final long getAndAddAcquire(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        long v = getAndAdd(owner, index, value);
        return rev(v);
    }

    public final float getAndAddAcquire(Object owner, int index, float value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        float v = getAndAdd(owner, index, value);
        return v;
    }

    public final double getAndAddAcquire(Object owner, int index, double value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        double v = getAndAdd(owner, index, value);
        return v;
    }

    public final byte getAndAddRelease(byte value) {
        return getAndAdd(value);
    }

    public final char getAndAddRelease(char value) {
        return getAndAdd(value);
    }

    public final short getAndAddRelease(short value) {
        return getAndAdd(value);
    }

    public final int getAndAddRelease(int value) {
        return getAndAdd(value);
    }

    public final long getAndAddRelease(long value) {
        return getAndAdd(value);
    }

    public final float getAndAddRelease(float value) {
        return getAndAdd(value);
    }

    public final double getAndAddRelease(double value) {
        return getAndAdd(value);
    }

    public final byte getAndAddRelease(Object owner, byte value) {
        return getAndAdd(owner, value);
    }

    public final char getAndAddRelease(Object owner, char value) {
        return getAndAdd(owner, value);
    }

    public final short getAndAddRelease(Object owner, short value) {
        return getAndAdd(owner, value);
    }

    public final int getAndAddRelease(Object owner, int value) {
        return getAndAdd(value);
    }

    public final long getAndAddRelease(Object owner, long value) {
        return getAndAdd(value);
    }

    public final float getAndAddRelease(Object owner, float value) {
        return getAndAdd(owner, value);
    }

    public final double getAndAddRelease(Object owner, double value) {
        return getAndAdd(owner, value);
    }

    public final byte getAndAddRelease(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndAdd(owner, index, value);
    }

    public final char getAndAddRelease(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndAdd(owner, index, value);
    }

    public final short getAndAddRelease(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndAdd(owner, index, value);
    }

    public final int getAndAddRelease(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndAdd(owner, index, value);
    }

    public final long getAndAddRelease(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndAdd(owner, index, value);
    }

    public final float getAndAddRelease(Object owner, int index, float value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndAdd(owner, index, value);
    }

    public final double getAndAddRelease(Object owner, int index, double value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndAdd(owner, index, value);
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
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev((char) (rev((char) v) | value))));
        return rev((char) v);
    }

    public final short getAndBitwiseOr(short value) {
        checkStatic(short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev((short) (rev((short) v) | value))));
        return rev((short) v);
    }

    public final int getAndBitwiseOr(int value) {
        checkStatic(int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev(rev(v) | value)));
        return rev(v);
    }

    public final long getAndBitwiseOr(long value) {
        checkStatic(long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, rev(rev(v) | value)));
        return rev(v);
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
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev((char) (rev((char) v) | value))));
        return rev((char) v);
    }

    public final short getAndBitwiseOr(Object owner, short value) {
        checkOwner(owner, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev((short) (rev((short) v) | value))));
        return rev((short) v);
    }

    public final int getAndBitwiseOr(Object owner, int value) {
        checkOwner(owner, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev(rev(v) | value)));
        return rev(v);
    }

    public final long getAndBitwiseOr(Object owner, long value) {
        checkOwner(owner, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, rev(rev(v) | value)));
        return rev(v);
    }

    public final boolean getAndBitwiseOr(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, (byte) v != 0 || value ? 1 : 0));
        return (byte) v != 0;
    }

    public final byte getAndBitwiseOr(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, v | value));
        return (byte) v;
    }

    public final char getAndBitwiseOr(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev((char) (rev((char) v) | value))));
        return rev((char) v);
    }

    public final short getAndBitwiseOr(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev((short) (rev((short) v) | value))));
        return rev((short) v);
    }

    public final int getAndBitwiseOr(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev(rev(v) | value)));
        return rev(v);
    }

    public final long getAndBitwiseOr(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapLong(owner, indexOffset(index), v, rev(rev(v) | value)));
        return rev(v);
    }


    public final boolean getAndBitwiseOrAcquire(boolean value) {
        boolean v = getAndBitwiseOr(value);
        return v;
    }

    public final byte getAndBitwiseOrAcquire(byte value) {
        byte v = getAndBitwiseOr(value);
        return v;
    }

    public final char getAndBitwiseOrAcquire(char value) {
        char v = getAndBitwiseOr(value);
        return v;
    }

    public final short getAndBitwiseOrAcquire(short value) {
        short v = getAndBitwiseOr(value);
        return v;
    }

    public final int getAndBitwiseOrAcquire(int value) {
        int v = getAndBitwiseOr(value);
        return rev(v);
    }

    public final long getAndBitwiseOrAcquire(long value) {
        long v = getAndBitwiseOr(value);
        return rev(v);
    }

    public final boolean getAndBitwiseOrAcquire(Object owner, boolean value) {
        boolean v = getAndBitwiseOr(owner, value);
        return v;
    }

    public final byte getAndBitwiseOrAcquire(Object owner, byte value) {
        byte v = getAndBitwiseOr(owner, value);
        return v;
    }

    public final char getAndBitwiseOrAcquire(Object owner, char value) {
        char v = getAndBitwiseOr(owner, value);
        return v;
    }

    public final short getAndBitwiseOrAcquire(Object owner, short value) {
        short v = getAndBitwiseOr(owner, value);
        return v;
    }

    public final int getAndBitwiseOrAcquire(Object owner, int value) {
        int v = getAndBitwiseOr(owner, value);
        return rev(v);
    }

    public final long getAndBitwiseOrAcquire(Object owner, long value) {
        long v = getAndBitwiseOr(owner, value);
        return rev(v);
    }

    public final boolean getAndBitwiseOrAcquire(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean v = getAndBitwiseOr(owner, index, value);
        return v;
    }

    public final byte getAndBitwiseOrAcquire(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        byte v = getAndBitwiseOr(owner, index, value);
        return v;
    }

    public final char getAndBitwiseOrAcquire(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        char v = getAndBitwiseOr(owner, index, value);
        return v;
    }

    public final short getAndBitwiseOrAcquire(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        short v = getAndBitwiseOr(owner, index, value);
        return v;
    }

    public final int getAndBitwiseOrAcquire(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        int v = getAndBitwiseOr(owner, index, value);
        return rev(v);
    }

    public final long getAndBitwiseOrAcquire(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        long v = getAndBitwiseOr(owner, index, value);
        return rev(v);
    }

    public final boolean getAndBitwiseOrRelease(boolean value) {
        return getAndBitwiseOr(value);
    }

    public final byte getAndBitwiseOrRelease(byte value) {
        return getAndBitwiseOr(value);
    }

    public final short getAndBitwiseOrRelease(short value) {
        return getAndBitwiseOr(value);
    }

    public final char getAndBitwiseOrRelease(char value) {
        return getAndBitwiseOr(value);
    }

    public final int getAndBitwiseOrRelease(int value) {
        return getAndBitwiseOr(value);
    }

    public final long getAndBitwiseOrRelease(long value) {
        return getAndBitwiseOr(value);
    }

    public final boolean getAndBitwiseOrRelease(Object owner, boolean value) {
        return getAndBitwiseOr(owner, value);
    }

    public final byte getAndBitwiseOrRelease(Object owner, byte value) {
        return getAndBitwiseOr(owner, value);
    }

    public final char getAndBitwiseOrRelease(Object owner, char value) {
        return getAndBitwiseOr(owner, value);
    }

    public final short getAndBitwiseOrRelease(Object owner, short value) {
        return getAndBitwiseOr(owner, value);
    }

    public final int getAndBitwiseOrRelease(Object owner, int value) {
        return getAndBitwiseOr(owner, value);
    }

    public final long getAndBitwiseOrRelease(Object owner, long value) {
        return getAndBitwiseOr(owner, value);
    }

    public final boolean getAndBitwiseOrRelease(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseOr(owner, index, value);
    }

    public final byte getAndBitwiseOrRelease(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseOr(owner, index, value);
    }

    public final char getAndBitwiseOrRelease(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseOr(owner, index, value);
    }

    public final short getAndBitwiseOrRelease(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseOr(owner, index, value);
    }

    public final int getAndBitwiseOrRelease(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseOr(owner, index, value);
    }

    public final long getAndBitwiseOrRelease(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseOr(owner, index, value);
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
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev((char) (rev((char) v) & value))));
        return rev((char) v);
    }

    public final short getAndBitwiseAnd(short value) {
        checkStatic(short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev((short) (rev((short) v) & value))));
        return rev((short) v);
    }

    public final int getAndBitwiseAnd(int value) {
        checkStatic(int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev(rev(v) & value)));
        return rev(v);
    }

    public final long getAndBitwiseAnd(long value) {
        checkStatic(long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, rev(rev(v) & value)));
        return rev(v);
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
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev((char) (rev((char) v) & value))));
        return rev((char) v);
    }

    public final short getAndBitwiseAnd(Object owner, short value) {
        checkOwner(owner, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev((short) (rev((short) v) & value))));
        return rev((short) v);
    }

    public final int getAndBitwiseAnd(Object owner, int value) {
        checkOwner(owner, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev(rev(v) & value)));
        return rev(v);
    }

    public final long getAndBitwiseAnd(Object owner, long value) {
        checkOwner(owner, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, rev(rev(v) & value)));
        return rev(v);
    }

    public final boolean getAndBitwiseAnd(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, (byte) v != 0 && value ? 1 : 0));
        return (byte) v != 0;
    }

    public final byte getAndBitwiseAnd(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, v & value));
        return (byte) v;
    }

    public final char getAndBitwiseAnd(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev((char) (rev((char) v) & value))));
        return rev((char) v);
    }

    public final short getAndBitwiseAnd(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev((short) (rev((short) v) & value))));
        return rev((short) v);
    }

    public final int getAndBitwiseAnd(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev(rev(v) & value)));
        return rev(v);
    }

    public final long getAndBitwiseAnd(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, true, true);
        long v;
        do {
             v = unsafe.getLongVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapLong(owner, indexOffset(index), v, rev(rev(v) & value)));
        return rev(v);
    }

    public final boolean getAndBitwiseAndAcquire(boolean value) {
        boolean v = getAndBitwiseAnd(value);
        return v;
    }

    public final byte getAndBitwiseAndAcquire(byte value) {
        byte v = getAndBitwiseAnd(value);
        return v;
    }

    public final char getAndBitwiseAndAcquire(char value) {
        char v = getAndBitwiseAnd(value);
        return v;
    }

    public final short getAndBitwiseAndAcquire(short value) {
        short v = getAndBitwiseAnd(value);
        return v;
    }

    public final int getAndBitwiseAndAcquire(int value) {
        int v = getAndBitwiseAnd(value);
        return rev(v);
    }

    public final long getAndBitwiseAndAcquire(long value) {
        long v = getAndBitwiseAnd(value);
        return rev(v);
    }

    public final boolean getAndBitwiseAndAcquire(Object owner, boolean value) {
        boolean v = getAndBitwiseAnd(owner, value);
        return v;
    }

    public final byte getAndBitwiseAndAcquire(Object owner, byte value) {
        byte v = getAndBitwiseAnd(owner, value);
        return v;
    }

    public final char getAndBitwiseAndAcquire(Object owner, char value) {
        char v = getAndBitwiseAnd(owner, value);
        return v;
    }

    public final short getAndBitwiseAndAcquire(Object owner, short value) {
        short v = getAndBitwiseAnd(owner, value);
        return v;
    }

    public final int getAndBitwiseAndAcquire(Object owner, int value) {
        int v = getAndBitwiseAnd(owner, value);
        return rev(v);
    }

    public final long getAndBitwiseAndAcquire(Object owner, long value) {
        long v = getAndBitwiseAnd(owner, value);
        return rev(v);
    }
    
    public final boolean getAndBitwiseAndAcquire(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean v = getAndBitwiseAnd(owner, index, value);
        return v;
    }
    
    public final byte getAndBitwiseAndAcquire(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        byte v = getAndBitwiseAnd(owner, index, value);
        return v;
    }
    
    public final char getAndBitwiseAndAcquire(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        char v = getAndBitwiseAnd(owner, index, value);
        return v;
    }
    
    public final short getAndBitwiseAndAcquire(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        short v = getAndBitwiseAnd(owner, index, value);
        return v;
    }
    
    public final int getAndBitwiseAndAcquire(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        int v = getAndBitwiseAnd(owner, index, value);
        return rev(v);
    }
    
    public final long getAndBitwiseAndAcquire(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        long v = getAndBitwiseAnd(owner, index, value);
        return rev(v);
    }

    public final boolean getAndBitwiseAndRelease(boolean value) {
        return getAndBitwiseAnd(value);
    }

    public final byte getAndBitwiseAndRelease(byte value) {
        return getAndBitwiseAnd(value);
    }

    public final char getAndBitwiseAndRelease(char value) {
        return getAndBitwiseAnd(value);
    }

    public final short getAndBitwiseAndRelease(short value) {
        return getAndBitwiseAnd(value);
    }

    public final int getAndBitwiseAndRelease(int value) {
        return getAndBitwiseAnd(value);
    }

    public final long getAndBitwiseAndRelease(long value) {
        return getAndBitwiseAnd(value);
    }

    public final boolean getAndBitwiseAndRelease(Object owner, boolean value) {
        return getAndBitwiseAnd(owner, value);
    }

    public final byte getAndBitwiseAndRelease(Object owner, byte value) {
        return getAndBitwiseAnd(owner, value);
    }

    public final char getAndBitwiseAndRelease(Object owner, char value) {
        return getAndBitwiseAnd(owner, value);
    }

    public final short getAndBitwiseAndRelease(Object owner, short value) {
        return getAndBitwiseAnd(owner, value);
    }

    public final int getAndBitwiseAndRelease(Object owner, int value) {
        return getAndBitwiseAnd(owner, value);
    }

    public final long getAndBitwiseAndRelease(Object owner, long value) {
        return getAndBitwiseAnd(owner, value);
    }

    public final boolean getAndBitwiseAndRelease(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseAnd(owner, index, value);
    }

    public final byte getAndBitwiseAndRelease(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseAnd(owner, index, value);
    }

    public final char getAndBitwiseAndRelease(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseAnd(owner, index, value);
    }

    public final short getAndBitwiseAndRelease(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseAnd(owner, index, value);
    }

    public final int getAndBitwiseAndRelease(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseAnd(owner, index, value);
    }

    public final long getAndBitwiseAndRelease(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseAnd(owner, index, value);
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
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev((char) (rev((char) v) ^ value))));
        return rev((char) v);
    }


    public final short getAndBitwiseXor(short value) {
        checkStatic(short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev((short) (rev((short) v) ^ value))));
        return rev((short) v);
    }

    public final int getAndBitwiseXor(int value) {
        checkStatic(int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapInt(staticFieldBase, fieldOffset, v, rev(rev(v) ^ value)));
        return rev(v);
    }

    public final long getAndBitwiseXor(long value) {
        checkStatic(long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(staticFieldBase, fieldOffset);
        } while (!unsafe.compareAndSwapLong(staticFieldBase, fieldOffset, v, rev(rev(v) ^ value)));
        return rev(v);
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
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev((char) (rev((char) v) ^ value))));
        return rev((char) v);
    }

    public final short getAndBitwiseXor(Object owner, short value) {
        checkOwner(owner, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev((short) (rev((short) v) ^ value))));
        return rev((short) v);
    }

    public final int getAndBitwiseXor(Object owner, int value) {
        checkOwner(owner, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapInt(owner, fieldOffset, v, rev(rev(v) ^ value)));
        return rev(v);
    }

    public final long getAndBitwiseXor(Object owner, long value) {
        checkOwner(owner, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, fieldOffset);
        } while (!unsafe.compareAndSwapLong(owner, fieldOffset, v, rev(rev(v) ^ value)));
        return rev(v);
    }

    public final boolean getAndBitwiseXor(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, boolean.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, ((byte) v != 0) ^ value ? 1 : 0));
        return (byte) v != 0;
    }

    public final byte getAndBitwiseXor(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, byte.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, v ^ value));
        return (byte) v;
    }

    public final char getAndBitwiseXor(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, char.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev((char) (rev((char) v) ^ value))));
        return rev((char) v);
    }

    public final short getAndBitwiseXor(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, short.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev((short) (rev((short) v) ^ value))));
        return rev((short) v);
    }

    public final int getAndBitwiseXor(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, int.class, true, true);
        int v;
        do {
            v = unsafe.getIntVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapInt(owner, indexOffset(index), v, rev(rev(v) ^ value)));
        return rev(v);
    }

    public final long getAndBitwiseXor(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        checkArray(owner, index, long.class, true, true);
        long v;
        do {
            v = unsafe.getLongVolatile(owner, indexOffset(index));
        } while (!unsafe.compareAndSwapLong(owner, indexOffset(index), v, rev(rev(v) ^ value)));
        return rev(v);
    }


    public final boolean getAndBitwiseXorAcquire(boolean value) {
        boolean v = getAndBitwiseXor(value);
        return v;
    }

    public final byte getAndBitwiseXorAcquire(byte value) {
        byte v = getAndBitwiseXor(value);
        return v;
    }

    public final char getAndBitwiseXorAcquire(char value) {
        char v = getAndBitwiseXor(value);
        return v;
    }

    public final short getAndBitwiseXorAcquire(short value) {
        short v = getAndBitwiseXor(value);
        return v;
    }

    public final int getAndBitwiseXorAcquire(int value) {
        int v = getAndBitwiseXor(value);
        return rev(v);
    }

    public final long getAndBitwiseXorAcquire(long value) {
        long v = getAndBitwiseXor(value);
        return rev(v);
    }

    public final boolean getAndBitwiseXorAcquire(Object owner, boolean value) {
        boolean v = getAndBitwiseXor(owner, value);
        return v;
    }

    public final byte getAndBitwiseXorAcquire(Object owner, byte value) {
        byte v = getAndBitwiseXor(owner, value);
        return v;
    }

    public final char getAndBitwiseXorAcquire(Object owner, char value) {
        char v = getAndBitwiseXor(owner, value);
        return v;
    }

    public final short getAndBitwiseXorAcquire(Object owner, short value) {
        short v = getAndBitwiseXor(owner, value);
        return v;
    }

    public final int getAndBitwiseXorAcquire(Object owner, int value) {
        int v = getAndBitwiseXor(owner, value);
        return rev(v);
    }

    public final long getAndBitwiseXorAcquire(Object owner, long value) {
        long v = getAndBitwiseXor(owner, value);
        return rev(v);
    }

    public final boolean getAndBitwiseXorAcquire(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        boolean v = getAndBitwiseXor(owner, index, value);
        return v;
    }

    public final byte getAndBitwiseXorAcquire(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        byte v = getAndBitwiseXor(owner, index, value);
        return v;
    }

    public final char getAndBitwiseXorAcquire(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        char v = getAndBitwiseXor(owner, index, value);
        return v;
    }

    public final short getAndBitwiseXorAcquire(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        short v = getAndBitwiseXor(owner, index, value);
        return v;
    }

    public final int getAndBitwiseXorAcquire(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        int v = getAndBitwiseXor(owner, index, value);
        return rev(v);
    }

    public final long getAndBitwiseXorAcquire(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        long v = getAndBitwiseXor(owner, index, value);
        return rev(v);
    }

     public final boolean getAndBitwiseXorRelease(boolean value) {
        return getAndBitwiseXor(value);
    }

    public final byte getAndBitwiseXorRelease(byte value) {
        return getAndBitwiseXor(value);
    }

    public final short getAndBitwiseXorRelease(short value) {
        return getAndBitwiseXor(value);
    }

    public final char getAndBitwiseXorRelease(char value) {
        return getAndBitwiseXor(value);
    }

    public final int getAndBitwiseXorRelease(int value) {
        return getAndBitwiseXor(value);
    }

    public final long getAndBitwiseXorRelease(long value) {
        return getAndBitwiseXor(value);
    }

    public final boolean getAndBitwiseXorRelease(Object owner, boolean value) {
        return getAndBitwiseXor(owner, value);
    }

    public final byte getAndBitwiseXorRelease(Object owner, byte value) {
        return getAndBitwiseXor(owner, value);
    }

    public final char getAndBitwiseXorRelease(Object owner, char value) {
        return getAndBitwiseXor(owner, value);
    }

    public final short getAndBitwiseXorRelease(Object owner, short value) {
        return getAndBitwiseXor(owner, value);
    }

    public final int getAndBitwiseXorRelease(Object owner, int value) {
        return getAndBitwiseXor(owner, value);
    }

    public final long getAndBitwiseXorRelease(Object owner, long value) {
        return getAndBitwiseXor(owner, value);
    }

    public final boolean getAndBitwiseXorRelease(Object owner, int index, boolean value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseXor(owner, index, value);
    }

    public final byte getAndBitwiseXorRelease(Object owner, int index, byte value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseXor(owner, index, value);
    }

    public final char getAndBitwiseXorRelease(Object owner, int index, char value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseXor(owner, index, value);
    }

    public final short getAndBitwiseXorRelease(Object owner, int index, short value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseXor(owner, index, value);
    }

    public final int getAndBitwiseXorRelease(Object owner, int index, int value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

        return getAndBitwiseXor(owner, index, value);
    }

    public final long getAndBitwiseXorRelease(Object owner, int index, long value) {
        index += arrayIndexOffset(owner);
        owner = arrayView(owner);

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
                } else if (isArray) {
                    mt = MethodType.methodType(t, Object.class, int.class);
                    vmt = MethodType.methodType(vt, ownerClass, int.class);
                } else {
                    mt = MethodType.methodType(t, Object.class);
                    vmt = MethodType.methodType(vt, ownerClass);
                }
                break;
            case SET:
                if (isStatic) {
                    mt = MethodType.methodType(void.class, t);
                    vmt = MethodType.methodType(void.class, vt);
                } else if (isArray) {
                    mt = MethodType.methodType(void.class, Object.class, int.class, t);
                    vmt = MethodType.methodType(void.class, ownerClass, int.class, vt);
                } else {
                    mt = MethodType.methodType(void.class, Object.class, t);
                    vmt = MethodType.methodType(void.class, ownerClass, vt);
                }
                break;
            case COMPARE_AND_SET:
                if (isStatic) {
                    mt = MethodType.methodType(boolean.class, t, t);
                    vmt = MethodType.methodType(boolean.class, vt, vt);
                } else if (isArray) {
                    mt = MethodType.methodType(boolean.class, Object.class, int.class, t, t);
                    vmt = MethodType.methodType(boolean.class, ownerClass, int.class, vt, vt);
                } else  {
                    mt = MethodType.methodType(boolean.class, Object.class, t, t);
                    vmt = MethodType.methodType(boolean.class, ownerClass, vt, vt);
                }
                break;
            case COMPARE_AND_EXCHANGE:
                if (isStatic) {
                    mt = MethodType.methodType(t, t, t);
                    vmt = MethodType.methodType(vt, vt, vt);
                } else if (isArray) {
                    mt = MethodType.methodType(t, Object.class, int.class, t, t);
                    vmt = MethodType.methodType(vt, ownerClass, int.class, vt, vt);
                } else  {
                    mt = MethodType.methodType(t, Object.class, t, t);
                    vmt = MethodType.methodType(vt, ownerClass, vt, vt);
                }
                break;
            case GET_AND_UPDATE:
                if (isStatic) {
                    mt = MethodType.methodType(t, t);
                    vmt = MethodType.methodType(vt, vt);
                } else if (isArray) {
                    mt = MethodType.methodType(t, Object.class, int.class, t);
                    vmt = MethodType.methodType(vt, ownerClass, int.class, vt);
                } else {
                    mt = MethodType.methodType(t, Object.class, t);
                    vmt = MethodType.methodType(vt, ownerClass, vt);
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
        GET_AND_BITWISE_AND("getAndBitwiseAnd", AccessType.GET_AND_UPDATE),
        GET_AND_BITWISE_AND_RELEASE("getAndBitwiseAndRelease", AccessType.GET_AND_UPDATE),
        GET_AND_BITWISE_AND_ACQUIRE("getAndBitwiseAndAcquire", AccessType.GET_AND_UPDATE),
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
