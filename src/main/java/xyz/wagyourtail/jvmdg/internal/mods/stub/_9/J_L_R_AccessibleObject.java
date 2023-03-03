package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.lang.reflect.*;

public class J_L_R_AccessibleObject {
    @Stub(value = JavaVersion.VERSION_1_9)
    public static boolean trySetAccessible(AccessibleObject obj) {
        try {
            obj.setAccessible(true);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    @Stub(value = JavaVersion.VERSION_1_9, include = CallingClass.class)
    public static boolean canAccess(AccessibleObject obj, Object target) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (!Member.class.isInstance(obj)) {
            return obj.isAccessible();
        }

        Class<?> declaringClass = ((Member) obj).getDeclaringClass();
        int modifiers = ((Member) obj).getModifiers();
        if (!Modifier.isStatic(modifiers) &&
            (obj instanceof Method || obj instanceof Field)) {
            if (target == null) {
                throw new IllegalArgumentException("null object for " + obj);
            }
            // if this object is an instance member, the given object
            // must be a subclass of the declaring class of this reflected object
            if (!declaringClass.isAssignableFrom(obj.getClass())) {
                throw new IllegalArgumentException("object is not an instance of "
                    + declaringClass.getName());
            }
        } else if (target != null) {
            throw new IllegalArgumentException("non-null object for " + obj);
        }

        // access check is suppressed
        if (obj.isAccessible()) return true;

        Class<?> caller = CallingClass.INSTANCE.getCallingClass();
        Class<?> targetClass;
        if (obj instanceof Constructor) {
            targetClass = declaringClass;
        } else {
            targetClass = Modifier.isStatic(modifiers) ? null : obj.getClass();
        }


        //noinspection JavaReflectionMemberAccess it's there in j8
        Method checkAccess = AccessibleObject.class.getDeclaredMethod("checkAccess", Class.class, Class.class, Object.class, int.class);
        checkAccess.setAccessible(true);
        try {
            checkAccess.invoke(obj, caller, targetClass, target, modifiers);
            return true;
        } catch (InvocationTargetException e) {
            return false;
        }
    }

    @SuppressWarnings("removal")
    public static class CallingClass extends SecurityManager {
        public static final CallingClass INSTANCE = new CallingClass();

        public Class[] getCallingClasses() {
            return getClassContext();
        }

        public Class getCallingClass() {
            return getClassContext()[2];
        }
    }
}
