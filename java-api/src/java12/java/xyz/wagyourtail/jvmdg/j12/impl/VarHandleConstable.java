package xyz.wagyourtail.jvmdg.j12.impl;

import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_C_Constable;
import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_I_VarHandle;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Optional;

public class VarHandleConstable implements J_L_C_Constable {
    private final VarHandle varHandle;

    public VarHandleConstable(VarHandle varHandle) {
        this.varHandle = varHandle;
    }

    @Override
    public Optional<J_L_I_VarHandle.VarHandleDesc> describeConstable() {
        throw MissingStubError.create();
    }

}
