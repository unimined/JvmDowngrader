package xyz.wagyourtail.jvmdg.j11.stub.java_base;

import xyz.wagyourtail.jvmdg.j11.NestHost;
import xyz.wagyourtail.jvmdg.j11.NestMembers;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class J_L_Class {

    @Stub
    public static Class<?> getNestHost(Class<?> clazz) throws IllegalAccessException, ClassNotFoundException {
        if (clazz.isPrimitive() || clazz.isArray()) {
            return clazz;
        }
        if (!clazz.isAnnotationPresent(NestHost.class)) {
            return clazz;
        }
        return clazz.getAnnotation(NestHost.class).value();
    }

    @Stub
    public static boolean isNestmateOf(Class<?> host, Class<?> clazz) throws ClassNotFoundException, IllegalAccessException {
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

    @Stub
    public static Class<?>[] getNestMembers(Class<?> clazz) throws IllegalAccessException, ClassNotFoundException {
        if (clazz.isPrimitive() || clazz.isArray()) {
            return new Class<?>[]{clazz};
        }
        Class<?> host = getNestHost(clazz);
        List<Class<?>> members = new ArrayList<>();
        members.add(host);
        if (!host.isAnnotationPresent(NestMembers.class)) {
            return members.toArray(new Class<?>[0]);
        }
        members.addAll(Arrays.asList(host.getAnnotation(NestMembers.class).value()));
        return members.toArray(new Class<?>[0]);
    }

}
