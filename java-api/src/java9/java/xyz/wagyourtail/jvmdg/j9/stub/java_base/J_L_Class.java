package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Class {

    @Stub(ref = @Ref("Ljava/lang/Class;"))
    public static Class<?> forName(J_L_Module module, String name) throws ClassNotFoundException {
        return Class.forName(name, true, module.getClassLoader());
    }

    @Stub
    public static J_L_Module getModule(Class<?> clazz) {
        return new J_L_Module(clazz.getClassLoader());
    }

    @Stub
    public static String getPackageName(Class<?> clazz) {
        String name = clazz.getName();
        int lastDot = name.lastIndexOf('.');
        if (clazz.isPrimitive() || clazz == void.class) {
            return "java.lang";
        }
        if (clazz.isArray()) {
            return getPackageName(clazz.getComponentType());
        }
        if (lastDot == -1) {
            return "";
        }
        return name.substring(0, lastDot);
    }

}
