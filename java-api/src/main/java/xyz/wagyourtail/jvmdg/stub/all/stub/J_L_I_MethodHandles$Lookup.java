package xyz.wagyourtail.jvmdg.stub.all.stub;

import xyz.wagyourtail.jvmdg.stub.reflection.ReflectionTable;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * {@link MethodHandles.Lookup} member lookups for stubbed members — thin
 * delegates into the ASM-free {@link ReflectionTable} handle layer. A
 * stubbed member answers with the indy-dispatch handle the callsites use
 * (override-preserving); misses fall through to the native lookup.
 * The callsite carries the authoring version in via {@code
 * downgradeVersion}'s trailing int.
 */
public class J_L_I_MethodHandles$Lookup {

    @Stub(downgradeVersion = true)
    public static Class<?> findClass(MethodHandles.Lookup self, String name, int origVersion) throws ClassNotFoundException {
        return ReflectionTable.findClass(self, name, origVersion);
    }

    @Stub(downgradeVersion = true)
    public static MethodHandle findVirtual(MethodHandles.Lookup self, Class<?> refc, String name,
                                           MethodType type, int origVersion)
        throws NoSuchMethodException, IllegalAccessException {
        return ReflectionTable.findVirtual(self, refc, name, type, origVersion);
    }

    @Stub(downgradeVersion = true)
    public static MethodHandle findSpecial(MethodHandles.Lookup self, Class<?> refc, String name,
                                           MethodType type, Class<?> caller, int origVersion)
        throws NoSuchMethodException, IllegalAccessException {
        return ReflectionTable.findSpecial(self, refc, name, type, caller, origVersion);
    }

    @Stub(downgradeVersion = true)
    public static MethodHandle findStatic(MethodHandles.Lookup self, Class<?> refc, String name,
                                          MethodType type, int origVersion)
        throws NoSuchMethodException, IllegalAccessException {
        return ReflectionTable.findStatic(self, refc, name, type, origVersion);
    }

    /**
     * {@code unreflect} needs no resolution help — the Method the table
     * hands out already is the receiver-first impl — but the produced
     * handle registers its source view so {@code revealDirect} doesn't
     * expose impl internals.
     */
    @Stub
    public static MethodHandle unreflect(MethodHandles.Lookup self, Method m)
        throws IllegalAccessException {
        return ReflectionTable.unreflect(self, m);
    }

    /**
     * {@code findConstructor} on a stubbed {@code <init>} answers a
     * synthesized {@code (origArgs) -> owner} handle reproducing the
     * callsite rewrite — kept/converter args plus {@code @InitPre}/
     * {@code @InitPost} hooks — instead of failing the lookup.
     */
    @Stub(downgradeVersion = true)
    public static MethodHandle findConstructor(MethodHandles.Lookup self, Class<?> refc,
                                               MethodType type, int origVersion)
        throws NoSuchMethodException, IllegalAccessException {
        return ReflectionTable.findConstructor(self, refc, type, origVersion);
    }

    /**
     * {@code unreflectConstructor} on a table-registered ctor hands back the
     * synthesized source-shaped handle (and registers its {@code
     * revealDirect} view); anything else resolves natively.
     */
    @Stub
    public static MethodHandle unreflectConstructor(MethodHandles.Lookup self,
                                                    java.lang.reflect.Constructor<?> c)
        throws IllegalAccessException {
        return ReflectionTable.unreflectConstructor(self, c);
    }

    /**
     * {@code unreflectSpecial} rejects static methods natively, but the
     * table hands out the {@code public static} impl — a view-registered
     * {@code Method} unreflects directly, which already is the non-virtual
     * call.
     */
    @Stub
    public static MethodHandle unreflectSpecial(MethodHandles.Lookup self, Method m, Class<?> caller)
        throws IllegalAccessException {
        return ReflectionTable.unreflectSpecial(self, m, caller);
    }

    /**
     * {@code revealDirect} on a handle the table produced answers the
     * source view (original owner/name/signature/{@code REF_*} kind);
     * anything else resolves natively.
     */
    @Stub
    public static java.lang.invoke.MethodHandleInfo revealDirect(MethodHandles.Lookup self, MethodHandle h) {
        return ReflectionTable.revealDirect(self, h);
    }

}
