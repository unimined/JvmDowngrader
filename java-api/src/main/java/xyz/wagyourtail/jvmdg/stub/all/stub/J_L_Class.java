package xyz.wagyourtail.jvmdg.stub.all.stub;

import xyz.wagyourtail.jvmdg.stub.ReflectionTable;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Method;

/**
 * Caller-sensitive {@link Class} reflection stubs — thin delegates into the
 * ASM-free {@link ReflectionTable} runtime layer. The callsite carries the
 * authoring version in via {@code downgradeVersion}'s trailing int; the
 * one-arg {@code forName} also carries the containing class itself via
 * {@code callerClass}'s {@code ldc}, so resolution happens against the
 * callsite's own class loader rather than this class's.
 */
public class J_L_Class {

    //TODO: FIELD STUBS

    @Stub(ref = @Ref("java/lang/Class"), downgradeVersion = true, callerClass = true)
    public static Class<?> forName(String className, Class<?> caller, int origVersion) throws ClassNotFoundException {
        return ReflectionTable.forName(className, caller, origVersion);
    }

    @Stub(ref = @Ref("java/lang/Class"), downgradeVersion = true)
    public static Class<?> forName(String className, boolean initialize, ClassLoader loader, int origVersion) throws ClassNotFoundException {
        return ReflectionTable.forName(className, initialize, loader, origVersion);
    }

    @Stub(downgradeVersion = true)
    public static Method getMethod(Class<?> clazz, String name, Class<?>[] parameterTypes, int origVersion) throws NoSuchMethodException {
        return ReflectionTable.getMethod(clazz, name, parameterTypes, origVersion);
    }

    @Stub(downgradeVersion = true)
    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>[] parameterTypes, int origVersion) throws NoSuchMethodException {
        return ReflectionTable.getDeclaredMethod(clazz, name, parameterTypes, origVersion);
    }

    @Stub(downgradeVersion = true)
    public static java.lang.reflect.Constructor<?> getConstructor(Class<?> clazz, Class<?>[] parameterTypes, int origVersion) throws NoSuchMethodException {
        return ReflectionTable.getConstructor(clazz, parameterTypes, origVersion);
    }

    @Stub(downgradeVersion = true)
    public static java.lang.reflect.Constructor<?> getDeclaredConstructor(Class<?> clazz, Class<?>[] parameterTypes, int origVersion) throws NoSuchMethodException {
        return ReflectionTable.getDeclaredConstructor(clazz, parameterTypes, origVersion);
    }

    /**
     * {@code Class.getInterfaces} — the runtime list adjusted to the
     * authored view: {@code @GainedInterfaces}-recorded injections and
     * {@code $Ov$} markers filtered, {@code $Marker} entries replaced by
     * their stand-in, and interfaces the class gained in
     * {@code (runningJvm, origVersion]} appended as stand-ins. Stand-ins
     * resolve against the callsite's loader ({@code callerClass}) so the
     * returned {@code Class} objects are identity-equal to the ones the
     * callsite's own {@code ldc}s produce.
     */
    // owner derives from the self param — an explicit ref would keep self
    // in the member key and no callsite would ever match
    @Stub(downgradeVersion = true, callerClass = true)
    public static Class<?>[] getInterfaces(Class<?> clazz, Class<?> caller, int origVersion) {
        return ReflectionTable.getInterfaces(clazz, caller, origVersion);
    }

    /** {@code Class.getGenericInterfaces} — same adjustments on the generic view. */
    @Stub(downgradeVersion = true, callerClass = true)
    public static java.lang.reflect.Type[] getGenericInterfaces(Class<?> clazz, Class<?> caller, int origVersion) {
        return ReflectionTable.getGenericInterfaces(clazz, caller, origVersion);
    }

    /**
     * Real-only: the member table answers singular lookups for stubbed
     * members, but a whole-class enumeration can't honestly synthesize
     * missing members — deferred to CG-3d's filter machinery if needed.
     */
    @Stub
    public static Method[] getMethods(Class<?> clazz) {
        return clazz.getMethods();
    }

    @Stub
    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }

}
