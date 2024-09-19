package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Adapter("java/lang/constant/MethodHandleDesc")
public interface J_L_C_MethodHandleDesc extends J_L_C_ConstantDesc {

    static J_L_C_DirectMethodHandleDesc of(
        J_L_C_DirectMethodHandleDesc.Kind kind,
        J_L_C_ClassDesc owner,
        String name,
        String lookupDesc
    ) {
        if (kind.refKind < 5) {
            return ofField(kind, owner, name, J_L_C_ClassDesc.ofDescriptor(lookupDesc));
        }
        return new J_L_C_DirectMethodHandleDesc.DirectMethodHandleDescImpl(
            kind,
            owner,
            name,
            J_L_C_MethodTypeDesc.ofDescriptor(lookupDesc)
        );
    }

    static J_L_C_DirectMethodHandleDesc ofMethod(
        J_L_C_DirectMethodHandleDesc.Kind kind,
        J_L_C_ClassDesc owner,
        String name,
        J_L_C_MethodTypeDesc type
    ) {
        if (kind.refKind < 5) {
            throw new IllegalArgumentException("Kind must be a method");
        }
        return new J_L_C_DirectMethodHandleDesc.DirectMethodHandleDescImpl(kind, owner, name, type);
    }

    static J_L_C_DirectMethodHandleDesc ofField(
        J_L_C_DirectMethodHandleDesc.Kind kind,
        J_L_C_ClassDesc owner,
        String name,
        J_L_C_ClassDesc type
    ) {
        J_L_C_MethodTypeDesc mtd;
        switch (kind) {
            case GETTER:
                mtd = J_L_C_MethodTypeDesc.of(type, owner);
                break;
            case SETTER:
                mtd = J_L_C_MethodTypeDesc.of(J_L_C_ConstantDescs.CD_void, owner, type);
                break;
            case STATIC_GETTER:
                mtd = J_L_C_MethodTypeDesc.of(type);
                break;
            case STATIC_SETTER:
                mtd = J_L_C_MethodTypeDesc.of(J_L_C_ConstantDescs.CD_void, type);
                break;
            default:
                throw new IllegalArgumentException("Kind must be a field");
        }
        return new J_L_C_DirectMethodHandleDesc.DirectMethodHandleDescImpl(
            kind,
            owner,
            name,
            mtd
        );
    }

    static J_L_C_DirectMethodHandleDesc ofConstructor(J_L_C_ClassDesc owner, J_L_C_ClassDesc... args) {
        return J_L_C_MethodHandleDesc.ofMethod(J_L_C_DirectMethodHandleDesc.Kind.CONSTRUCTOR, owner, "_", J_L_C_MethodTypeDesc.of(J_L_C_ConstantDescs.CD_void, args));
    }

    default J_L_C_MethodHandleDesc asType(J_L_C_MethodTypeDesc type) {
        return invocationType().equals(type) ? this : new AsType(this, type);
    }

    J_L_C_MethodTypeDesc invocationType();

    @Override
    boolean equals(Object o);

    class AsType extends J_L_C_DynamicConstantDesc<MethodHandle> implements J_L_C_MethodHandleDesc {
        private final J_L_C_MethodHandleDesc underlying;
        private final J_L_C_MethodTypeDesc type;

        AsType(J_L_C_MethodHandleDesc underlying, J_L_C_MethodTypeDesc type) {
            super(
                J_L_C_ConstantDescs.BSM_INVOKE,
                "_",
                J_L_C_ConstantDescs.CD_MethodHandle,
                J_L_C_ConstantDescs.AS_TYPE,
                underlying,
                type
            );
            this.underlying = underlying;
            this.type = type;
        }

        @Override
        public J_L_C_MethodTypeDesc invocationType() {
            return type;
        }

        @Override
        public MethodHandle resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
            MethodHandle h = (MethodHandle) underlying.resolveConstantDesc(lookup);
            MethodType t = (MethodType) type.resolveConstantDesc(lookup);
            return h.asType(t);
        }

        @Override
        public String toString() {
            return underlying + ".asType" + type.toString();
        }

    }

}
