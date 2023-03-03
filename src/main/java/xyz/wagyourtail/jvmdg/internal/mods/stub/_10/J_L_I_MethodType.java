package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.lang.invoke.MethodType;

public class J_L_I_MethodType {


    @Stub(JavaVersion.VERSION_1_10)
    public static Class<?> lastParameterType(MethodType mt) {
        if (mt.parameterCount() == 0) {
            return void.class;
        }
        return mt.parameterType(mt.parameterCount() - 1);
    }
}
