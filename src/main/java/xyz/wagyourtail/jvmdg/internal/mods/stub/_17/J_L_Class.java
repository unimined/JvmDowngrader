package xyz.wagyourtail.jvmdg.internal.mods.stub._17;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.lang.reflect.Field;

public class J_L_Class {

    @Stub(JavaVersion.VERSION_17)
    public static Class<?>[] getPermittedSubclasses(Class<?> clazz) {
        // check if the field exists
        try {
            Field f = clazz.getDeclaredField("permittedSubclasses$jvmdowngrader");
            String typeStr = (String) f.get(null);
            String[] types = typeStr.split(";");
            Class<?>[] classes = new Class<?>[types.length];
            for (int i = 0; i < types.length; i++) {
                classes[i] = Class.forName(types[i].replace('/', '.'), false, clazz.getClassLoader());
            }
            return classes;
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Stub(JavaVersion.VERSION_17)
    public static boolean isSealed(Class<?> clazz) {
        try {
            clazz.getDeclaredField("permittedSubclasses$jvmdowngrader");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
