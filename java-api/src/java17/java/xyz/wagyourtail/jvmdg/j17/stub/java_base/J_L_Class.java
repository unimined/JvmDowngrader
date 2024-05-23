package xyz.wagyourtail.jvmdg.j17.stub.java_base;


import xyz.wagyourtail.jvmdg.j17.PermittedSubClasses;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Class {

    @Stub
    public static Class<?>[] getPermittedSubclasses(Class<?> clazz) {
        if (!isSealed(clazz)) {
            return null;
        }
        return clazz.getAnnotation(PermittedSubClasses.class).value();
    }

    @Stub
    public static boolean isSealed(Class<?> clazz) {
        return clazz.isAnnotationPresent(PermittedSubClasses.class);
    }

}
