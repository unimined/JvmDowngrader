package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.j12.impl.*;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.JEP;

import java.util.Optional;

@JEP(334)
@Adapter(value = "java/lang/constant/Constable", impls = {
    EnumConstable.class,
    ClassConstable.class,
    MethodHandleConstable.class,
    VarHandleConstable.class,
    MethodTypeConstable.class
})
public interface J_L_C_Constable {

    Optional<? extends J_L_C_ConstantDesc> describeConstable();


}
