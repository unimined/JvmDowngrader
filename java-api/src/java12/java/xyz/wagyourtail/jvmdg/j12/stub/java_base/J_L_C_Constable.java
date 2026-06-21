package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.j12.impl.*;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Coerce;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.Optional;

@JEP(334)
@Adapter("java/lang/constant/Constable")
public interface J_L_C_Constable {

    static boolean jvmdg$instanceof(Object obj) {
        return obj instanceof J_L_C_Constable ||
            obj instanceof Enum<?> ||
            obj instanceof Class<?> ||
            obj instanceof MethodHandle ||
            obj instanceof VarHandle ||
            obj instanceof MethodType ||
            obj instanceof Double ||
            obj instanceof Float ||
            obj instanceof Integer ||
            obj instanceof Long ||
            obj instanceof String;
    }

    static J_L_C_Constable jvmdg$checkcast(Object self) {
        if (self instanceof J_L_C_Constable) {
            return (J_L_C_Constable) self;
        }
        if (self instanceof Enum<?>) {
            return new EnumConstable((Enum<?>) self);
        }
        if (self instanceof Class<?>) {
            return new ClassConstable((Class<?>) self);
        }
        if (self instanceof MethodHandle) {
            return new MethodHandleConstable((MethodHandle) self);
        }
        if (self instanceof VarHandle) {
            return new VarHandleConstable((VarHandle) self);
        }
        if (self instanceof MethodType) {
            return new MethodTypeConstable((MethodType) self);
        }
        if (self instanceof Double) {
            return new DoubleConstable((Double) self);
        }
        if (self instanceof Float) {
            return new FloatConstable((Float) self);
        }
        if (self instanceof Integer) {
            return new IntegerConstable((Integer) self);
        }
        if (self instanceof Long) {
            return new LongConstable((Long) self);
        }
        if (self instanceof String) {
            return new StringConstable((String) self);
        }
        throw new IllegalArgumentException("Cannot cast " + self.getClass() + " to " + J_L_C_Constable.class);
    }

    Optional<? extends J_L_C_ConstantDesc> describeConstable();

    @Stub(noSpecial = true)
    static Optional<? extends J_L_C_ConstantDesc> describeConstable(@Coerce(J_L_C_Constable.class) Object obj) {
        return jvmdg$checkcast(obj).describeConstable();
    }


}
