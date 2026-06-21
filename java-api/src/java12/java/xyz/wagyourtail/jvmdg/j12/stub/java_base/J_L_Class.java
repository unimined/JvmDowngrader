package xyz.wagyourtail.jvmdg.j12.stub.java_base;


import xyz.wagyourtail.jvmdg.j12.impl.ClassConstable;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Array;
import java.util.Optional;

@JEP(334)
public class J_L_Class {

    @Stub
    public static Optional<J_L_C_ClassDesc> describeConstable(Class<?> clazz) {
        return new ClassConstable(clazz).describeConstable();
    }

    @Stub
    public static String descriptorString(Class<?> clazz) {
        return new ClassConstable(clazz).descriptorString();
    }

    @Stub
    public static Class<?> componentType(Class<?> clazz) {
        return clazz.getComponentType();
    }

    @Stub
    public static Class<?> arrayType(Class<?> clazz) {
        return Array.newInstance(clazz, 0).getClass();
    }

}
