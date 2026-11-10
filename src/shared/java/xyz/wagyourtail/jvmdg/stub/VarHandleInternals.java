package xyz.wagyourtail.jvmdg.stub;

import java.lang.reflect.Field;

/**
 * Cross-version introspection for the {@code java.lang.invoke.VarHandle}
 * stand-in implementation ({@code J_L_I_VarHandle}). Versioned stub
 * sourcesets can't reference each other's classes, so this shared interface
 * lets later versions inspect the implementation without a compile-time
 * dependency on it.
 */
public interface VarHandleInternals {

    /**
     * the backing field, or {@code null} for array/view handles
     */
    Field jvmdg$field();

    /**
     * the class the handle coordinates start from: declaring class for field
     * handles, array class for array handles
     */
    Class<?> jvmdg$ownerClass();

    /**
     * the type read/written through the handle
     */
    Class<?> jvmdg$varType();

    boolean jvmdg$isStatic();

    boolean jvmdg$isArray();

    boolean jvmdg$isByteArrayView();

}
