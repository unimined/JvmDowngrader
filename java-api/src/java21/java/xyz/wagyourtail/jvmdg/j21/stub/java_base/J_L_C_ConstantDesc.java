package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * JDK 21 generified {@code ConstantDesc}: its subinterfaces redeclared
 * {@code resolveConstantDesc} covariantly, so the {@code (Lookup)<T>} callsite
 * shape javac emits on 21+ doesn't exist on the real interfaces below 65 —
 * where only the erasure-shaped {@code (Lookup)Object} is inherited from
 * {@code ConstantDesc}. These stubs cover the 56–64 window: below 56 the owner
 * type itself is remapped onto the j12 adapters (whose member sets carry the
 * covariant signatures), and at 65+ the member is real so callsites stay
 * native. Each impl delegates to the real inherited member and narrows the
 * result.
 */
public class J_L_C_ConstantDesc {

    @Stub
    public static Class<?> resolveConstantDesc(ClassDesc self, MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        return (Class<?>) self.resolveConstantDesc(lookup);
    }

    @Stub
    public static MethodType resolveConstantDesc(MethodTypeDesc self, MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        return (MethodType) self.resolveConstantDesc(lookup);
    }

    @Stub
    public static MethodHandle resolveConstantDesc(MethodHandleDesc self, MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        return (MethodHandle) self.resolveConstantDesc(lookup);
    }

}
