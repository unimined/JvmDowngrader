package xyz.wagyourtail.jvmdg.j8.stub;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class J_L_R_Method {

    @Stub
    public static int getParameterCount(Method self) {
        return self.getParameterTypes().length;
    }

    @Stub
    public static boolean isDefault(Method self) {
        return (self.getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC && self.getDeclaringClass().isInterface();
    }

    // TODO: getAnnotatedReturnType

}
