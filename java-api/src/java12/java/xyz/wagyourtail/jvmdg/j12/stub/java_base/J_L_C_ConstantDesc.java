package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.j12.impl.*;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Coerce;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandles;

@JEP(334)
@Adapter("java/lang/constant/ConstantDesc")
public interface J_L_C_ConstantDesc {

    static boolean jvmdg$instanceof(Object obj) {
        return obj instanceof J_L_C_ConstantDesc ||
            obj instanceof Double ||
            obj instanceof Float ||
            obj instanceof Integer ||
            obj instanceof Long ||
            obj instanceof String;

    }

    static J_L_C_ConstantDesc jvmdg$checkcast(Object self) {
        if (self instanceof J_L_C_ConstantDesc) {
            return (J_L_C_ConstantDesc) self;
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
        throw new IllegalArgumentException("Cannot cast " + self.getClass() + " to " + J_L_C_ConstantDesc.class);
    }

    Object resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException;

    @Stub(noSpecial = true)
    static Object resolveConstantDesc(@Coerce(J_L_C_ConstantDesc.class) Object obj, MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        return jvmdg$checkcast(obj).resolveConstantDesc(lookup);
    }
}
