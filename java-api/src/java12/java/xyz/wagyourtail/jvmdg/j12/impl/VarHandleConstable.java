package xyz.wagyourtail.jvmdg.j12.impl;

import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_C_ClassDesc;
import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_C_Constable;
import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_I_VarHandle;
import xyz.wagyourtail.jvmdg.stub.VarHandleInternals;

import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.Optional;

public class VarHandleConstable implements J_L_C_Constable {
    private final VarHandle varHandle;

    public VarHandleConstable(VarHandle varHandle) {
        this.varHandle = varHandle;
    }

    @Override
    public Optional<J_L_I_VarHandle.VarHandleDesc> describeConstable() {
        // VarHandleInternals.class.isInstance because instanceof refuses to
        // compile against the abstract VarHandle type
        if (!VarHandleInternals.class.isInstance(varHandle)) {
            // a real JDK VarHandle impl — reachable when only describeConstable
            // itself is stubbed (j9-j14 targets). Only array-element handles
            // expose enough through the public API (coordTypes [array, int])
            // to describe; field handles don't reveal the field name.
            java.util.List<Class<?>> coord = varHandle.coordinateTypes();
            if (coord.size() == 2 && coord.get(0).isArray() && coord.get(1) == int.class) {
                return Optional.of(J_L_I_VarHandle.VarHandleDesc.ofArray(descFor(coord.get(0))));
            }
            return Optional.empty();
        }
        VarHandleInternals vh = VarHandleInternals.class.cast(varHandle);
        if (vh.jvmdg$isByteArrayView()) {
            return Optional.empty();
        }
        if (vh.jvmdg$isArray()) {
            return Optional.of(J_L_I_VarHandle.VarHandleDesc.ofArray(descFor(vh.jvmdg$ownerClass())));
        }
        Field field = vh.jvmdg$field();
        if (field == null) {
            return Optional.empty();
        }
        J_L_C_ClassDesc declaring = J_L_C_ClassDesc.of(vh.jvmdg$ownerClass().getName());
        J_L_C_ClassDesc varType = descFor(field.getType());
        return Optional.of(vh.jvmdg$isStatic()
            ? J_L_I_VarHandle.VarHandleDesc.ofStaticField(declaring, field.getName(), varType)
            : J_L_I_VarHandle.VarHandleDesc.ofField(declaring, field.getName(), varType));
    }

    private static J_L_C_ClassDesc descFor(Class<?> clazz) {
        if (clazz.isArray()) {
            return J_L_C_ClassDesc.ofDescriptor(clazz.getName().replace('.', '/'));
        }
        if (clazz.isPrimitive()) {
            return J_L_C_ClassDesc.ofDescriptor(primitiveDesc(clazz));
        }
        return J_L_C_ClassDesc.of(clazz.getName());
    }

    private static String primitiveDesc(Class<?> clazz) {
        if (clazz == boolean.class) return "Z";
        if (clazz == byte.class) return "B";
        if (clazz == char.class) return "C";
        if (clazz == short.class) return "S";
        if (clazz == int.class) return "I";
        if (clazz == long.class) return "J";
        if (clazz == float.class) return "F";
        if (clazz == double.class) return "D";
        return "V";
    }

}
