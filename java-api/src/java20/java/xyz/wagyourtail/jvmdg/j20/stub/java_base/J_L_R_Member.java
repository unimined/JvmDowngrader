package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Set;

public class J_L_R_Member {

    @Stub
    public static Set<J_L_R_AccessFlag> accessFlags(Member member) {
        J_L_R_AccessFlag.Location location;
        if (member instanceof Field) {
            location = J_L_R_AccessFlag.Location.FIELD;
        } else if (member instanceof Method || member instanceof Constructor) {
            location = J_L_R_AccessFlag.Location.METHOD;
        } else {
            location = J_L_R_AccessFlag.Location.CLASS;
        }
        return J_L_R_AccessFlag.maskToAccessFlags(member.getModifiers(), location);
    }

}
