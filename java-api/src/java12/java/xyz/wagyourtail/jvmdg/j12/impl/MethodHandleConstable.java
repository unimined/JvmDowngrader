package xyz.wagyourtail.jvmdg.j12.impl;

import xyz.wagyourtail.jvmdg.j12.stub.java_base.*;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static java.lang.invoke.MethodHandleInfo.*;

public class MethodHandleConstable implements J_L_C_Constable {
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private final MethodHandle methodHandle;

    public MethodHandleConstable(MethodHandle methodHandle) {
        this.methodHandle = methodHandle;
    }

    @Override
    public Optional<J_L_C_MethodHandleDesc> describeConstable() {
        try {
            MethodHandleInfo info = IMPL_LOOKUP.revealDirect(methodHandle);
            boolean isInterface = info.getDeclaringClass().isInterface();
            J_L_C_ClassDesc owner = new ClassConstable(info.getDeclaringClass()).describeConstable().orElseThrow();
            J_L_C_MethodTypeDesc type = new MethodTypeConstable(info.getMethodType()).describeConstable().orElseThrow();
            String name = info.getName();

            switch (info.getReferenceKind()) {
                case REF_getField:
                    return Optional.of(J_L_C_MethodHandleDesc.ofField(J_L_C_DirectMethodHandleDesc.Kind.GETTER, owner, name, type.returnType()));
                case REF_putField:
                    return Optional.of(J_L_C_MethodHandleDesc.ofField(J_L_C_DirectMethodHandleDesc.Kind.SETTER, owner, name, type.parameterType(0)));
                case REF_getStatic:
                    return Optional.of(J_L_C_MethodHandleDesc.ofField(J_L_C_DirectMethodHandleDesc.Kind.STATIC_GETTER, owner, name, type.returnType()));
                case REF_putStatic:
                    return Optional.of(J_L_C_MethodHandleDesc.ofField(J_L_C_DirectMethodHandleDesc.Kind.STATIC_SETTER, owner, name, type.parameterType(0)));
                case REF_invokeStatic:
                    return Optional.of(J_L_C_MethodHandleDesc.ofMethod(isInterface ? J_L_C_DirectMethodHandleDesc.Kind.INTERFACE_STATIC : J_L_C_DirectMethodHandleDesc.Kind.STATIC, owner, name, type));
                case REF_invokeSpecial:
                    return Optional.of(J_L_C_MethodHandleDesc.ofMethod(isInterface ? J_L_C_DirectMethodHandleDesc.Kind.INTERFACE_SPECIAL : J_L_C_DirectMethodHandleDesc.Kind.SPECIAL, owner, name, type));
                case REF_invokeVirtual:
                    return Optional.of(J_L_C_MethodHandleDesc.ofMethod(J_L_C_DirectMethodHandleDesc.Kind.VIRTUAL, owner, name, type));
                case REF_invokeInterface:
                    return Optional.of(J_L_C_MethodHandleDesc.ofMethod(J_L_C_DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL, owner, name, type));
                case REF_newInvokeSpecial:
                    return Optional.of(J_L_C_MethodHandleDesc.ofMethod(J_L_C_DirectMethodHandleDesc.Kind.CONSTRUCTOR, owner, name, type));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
