package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

@Adapter("java/lang/Enum$EnumDesc")
public class J_L_Enum$EnumDesc<E extends Enum<E>> extends J_L_C_DynamicConstantDesc<E> {

    protected J_L_Enum$EnumDesc(J_L_C_ClassDesc constClass, String constName) {
        super(J_L_C_ConstantDescs.BSM_ENUM_CONSTANT, Objects.requireNonNull(constName), Objects.requireNonNull(constClass));
    }

    public static <E extends Enum<E>> J_L_Enum$EnumDesc<E> of(J_L_C_ClassDesc constClass, String constName) {
        return new J_L_Enum$EnumDesc<>(constClass, constName);
    }

    @Override
    public E resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        return Enum.valueOf((Class<E>) constantType().resolveConstantDesc(lookup), constantName());
    }

    @Override
    public String toString() {
        return "EnumDesc[" + constantType().displayName() + "." + constantName() + "]";
    }
}
