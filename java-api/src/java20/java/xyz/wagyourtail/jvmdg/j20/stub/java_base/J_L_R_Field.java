package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import java.lang.reflect.Field;
import java.util.Set;

public class J_L_R_Field {

    public static Set<J_L_R_AccessFlag> accessFlags(Field field) {
        return J_L_R_AccessFlag.maskToAccessFlags(field.getModifiers(), J_L_R_AccessFlag.Location.FIELD);
    }

}
