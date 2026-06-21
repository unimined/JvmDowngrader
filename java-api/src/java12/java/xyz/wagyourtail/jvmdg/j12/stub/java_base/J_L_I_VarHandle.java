package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.j12.impl.VarHandleConstable;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Optional;

public class J_L_I_VarHandle {

    @Stub
    public static Optional<VarHandleDesc> describeConstable(VarHandle self) {
        return new VarHandleConstable(self).describeConstable();
    }

    @Adapter("java/lang/invoke/VarHandle$VarHandleDesc")
    public static class VarHandleDesc extends J_L_C_DynamicConstantDesc<VarHandle> {

        public enum Kind {
            FIELD(J_L_C_ConstantDescs.BSM_VARHANDLE_FIELD),
            STATIC_FIELD(J_L_C_ConstantDescs.BSM_VARHANDLE_STATIC_FIELD),
            ARRAY(J_L_C_ConstantDescs.BSM_VARHANDLE_ARRAY);

            private final J_L_C_DirectMethodHandleDesc bsm;
            Kind(J_L_C_DirectMethodHandleDesc bsm) {
                this.bsm = bsm;
            }

            J_L_C_ConstantDesc[] toBSMArgs(J_L_C_ClassDesc declaring, J_L_C_ClassDesc varType) {
                switch(this) {
                    case FIELD:
                    case STATIC_FIELD:
                        return new J_L_C_ConstantDesc[] { declaring, varType };
                    case ARRAY:
                        return new J_L_C_ConstantDesc[] { varType };
                }
                throw new IllegalArgumentException("Invalid kind: " + this);
            }
        }

        private final Kind kind;
        private final J_L_C_ClassDesc declaring;
        private final J_L_C_ClassDesc varType;

        private VarHandleDesc(Kind kind, String name, J_L_C_ClassDesc declaring, J_L_C_ClassDesc varType) {
            super(kind.bsm, name, J_L_C_ConstantDescs.CD_VarHandle, kind.toBSMArgs(declaring, varType));
            this.kind = kind;
            this.declaring = declaring;
            this.varType = varType;
        }

        public static VarHandleDesc ofField(J_L_C_ClassDesc declaring, String name, J_L_C_ClassDesc fieldType) {
            return new VarHandleDesc(Kind.FIELD, name, declaring, fieldType);
        }

        public static VarHandleDesc ofStaticField(J_L_C_ClassDesc declaring, String name, J_L_C_ClassDesc fieldType) {
            return new VarHandleDesc(Kind.STATIC_FIELD, name, declaring, fieldType);
        }

        public static VarHandleDesc ofArray(J_L_C_ClassDesc array) {
            return new VarHandleDesc(Kind.ARRAY, J_L_C_ConstantDescs.DEFAULT_NAME, array, array);
        }

        public J_L_C_ClassDesc varType() {
            return varType;
        }

        @Override
        public VarHandle resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
            switch (kind) {
                case FIELD:
                    return lookup.findVarHandle(declaring.resolveConstantDesc(lookup), constantName(), varType.resolveConstantDesc(lookup));
                case STATIC_FIELD:
                    return lookup.findStaticVarHandle(declaring.resolveConstantDesc(lookup), constantName(), varType.resolveConstantDesc(lookup));
                case ARRAY:
                    return MethodHandles.arrayElementVarHandle(varType.resolveConstantDesc(lookup));
            }
            throw new IllegalArgumentException("Invalid kind: " + kind);
        }

    }

}
