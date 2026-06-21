package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.j12.impl.MethodTypeConstable;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodType;
import java.util.Optional;

public class J_L_I_MethodType {

    @Stub
    public static Optional<J_L_C_MethodTypeDesc> describeConstable(MethodType self) {
        return new MethodTypeConstable(self).describeConstable();
    }

    @Stub
    public static String descriptorString(MethodType self) {
        return self.toMethodDescriptorString();
    }

}
