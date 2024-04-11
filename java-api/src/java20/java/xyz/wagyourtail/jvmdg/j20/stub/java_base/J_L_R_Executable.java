package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Executable;
import java.util.Set;

public class J_L_R_Executable {

    @Stub
    public static Set<J_L_R_AccessFlag> accessFlags(Executable executable) {
        return J_L_R_AccessFlag.maskToAccessFlags(executable.getModifiers(), J_L_R_AccessFlag.Location.METHOD);
    }

}
