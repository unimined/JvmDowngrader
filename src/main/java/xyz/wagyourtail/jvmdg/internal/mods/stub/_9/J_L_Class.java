package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_Class {

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/lang/Class;")
    public static Class<?> forName(String name, J_L_Module module) throws ClassNotFoundException {
        return Class.forName(name, true, module.getClassLoader());
    }

    @Stub(JavaVersion.VERSION_1_9)
    public static J_L_Module getModule(Class<?> clazz) {
        return new J_L_Module(clazz.getClassLoader());
    }

    @Stub(JavaVersion.VERSION_1_9)
    public static String getPackageName(Class<?> clazz) {
        var name = clazz.getName();
        var lastDot = name.lastIndexOf('.');
        if (clazz.isPrimitive() || clazz == void.class) {
            return "java.lang";
        }
        if (clazz.isArray()) {
            return clazz.getComponentType().getPackageName();
        }
        if (lastDot == -1) {
            return "";
        }
        return name.substring(0, lastDot);
    }

}
