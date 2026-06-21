package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.j12.impl.MethodHandleConstable;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.util.Optional;

public class J_L_I_MethodHandle {

    @Stub
    public static Optional<J_L_C_MethodHandleDesc> describeConstable(MethodHandle self) {
        return new MethodHandleConstable(self).describeConstable();
    }

}
