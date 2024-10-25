package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.invoke.MethodHandles;

@Adapter("java/lang/constant/ConstantDesc")
public interface J_L_C_ConstantDesc {
    Object resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException;

}
