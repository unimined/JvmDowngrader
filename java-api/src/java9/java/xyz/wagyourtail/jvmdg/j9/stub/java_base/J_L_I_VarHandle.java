package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.j9.intl.varhandle.ArrayVarHandle;
import xyz.wagyourtail.jvmdg.j9.intl.varhandle.ByteArrayViewVarHandle;
import xyz.wagyourtail.jvmdg.j9.intl.varhandle.ByteBufferViewVarHandle;
import xyz.wagyourtail.jvmdg.j9.intl.varhandle.InstanceFieldVarHandle;
import xyz.wagyourtail.jvmdg.j9.intl.varhandle.StaticFieldVarHandle;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JEP(193)
@Adapter("java/lang/invoke/VarHandle")
public abstract class J_L_I_VarHandle implements xyz.wagyourtail.jvmdg.stub.VarHandleInternals {
    protected static final Unsafe unsafe = Utils.getUnsafe();

    @Modify(ref = @Ref("xyz/wagyourtail/jvmdg/j9/stub/java_base/J_L_I_VarHandle"))
    public static void modifyPolymorphics(MethodNode mnode, int i) {
        MethodInsnNode min = (MethodInsnNode) mnode.instructions.get(i);
        AccessMode am = AccessMode.byName.get(min.name);
        if (am == null) return;
        Type ret = Type.getReturnType(min.desc);
        min.name = fixMethodName(am.stubName, ret.getDescriptor());
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

    protected final Class<?> ownerClass;
    protected final Class<?> type;

    protected J_L_I_VarHandle(Class<?> ownerClass, Class<?> type) {
        this.ownerClass = ownerClass;
        this.type = type;
    }

    public static J_L_I_VarHandle of(Field field) {
        return Modifier.isStatic(field.getModifiers()) ? new StaticFieldVarHandle(field) : new InstanceFieldVarHandle(field);
    }

    public static J_L_I_VarHandle ofArray(Class<?> arrayClass) {
        return new ArrayVarHandle(arrayClass);
    }

    public static J_L_I_VarHandle ofByteArrayView(Class<?> viewClass, boolean reverseBytes) {
        return new ByteArrayViewVarHandle(viewClass, reverseBytes);
    }

    public static J_L_I_VarHandle ofByteBufferView(Class<?> viewClass, boolean reverseBytes) {
        return new ByteBufferViewVarHandle(viewClass, reverseBytes);
    }

    @Override
    public Field jvmdg$field() {
        return null;
    }

    @Override
    public Class<?> jvmdg$ownerClass() {
        return ownerClass;
    }

    @Override
    public Class<?> jvmdg$varType() {
        return type;
    }

    @Override
    public boolean jvmdg$isStatic() {
        return false;
    }

    @Override
    public boolean jvmdg$isArray() {
        return false;
    }

    @Override
    public boolean jvmdg$isByteArrayView() {
        return false;
    }

    /**
     * Matches the jdk9+ {@code VarHandle.toString} format
     * {@code VarHandle[varType=<type>, coord=[<coord types>]}.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("VarHandle[varType=");
        sb.append(type.getName()).append(", coord=[");
        if (jvmdg$isArray()) {
            // byte array views coordinate on the underlying byte[]
            sb.append(jvmdg$isByteArrayView() ? byte[].class : ownerClass).append(", int");
        } else if (!jvmdg$isStatic()) {
            sb.append(ownerClass);
        }
        return sb.append("]]").toString();
    }

    public Object get() { throw new WrongMethodTypeException(); }

    public boolean getZ() { throw new WrongMethodTypeException(); }

    public byte getB() { throw new WrongMethodTypeException(); }

    public char getC() { throw new WrongMethodTypeException(); }

    public short getS() { throw new WrongMethodTypeException(); }

    public int getI() { throw new WrongMethodTypeException(); }

    public long getJ() { throw new WrongMethodTypeException(); }

    public float getF() { throw new WrongMethodTypeException(); }

    public double getD() { throw new WrongMethodTypeException(); }

    public Object get(Object owner) { throw new WrongMethodTypeException(); }

    public boolean getZ(Object owner) { throw new WrongMethodTypeException(); }

    public byte getB(Object owner) { throw new WrongMethodTypeException(); }

    public char getC(Object owner) { throw new WrongMethodTypeException(); }

    public short getS(Object owner) { throw new WrongMethodTypeException(); }

    public int getI(Object owner) { throw new WrongMethodTypeException(); }

    public long getJ(Object owner) { throw new WrongMethodTypeException(); }

    public float getF(Object owner) { throw new WrongMethodTypeException(); }

    public double getD(Object owner) { throw new WrongMethodTypeException(); }

    public Object get(Object owner, int index) { throw new WrongMethodTypeException(); }

    public boolean getZ(Object owner, int index) { throw new WrongMethodTypeException(); }

    public byte getB(Object owner, int index) { throw new WrongMethodTypeException(); }

    public char getC(Object owner, int index) { throw new WrongMethodTypeException(); }

    public short getS(Object owner, int index) { throw new WrongMethodTypeException(); }

    public int getI(Object owner, int index) { throw new WrongMethodTypeException(); }

    public long getJ(Object owner, int index) { throw new WrongMethodTypeException(); }

    public float getF(Object owner, int index) { throw new WrongMethodTypeException(); }

    public double getD(Object owner, int index) { throw new WrongMethodTypeException(); }

    public void set(Object value) { throw new WrongMethodTypeException(); }

    public void set(boolean value) { throw new WrongMethodTypeException(); }

    public void set(byte value) { throw new WrongMethodTypeException(); }

    public void set(char value) { throw new WrongMethodTypeException(); }

    public void set(short value) { throw new WrongMethodTypeException(); }

    public void set(int value) { throw new WrongMethodTypeException(); }

    public void set(long value) { throw new WrongMethodTypeException(); }

    public void set(float value) { throw new WrongMethodTypeException(); }

    public void set(double value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, Object value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, boolean value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, byte value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, char value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, short value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, int value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, long value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, float value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, double value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, int index, Object value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, int index, boolean value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, int index, byte value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, int index, char value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, int index, short value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, int index, int value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, int index, long value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, int index, float value) { throw new WrongMethodTypeException(); }

    public void set(Object owner, int index, double value) { throw new WrongMethodTypeException(); }

    public Object getVolatile() { throw new WrongMethodTypeException(); }

    public boolean getVolatileZ() { throw new WrongMethodTypeException(); }

    public byte getVolatileB() { throw new WrongMethodTypeException(); }

    public char getVolatileC() { throw new WrongMethodTypeException(); }

    public short getVolatileS() { throw new WrongMethodTypeException(); }

    public int getVolatileI() { throw new WrongMethodTypeException(); }

    public long getVolatileJ() { throw new WrongMethodTypeException(); }

    public float getVolatileF() { throw new WrongMethodTypeException(); }

    public double getVolatileD() { throw new WrongMethodTypeException(); }

    public Object getVolatile(Object owner) { throw new WrongMethodTypeException(); }

    public boolean getVolatileZ(Object owner) { throw new WrongMethodTypeException(); }

    public byte getVolatileB(Object owner) { throw new WrongMethodTypeException(); }

    public char getVolatileC(Object owner) { throw new WrongMethodTypeException(); }

    public short getVolatileS(Object owner) { throw new WrongMethodTypeException(); }

    public int getVolatileI(Object owner) { throw new WrongMethodTypeException(); }

    public long getVolatileJ(Object owner) { throw new WrongMethodTypeException(); }

    public float getVolatileF(Object owner) { throw new WrongMethodTypeException(); }

    public double getVolatileD(Object owner) { throw new WrongMethodTypeException(); }

    public Object getVolatile(Object owner, int index) { throw new WrongMethodTypeException(); }

    public boolean getVolatileZ(Object owner, int index) { throw new WrongMethodTypeException(); }

    public byte getVolatileB(Object owner, int index) { throw new WrongMethodTypeException(); }

    public char getVolatileC(Object owner, int index) { throw new WrongMethodTypeException(); }

    public short getVolatileS(Object owner, int index) { throw new WrongMethodTypeException(); }

    public int getVolatileI(Object owner, int index) { throw new WrongMethodTypeException(); }

    public long getVolatileJ(Object owner, int index) { throw new WrongMethodTypeException(); }

    public float getVolatileF(Object owner, int index) { throw new WrongMethodTypeException(); }

    public double getVolatileD(Object owner, int index) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object value) { throw new WrongMethodTypeException(); }

    public void setVolatile(boolean value) { throw new WrongMethodTypeException(); }

    public void setVolatile(byte value) { throw new WrongMethodTypeException(); }

    public void setVolatile(char value) { throw new WrongMethodTypeException(); }

    public void setVolatile(short value) { throw new WrongMethodTypeException(); }

    public void setVolatile(int value) { throw new WrongMethodTypeException(); }

    public void setVolatile(long value) { throw new WrongMethodTypeException(); }

    public void setVolatile(float value) { throw new WrongMethodTypeException(); }

    public void setVolatile(double value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, Object value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, boolean value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, byte value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, char value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, short value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, int value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, long value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, float value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, double value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, int index, Object value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, int index, boolean value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, int index, byte value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, int index, char value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, int index, short value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, int index, int value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, int index, long value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, int index, float value) { throw new WrongMethodTypeException(); }

    public void setVolatile(Object owner, int index, double value) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object expected, Object newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(boolean expected, boolean newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(byte expected, byte newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(char expected, char newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(short expected, short newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(int expected, int newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(long expected, long newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(float expected, float newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(double expected, double newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, Object expected, Object newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, boolean expected, boolean newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, byte expected, byte newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, char expected, char newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, short expected, short newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, int expected, int newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, long expected, long newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, float expected, float newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, double expected, double newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, int index, Object expected, Object newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, int index, boolean expected, boolean newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, int index, byte expected, byte newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, int index, char expected, char newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, int index, short expected, short newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, int index, int expected, int newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, int index, long expected, long newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, int index, float expected, float newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndSet(Object owner, int index, double expected, double newValue) { throw new WrongMethodTypeException(); }

    public Object compareAndExchange(Object expected, Object newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndExchange(boolean expected, boolean newValue) { throw new WrongMethodTypeException(); }

    public byte compareAndExchange(byte expected, byte newValue) { throw new WrongMethodTypeException(); }

    public char compareAndExchange(char expected, char newValue) { throw new WrongMethodTypeException(); }

    public short compareAndExchange(short expected, short newValue) { throw new WrongMethodTypeException(); }

    public int compareAndExchange(int expected, int newValue) { throw new WrongMethodTypeException(); }

    public long compareAndExchange(long expected, long newValue) { throw new WrongMethodTypeException(); }

    public float compareAndExchange(float expected, float newValue) { throw new WrongMethodTypeException(); }

    public double compareAndExchange(double expected, double newValue) { throw new WrongMethodTypeException(); }

    public Object compareAndExchange(Object owner, Object expected, Object newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndExchange(Object owner, boolean expected, boolean newValue) { throw new WrongMethodTypeException(); }

    public byte compareAndExchange(Object owner, byte expected, byte newValue) { throw new WrongMethodTypeException(); }

    public char compareAndExchange(Object owner, char expected, char newValue) { throw new WrongMethodTypeException(); }

    public short compareAndExchange(Object owner, short expected, short newValue) { throw new WrongMethodTypeException(); }

    public int compareAndExchange(Object owner, int expected, int newValue) { throw new WrongMethodTypeException(); }

    public long compareAndExchange(Object owner, long expected, long newValue) { throw new WrongMethodTypeException(); }

    public float compareAndExchange(Object owner, float expected, float newValue) { throw new WrongMethodTypeException(); }

    public double compareAndExchange(Object owner, double expected, double newValue) { throw new WrongMethodTypeException(); }

    public Object compareAndExchange(Object owner, int index, Object expected, Object newValue) { throw new WrongMethodTypeException(); }

    public boolean compareAndExchange(Object owner, int index, boolean expected, boolean newValue) { throw new WrongMethodTypeException(); }

    public byte compareAndExchange(Object owner, int index, byte expected, byte newValue) { throw new WrongMethodTypeException(); }

    public char compareAndExchange(Object owner, int index, char expected, char newValue) { throw new WrongMethodTypeException(); }

    public short compareAndExchange(Object owner, int index, short expected, short newValue) { throw new WrongMethodTypeException(); }

    public int compareAndExchange(Object owner, int index, int expected, int newValue) { throw new WrongMethodTypeException(); }

    public long compareAndExchange(Object owner, int index, long expected, long newValue) { throw new WrongMethodTypeException(); }

    public float compareAndExchange(Object owner, int index, float expected, float newValue) { throw new WrongMethodTypeException(); }

    public double compareAndExchange(Object owner, int index, double expected, double newValue) { throw new WrongMethodTypeException(); }

    public Object getAndSet(Object newValue) { throw new WrongMethodTypeException(); }

    public boolean getAndSet(boolean newValue) { throw new WrongMethodTypeException(); }

    public byte getAndSet(byte newValue) { throw new WrongMethodTypeException(); }

    public char getAndSet(char newValue) { throw new WrongMethodTypeException(); }

    public short getAndSet(short newValue) { throw new WrongMethodTypeException(); }

    public int getAndSet(int newValue) { throw new WrongMethodTypeException(); }

    public long getAndSet(long newValue) { throw new WrongMethodTypeException(); }

    public float getAndSet(float newValue) { throw new WrongMethodTypeException(); }

    public double getAndSet(double newValue) { throw new WrongMethodTypeException(); }

    public Object getAndSet(Object owner, Object newValue) { throw new WrongMethodTypeException(); }

    public boolean getAndSet(Object owner, boolean newValue) { throw new WrongMethodTypeException(); }

    public byte getAndSet(Object owner, byte newValue) { throw new WrongMethodTypeException(); }

    public char getAndSet(Object owner, char newValue) { throw new WrongMethodTypeException(); }

    public short getAndSet(Object owner, short newValue) { throw new WrongMethodTypeException(); }

    public int getAndSet(Object owner, int newValue) { throw new WrongMethodTypeException(); }

    public long getAndSet(Object owner, long newValue) { throw new WrongMethodTypeException(); }

    public float getAndSet(Object owner, float newValue) { throw new WrongMethodTypeException(); }

    public double getAndSet(Object owner, double newValue) { throw new WrongMethodTypeException(); }

    public Object getAndSet(Object owner, int index, Object newValue) { throw new WrongMethodTypeException(); }

    public boolean getAndSet(Object owner, int index, boolean newValue) { throw new WrongMethodTypeException(); }

    public byte getAndSet(Object owner, int index, byte newValue) { throw new WrongMethodTypeException(); }

    public char getAndSet(Object owner, int index, char newValue) { throw new WrongMethodTypeException(); }

    public short getAndSet(Object owner, int index, short newValue) { throw new WrongMethodTypeException(); }

    public int getAndSet(Object owner, int index, int newValue) { throw new WrongMethodTypeException(); }

    public long getAndSet(Object owner, int index, long newValue) { throw new WrongMethodTypeException(); }

    public float getAndSet(Object owner, int index, float newValue) { throw new WrongMethodTypeException(); }

    public double getAndSet(Object owner, int index, double newValue) { throw new WrongMethodTypeException(); }

    public byte getAndAdd(byte value) { throw new WrongMethodTypeException(); }

    public char getAndAdd(char value) { throw new WrongMethodTypeException(); }

    public short getAndAdd(short value) { throw new WrongMethodTypeException(); }

    public int getAndAdd(int value) { throw new WrongMethodTypeException(); }

    public long getAndAdd(long value) { throw new WrongMethodTypeException(); }

    public float getAndAdd(float value) { throw new WrongMethodTypeException(); }

    public double getAndAdd(double value) { throw new WrongMethodTypeException(); }

    public byte getAndAdd(Object owner, byte value) { throw new WrongMethodTypeException(); }

    public char getAndAdd(Object owner, char value) { throw new WrongMethodTypeException(); }

    public short getAndAdd(Object owner, short value) { throw new WrongMethodTypeException(); }

    public int getAndAdd(Object owner, int value) { throw new WrongMethodTypeException(); }

    public long getAndAdd(Object owner, long value) { throw new WrongMethodTypeException(); }

    public float getAndAdd(Object owner, float value) { throw new WrongMethodTypeException(); }

    public double getAndAdd(Object owner, double value) { throw new WrongMethodTypeException(); }

    public byte getAndAdd(Object owner, int index, byte value) { throw new WrongMethodTypeException(); }

    public char getAndAdd(Object owner, int index, char value) { throw new WrongMethodTypeException(); }

    public short getAndAdd(Object owner, int index, short value) { throw new WrongMethodTypeException(); }

    public int getAndAdd(Object owner, int index, int value) { throw new WrongMethodTypeException(); }

    public long getAndAdd(Object owner, int index, long value) { throw new WrongMethodTypeException(); }

    public float getAndAdd(Object owner, int index, float value) { throw new WrongMethodTypeException(); }

    public double getAndAdd(Object owner, int index, double value) { throw new WrongMethodTypeException(); }

    public boolean getAndBitwiseOr(boolean value) { throw new WrongMethodTypeException(); }

    public byte getAndBitwiseOr(byte value) { throw new WrongMethodTypeException(); }

    public char getAndBitwiseOr(char value) { throw new WrongMethodTypeException(); }

    public short getAndBitwiseOr(short value) { throw new WrongMethodTypeException(); }

    public int getAndBitwiseOr(int value) { throw new WrongMethodTypeException(); }

    public long getAndBitwiseOr(long value) { throw new WrongMethodTypeException(); }

    public boolean getAndBitwiseOr(Object owner, boolean value) { throw new WrongMethodTypeException(); }

    public byte getAndBitwiseOr(Object owner, byte value) { throw new WrongMethodTypeException(); }

    public char getAndBitwiseOr(Object owner, char value) { throw new WrongMethodTypeException(); }

    public short getAndBitwiseOr(Object owner, short value) { throw new WrongMethodTypeException(); }

    public int getAndBitwiseOr(Object owner, int value) { throw new WrongMethodTypeException(); }

    public long getAndBitwiseOr(Object owner, long value) { throw new WrongMethodTypeException(); }

    public boolean getAndBitwiseOr(Object owner, int index, boolean value) { throw new WrongMethodTypeException(); }

    public byte getAndBitwiseOr(Object owner, int index, byte value) { throw new WrongMethodTypeException(); }

    public char getAndBitwiseOr(Object owner, int index, char value) { throw new WrongMethodTypeException(); }

    public short getAndBitwiseOr(Object owner, int index, short value) { throw new WrongMethodTypeException(); }

    public int getAndBitwiseOr(Object owner, int index, int value) { throw new WrongMethodTypeException(); }

    public long getAndBitwiseOr(Object owner, int index, long value) { throw new WrongMethodTypeException(); }

    public boolean getAndBitwiseAnd(boolean value) { throw new WrongMethodTypeException(); }

    public byte getAndBitwiseAnd(byte value) { throw new WrongMethodTypeException(); }

    public char getAndBitwiseAnd(char value) { throw new WrongMethodTypeException(); }

    public short getAndBitwiseAnd(short value) { throw new WrongMethodTypeException(); }

    public int getAndBitwiseAnd(int value) { throw new WrongMethodTypeException(); }

    public long getAndBitwiseAnd(long value) { throw new WrongMethodTypeException(); }

    public boolean getAndBitwiseAnd(Object owner, boolean value) { throw new WrongMethodTypeException(); }

    public byte getAndBitwiseAnd(Object owner, byte value) { throw new WrongMethodTypeException(); }

    public char getAndBitwiseAnd(Object owner, char value) { throw new WrongMethodTypeException(); }

    public short getAndBitwiseAnd(Object owner, short value) { throw new WrongMethodTypeException(); }

    public int getAndBitwiseAnd(Object owner, int value) { throw new WrongMethodTypeException(); }

    public long getAndBitwiseAnd(Object owner, long value) { throw new WrongMethodTypeException(); }

    public boolean getAndBitwiseAnd(Object owner, int index, boolean value) { throw new WrongMethodTypeException(); }

    public byte getAndBitwiseAnd(Object owner, int index, byte value) { throw new WrongMethodTypeException(); }

    public char getAndBitwiseAnd(Object owner, int index, char value) { throw new WrongMethodTypeException(); }

    public short getAndBitwiseAnd(Object owner, int index, short value) { throw new WrongMethodTypeException(); }

    public int getAndBitwiseAnd(Object owner, int index, int value) { throw new WrongMethodTypeException(); }

    public long getAndBitwiseAnd(Object owner, int index, long value) { throw new WrongMethodTypeException(); }

    public boolean getAndBitwiseXor(boolean value) { throw new WrongMethodTypeException(); }

    public byte getAndBitwiseXor(byte value) { throw new WrongMethodTypeException(); }

    public char getAndBitwiseXor(char value) { throw new WrongMethodTypeException(); }

    public short getAndBitwiseXor(short value) { throw new WrongMethodTypeException(); }

    public int getAndBitwiseXor(int value) { throw new WrongMethodTypeException(); }

    public long getAndBitwiseXor(long value) { throw new WrongMethodTypeException(); }

    public boolean getAndBitwiseXor(Object owner, boolean value) { throw new WrongMethodTypeException(); }

    public byte getAndBitwiseXor(Object owner, byte value) { throw new WrongMethodTypeException(); }

    public char getAndBitwiseXor(Object owner, char value) { throw new WrongMethodTypeException(); }

    public short getAndBitwiseXor(Object owner, short value) { throw new WrongMethodTypeException(); }

    public int getAndBitwiseXor(Object owner, int value) { throw new WrongMethodTypeException(); }

    public long getAndBitwiseXor(Object owner, long value) { throw new WrongMethodTypeException(); }

    public boolean getAndBitwiseXor(Object owner, int index, boolean value) { throw new WrongMethodTypeException(); }

    public byte getAndBitwiseXor(Object owner, int index, byte value) { throw new WrongMethodTypeException(); }

    public char getAndBitwiseXor(Object owner, int index, char value) { throw new WrongMethodTypeException(); }

    public short getAndBitwiseXor(Object owner, int index, short value) { throw new WrongMethodTypeException(); }

    public int getAndBitwiseXor(Object owner, int index, int value) { throw new WrongMethodTypeException(); }

    public long getAndBitwiseXor(Object owner, int index, long value) { throw new WrongMethodTypeException(); }

    protected abstract Class<?>[] coordinateClasses();

    public final List<Class<?>> coordinateTypes() {
        return Collections.unmodifiableList(Arrays.asList(coordinateClasses()));
    }

    private static Class<?>[] append(Class<?>[] coords, Class<?>... params) {
        Class<?>[] r = Arrays.copyOf(coords, coords.length + params.length);
        System.arraycopy(params, 0, r, coords.length, params.length);
        return r;
    }

    public final MethodType accessModeType(AccessMode accessMode) {
        Class<?>[] coords = coordinateClasses();
        switch (accessMode.accessType) {
            case GET:
                return MethodType.methodType(type, coords);
            case SET:
                return MethodType.methodType(void.class, append(coords, type));
            case COMPARE_AND_SET:
                return MethodType.methodType(boolean.class, append(coords, type, type));
            case COMPARE_AND_EXCHANGE:
                return MethodType.methodType(type, append(coords, type, type));
            case GET_AND_UPDATE:
                return MethodType.methodType(type, append(coords, type));
            default:
                throw new IllegalStateException();
        }
    }

    public final MethodHandle toMethodHandle(AccessMode accessMode) throws NoSuchMethodException, IllegalAccessException {
        MethodType mt = accessModeType(accessMode);
        List<Class<?>> params = mt.parameterList();
        Class<?>[] erased = new Class<?>[params.size()];
        for (int i = 0; i < erased.length; i++) {
            erased[i] = params.get(i).isPrimitive() ? params.get(i) : Object.class;
        }
        MethodType smt = MethodType.methodType(mt.returnType().isPrimitive() ? mt.returnType() : Object.class, erased);
        return lookup.findVirtual(J_L_I_VarHandle.class, fixMethodName(accessMode.stubName, Utils.getDescForClass(type)), smt).bindTo(this).asType(mt);
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
        GET_ACQUIRE("getAcquire", AccessType.GET, "getVolatile"),
        SET_RELEASE("setRelease", AccessType.SET, "setVolatile"),
        GET_OPAQUE("getOpaque", AccessType.GET, "getVolatile"),
        SET_OPAQUE("setOpaque", AccessType.SET, "setVolatile"),
        COMPARE_AND_SET("compareAndSet", AccessType.COMPARE_AND_SET),
        COMPARE_AND_EXCHANGE("compareAndExchange", AccessType.COMPARE_AND_EXCHANGE),
        COMPARE_AND_EXCHANGE_ACQUIRE("compareAndExchangeAcquire", AccessType.COMPARE_AND_EXCHANGE, "compareAndExchange"),
        COMPARE_AND_EXCHANGE_RELEASE("compareAndExchangeRelease", AccessType.COMPARE_AND_EXCHANGE, "compareAndExchange"),
        WEAK_COMPARE_AND_SET_PLAIN("weakCompareAndSetPlain", AccessType.COMPARE_AND_SET, "compareAndSet"),
        WEAK_COMPARE_AND_SET("weakCompareAndSet", AccessType.COMPARE_AND_SET, "compareAndSet"),
        WEAK_COMPARE_AND_SET_ACQUIRE("weakCompareAndSetAcquire", AccessType.COMPARE_AND_SET, "compareAndSet"),
        WEAK_COMPARE_AND_SET_RELEASE("weakCompareAndSetRelease", AccessType.COMPARE_AND_SET, "compareAndSet"),
        GET_AND_SET("getAndSet", AccessType.GET_AND_UPDATE),
        GET_AND_SET_ACQUIRE("getAndSetAcquire", AccessType.GET_AND_UPDATE, "getAndSet"),
        GET_AND_SET_RELEASE("getAndSetRelease", AccessType.GET_AND_UPDATE, "getAndSet"),
        GET_AND_ADD("getAndAdd", AccessType.GET_AND_UPDATE),
        GET_AND_ADD_ACQUIRE("getAndAddAcquire", AccessType.GET_AND_UPDATE, "getAndAdd"),
        GET_AND_ADD_RELEASE("getAndAddRelease", AccessType.GET_AND_UPDATE, "getAndAdd"),
        GET_AND_BITWISE_OR("getAndBitwiseOr", AccessType.GET_AND_UPDATE),
        GET_AND_BITWISE_OR_RELEASE("getAndBitwiseOrRelease", AccessType.GET_AND_UPDATE, "getAndBitwiseOr"),
        GET_AND_BITWISE_OR_ACQUIRE("getAndBitwiseOrAcquire", AccessType.GET_AND_UPDATE, "getAndBitwiseOr"),
        GET_AND_BITWISE_AND("getAndBitwiseAnd", AccessType.GET_AND_UPDATE),
        GET_AND_BITWISE_AND_RELEASE("getAndBitwiseAndRelease", AccessType.GET_AND_UPDATE, "getAndBitwiseAnd"),
        GET_AND_BITWISE_AND_ACQUIRE("getAndBitwiseAndAcquire", AccessType.GET_AND_UPDATE, "getAndBitwiseAnd"),
        GET_AND_BITWISE_XOR("getAndBitwiseXor", AccessType.GET_AND_UPDATE),
        GET_AND_BITWISE_XOR_RELEASE("getAndBitwiseXorRelease", AccessType.GET_AND_UPDATE, "getAndBitwiseXor"),
        GET_AND_BITWISE_XOR_ACQUIRE("getAndBitwiseXorAcquire", AccessType.GET_AND_UPDATE, "getAndBitwiseXor")
        ;

        static final Map<String, AccessMode> byName = new HashMap<>();

        static {
            for (AccessMode value : AccessMode.values()) {
                byName.put(value.methodName(), value);
            }
        }

        final String methodName;
        final AccessType accessType;
        final String stubName;

        AccessMode(String methodName, AccessType at) {
            this(methodName, at, methodName);
        }

        AccessMode(String methodName, AccessType at, String stubName) {
            this.methodName = methodName;
            this.accessType = at;
            this.stubName = stubName;
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
