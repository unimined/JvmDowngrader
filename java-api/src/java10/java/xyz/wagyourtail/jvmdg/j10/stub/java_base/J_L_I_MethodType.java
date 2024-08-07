package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodType;

public class J_L_I_MethodType {


    // pkg-private -> public

    @Stub
    public static Class<?> lastParameterType(MethodType mt) {
        if (mt.parameterCount() == 0) {
            return void.class;
        }
        return mt.parameterType(mt.parameterCount() - 1);
    }

}
