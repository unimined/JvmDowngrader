package xyz.wagyourtail.jvmdg.j12.impl;

import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_C_Constable;
import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_C_ConstantDesc;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

public class FloatConstable implements J_L_C_Constable, J_L_C_ConstantDesc {
    private final Float value;

    public FloatConstable(Float value) {
        this.value = value;
    }

    @Override
    public Optional<? extends J_L_C_ConstantDesc> describeConstable() {
        return Optional.of(this);
    }

    @Override
    public Float resolveConstantDesc(MethodHandles.Lookup lookup) {
        return value;
    }

}
