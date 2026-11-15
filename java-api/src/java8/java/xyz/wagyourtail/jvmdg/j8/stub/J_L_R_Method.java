package xyz.wagyourtail.jvmdg.j8.stub;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class J_L_R_Method {

    // getParameterCount lives in stub/all (view-aware, j7-safe via
    // getParameterTypes().length) — a second registration here would
    // collide on the same callsite member at the 52->51 boundary.

    @Stub
    public static boolean isDefault(Method self) {
        return (self.getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC && self.getDeclaringClass().isInterface();
    }

    // TODO: getAnnotatedReturnType

}
