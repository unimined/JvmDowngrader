package xyz.wagyourtail.jvmdg.j11.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Field;

public class J_L_Class {

    @Stub(opcVers = Opcodes.V11)
    public static Class<?> getNestHost(Class<?> clazz) throws IllegalAccessException, ClassNotFoundException {
        if (clazz.isPrimitive() || clazz.isArray()) {
            return clazz;
        }
        try {
            Field fd = clazz.getDeclaredField("jvmdowngrader$nestHost");
            String host = (String) fd.get(null);
            return Class.forName(host.replace('/', '.'));
        } catch (NoSuchFieldException e) {
            return clazz;
        }
    }

    @Stub(opcVers = Opcodes.V11)
    public static boolean isNestmateOf(Class<?> clazz) throws ClassNotFoundException, IllegalAccessException {
        Class<?> host = getNestHost(clazz);
        if (host == clazz) {
            return true;
        }
        Class<?>[] members = getNestMembers(host);
        for (Class<?> member : members) {
            if (member == clazz) {
                return true;
            }
        }
        return false;
    }

    @Stub(opcVers = Opcodes.V11)
    public static Class<?>[] getNestMembers(Class<?> clazz) throws IllegalAccessException, ClassNotFoundException {
        if (clazz.isPrimitive() || clazz.isArray()) {
            return new Class<?>[] { clazz };
        }
        try {
            Class<?> host = getNestHost(clazz);
            Field fd = host.getDeclaredField("jvmdowngrader$nestMembers");
            String[] members = ((String) fd.get(null)).split(":");
            Class<?>[] classes = new Class<?>[members.length + 1];
            classes[0] = host;
            for (int i = 0; i < members.length; i++) {
                classes[i + 1] = Class.forName(members[i].replace('/', '.'));
            }
            return classes;
        } catch (NoSuchFieldException e) {
            return new Class<?>[] { clazz };
        }
    }

}
