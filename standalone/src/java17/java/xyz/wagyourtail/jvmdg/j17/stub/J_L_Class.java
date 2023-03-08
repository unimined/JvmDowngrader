package xyz.wagyourtail.jvmdg.j17.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.lang.reflect.Field;

public class J_L_Class {

    @Stub(javaVersion = Opcodes.V17)
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

    @Stub(javaVersion = Opcodes.V17)
    public static boolean isSealed(Class<?> clazz) {
        try {
            clazz.getDeclaredField("permittedSubclasses$jvmdowngrader");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

}
