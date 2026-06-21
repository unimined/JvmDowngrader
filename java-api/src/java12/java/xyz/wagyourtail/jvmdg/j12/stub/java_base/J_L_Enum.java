package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.j12.impl.EnumConstable;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.Optional;

@JEP(334)
public class J_L_Enum {

    @Stub
    public static <E extends Enum<E>> Optional<EnumDesc<E>> describeConstable(E self) {
        return new EnumConstable(self).describeConstable();
    }

    @Adapter("java/lang/Enum$EnumDesc")
    public static class EnumDesc<E extends Enum<E>> extends J_L_C_DynamicConstantDesc<E> {

        protected EnumDesc(J_L_C_ClassDesc constClass, String constName) {
            super(J_L_C_ConstantDescs.BSM_ENUM_CONSTANT, Objects.requireNonNull(constName), Objects.requireNonNull(constClass));
        }

        public static <E extends Enum<E>> EnumDesc<E> of(J_L_C_ClassDesc constClass, String constName) {
            return new EnumDesc<>(constClass, constName);
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
}
