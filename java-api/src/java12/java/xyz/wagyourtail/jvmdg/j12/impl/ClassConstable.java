package xyz.wagyourtail.jvmdg.j12.impl;

import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_C_ClassDesc;
import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_C_Constable;
import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_I_TypeDescriptor;

import java.lang.reflect.Array;
import java.util.Optional;

public class ClassConstable implements J_L_C_Constable, J_L_I_TypeDescriptor.OfField<ClassConstable> {
    final Class<?> clazz;

    public ClassConstable(Class<?> t) {
        this.clazz = t;
    }

    @Override
    public Optional<J_L_C_ClassDesc> describeConstable() {
        return Optional.of(J_L_C_ClassDesc.ofDescriptor(descriptorString()));
    }

    @Override
    public boolean isArray() {
        return clazz.isArray();
    }

    @Override
    public boolean isPrimitive() {
        return clazz.isPrimitive();
    }

    @Override
    public ClassConstable componentType() {
        return new ClassConstable(clazz.getComponentType());
    }

    @Override
    public ClassConstable arrayType() {
        return new ClassConstable(Array.newInstance(clazz, 0).getClass());
    }

    @Override
    public String descriptorString() {
        if (clazz.isPrimitive()) {
            switch (clazz.getName()) {
                case "boolean":
                    return "Z";
                case "byte":
                    return "B";
                case "char":
                    return "C";
                case "short":
                    return "S";
                case "int":
                    return "I";
                case "long":
                    return "J";
                case "float":
                    return "F";
                case "double":
                    return "D";
                case "void":
                    return "V";
                default:
                    throw new InternalError("Unknown primitive type: " + clazz.getName());
            }
        }
        if (clazz.isArray()) {
            return clazz.getName().replace('.', '/');
        }
        return "L" + clazz.getName().replace('.', '/') + ";";
    }

}
