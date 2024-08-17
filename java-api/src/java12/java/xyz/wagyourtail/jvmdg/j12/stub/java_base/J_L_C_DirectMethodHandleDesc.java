package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;

@Adapter("java/lang/constant/DirectMethodHandleDesc")
public interface J_L_C_DirectMethodHandleDesc extends J_L_C_MethodHandleDesc {

    @Adapter("java/lang/constant/DirectMethodHandleDesc$Kind")
    enum Kind {
        STATIC(Opcodes.H_INVOKESTATIC),
        INTERFACE_STATIC(Opcodes.H_INVOKESTATIC, true),
        VIRTUAL(Opcodes.H_INVOKEVIRTUAL),
        INTERFACE_VIRTUAL(Opcodes.H_INVOKEINTERFACE, true),
        SPECIAL(Opcodes.H_INVOKESPECIAL),
        INTERFACE_SPECIAL(Opcodes.H_INVOKESPECIAL, true),
        CONSTRUCTOR(Opcodes.H_NEWINVOKESPECIAL),
        GETTER(Opcodes.H_GETFIELD),
        SETTER(Opcodes.H_PUTFIELD),
        STATIC_GETTER(Opcodes.H_GETSTATIC),
        STATIC_SETTER(Opcodes.H_PUTSTATIC),
        ;

        public final int refKind;
        public final boolean isInterface;

        Kind(int refKind) {
            this.refKind = refKind;
            this.isInterface = false;
        }

        Kind(int refKind, boolean isInterface) {
            this.refKind = refKind;
            this.isInterface = isInterface;
        }

        public static Kind valueOf(int refKind) {
            return valueOf(refKind, refKind == Opcodes.H_INVOKEINTERFACE);
        }

        public static Kind valueOf(int refKind, boolean isInterface) {
            if (refKind < 1) throw new IllegalArgumentException();
            return entries[((refKind - 1) << 1) | (isInterface ? 1 : 0)];
        }

        private static final Kind[] entries = new Kind[9 * 2 + 1];
        static {
            for (Kind kind : values()) {
                int index = ((kind.refKind - 1) << 1) | (kind.isInterface ? 1 : 0);
                if (entries[index] != null) {
                    throw new IllegalStateException();
                }
                entries[index] = kind;
            }
        }

        boolean isVirtual() {
            switch (this) {
                case VIRTUAL:
                case SPECIAL:
                case INTERFACE_SPECIAL:
                case INTERFACE_VIRTUAL:
                    return true;
            }
            return false;
        }
    }

    Kind kind();

    int refKind();

    boolean isOwnerInterface();

    J_L_C_ClassDesc owner();

    String methodName();

    String lookupDescriptor();

    class DirectMethodHandleDescImpl implements J_L_C_DirectMethodHandleDesc {
        private final Kind kind;
        private final J_L_C_ClassDesc owner;
        private final String methodName;
        private final J_L_C_MethodTypeDesc type;

        DirectMethodHandleDescImpl(
            Kind kind,
            J_L_C_ClassDesc owner,
            String name,
            J_L_C_MethodTypeDesc type
        ) {
            this.kind = kind;
            this.owner = owner;
            this.methodName = name;
            if (kind.isVirtual()) {
                this.type = type.insertParameterTypes(0, owner);
            } else if (kind == Kind.CONSTRUCTOR) {
                this.type = type.changeReturnType(owner);
            } else {
                this.type = type;
            }
        }

        @Override
        public Kind kind() {
            return kind;
        }

        @Override
        public int refKind() {
            return kind.refKind;
        }

        @Override
        public boolean isOwnerInterface() {
            return kind.isInterface;
        }

        @Override
        public J_L_C_ClassDesc owner() {
            return owner;
        }

        @Override
        public String methodName() {
            return methodName;
        }

        @Override
        public J_L_C_MethodTypeDesc invocationType() {
            return type;
        }

        @Override
        public String lookupDescriptor() {
            switch (kind) {
                case VIRTUAL:
                case SPECIAL:
                case INTERFACE_SPECIAL:
                case INTERFACE_VIRTUAL:
                    return type.dropParameterTypes(0, 1).descriptorString();
                case STATIC:
                case INTERFACE_STATIC:
                    return type.descriptorString();
                case CONSTRUCTOR:
                    return type.changeReturnType(J_L_C_ConstantDescs.CD_void).descriptorString();
                case GETTER:
                case STATIC_GETTER:
                    return type.returnType().descriptorString();
                case STATIC_SETTER:
                    return type.parameterType(0).descriptorString();
                case SETTER:
                    return type.parameterType(1).descriptorString();
                default:
                    throw new IllegalStateException("unreachable");
            }
        }

        @Override
        public MethodHandle resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
            Class<?> owner = this.owner.resolveConstantDesc(lookup);
            MethodType type = (MethodType) this.invocationType().resolveConstantDesc(lookup);
            switch (kind) {
                case STATIC:
                case INTERFACE_STATIC:
                    return lookup.findStatic(owner, methodName, type);
                case VIRTUAL:
                case INTERFACE_VIRTUAL:
                    return lookup.findVirtual(owner, methodName, type.dropParameterTypes(0, 1));
                case SPECIAL:
                case INTERFACE_SPECIAL:
                    return lookup.findSpecial(owner, methodName, type.dropParameterTypes(0, 1), lookup.lookupClass());
                case CONSTRUCTOR:
                    return lookup.findConstructor(owner, type.changeReturnType(void.class));
                case GETTER:
                    return lookup.findGetter(owner, methodName, type.returnType());
                case STATIC_GETTER:
                    return lookup.findStaticGetter(owner, methodName, type.returnType());
                case SETTER:
                    return lookup.findSetter(owner, methodName, type.parameterType(1));
                case STATIC_SETTER:
                    return lookup.findStaticSetter(owner, methodName, type.parameterType(0));
                default:
                    throw new IllegalStateException("unreachable");
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DirectMethodHandleDescImpl that = (DirectMethodHandleDescImpl) o;
            return kind == that.kind && Objects.equals(owner, that.owner) && Objects.equals(methodName, that.methodName) && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, owner, methodName, type);
        }

        @Override
        public String toString() {
            return "MethodHandleDesc[" + kind + "/" + owner + "::" + methodName + type + "]";
        }

    }

}
