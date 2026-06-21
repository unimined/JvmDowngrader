package xyz.wagyourtail.jvmdg.j12.impl;

import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_C_Constable;
import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_C_ConstantDesc;
import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_Class;
import xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_Enum;

import java.util.Optional;

public class EnumConstable<E extends Enum<E>> implements J_L_C_Constable {
    private final E e;

    public EnumConstable(E e) {
        this.e = e;
    }

    @Override
    public Optional<? extends J_L_C_ConstantDesc> describeConstable() {
        return J_L_Class.describeConstable(e.getDeclaringClass()).map(c -> J_L_Enum.EnumDesc.of(c, e.name()));
    }

}
