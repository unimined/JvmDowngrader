package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Parameter;
import java.util.Set;

public class J_L_R_Parameter {

    @Stub
    public static Set<J_L_R_AccessFlag> accessFlags(Parameter parameter) {
        return J_L_R_AccessFlag.maskToAccessFlags(parameter.getModifiers(), J_L_R_AccessFlag.Location.METHOD_PARAMETER);
    }

}
