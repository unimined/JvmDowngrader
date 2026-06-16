package xyz.wagyourtail.jvmdg.j15.stub.java_base;

import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Class {

    @Stub
    @JEP(371)
    public static boolean isHidden(Class<?> clazz) {
        return clazz.getClassLoader() instanceof J_L_I_MethodHandles$Lookup.HiddenClassLoader;
    }

}
