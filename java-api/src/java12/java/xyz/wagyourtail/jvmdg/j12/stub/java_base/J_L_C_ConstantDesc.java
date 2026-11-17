package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.j12.impl.*;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.JEP;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

@JEP(334)
@Adapter(value = "java/lang/constant/ConstantDesc", impls = {
    DoubleConstable.class,
    FloatConstable.class,
    IntegerConstable.class,
    LongConstable.class,
    StringConstable.class
})
public interface J_L_C_ConstantDesc extends J_L_C_Constable {

    // JDK: ConstantDesc extends Constable with a default describeConstable
    @Override
    default Optional<? extends J_L_C_ConstantDesc> describeConstable() {
        return Optional.of(this);
    }

    Object resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException;

}
