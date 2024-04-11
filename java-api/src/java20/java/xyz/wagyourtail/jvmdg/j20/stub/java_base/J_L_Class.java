package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Set;

public class J_L_Class {

    @Stub
    public static Set<J_L_R_AccessFlag> accessFlags(Class<?> cls) {
        var location = (cls.isMemberClass() || cls.isLocalClass() || cls.isAnonymousClass() || cls.isArray()) ? J_L_R_AccessFlag.Location.INNER_CLASS : J_L_R_AccessFlag.Location.CLASS;
        if (cls.isPrimitive()) {
            return Set.of(J_L_R_AccessFlag.ABSTRACT, J_L_R_AccessFlag.FINAL, J_L_R_AccessFlag.PUBLIC);
        } else if (cls.isArray()) {
            return accessFlags(cls.getComponentType());
        } else {
            int mods = cls.getModifiers();
            return J_L_R_AccessFlag.maskToAccessFlags(mods, location);
        }
    }

}
