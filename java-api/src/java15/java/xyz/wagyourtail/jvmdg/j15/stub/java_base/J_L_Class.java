package xyz.wagyourtail.jvmdg.j15.stub.java_base;

public class J_L_Class {

    public static boolean isHidden(Class<?> clazz) {
        return clazz.getClassLoader() instanceof J_L_I_MethodHandles$Lookup.HiddenClassLoader;
    }

}
