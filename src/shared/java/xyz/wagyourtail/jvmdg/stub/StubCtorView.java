package xyz.wagyourtail.jvmdg.stub;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Source-API view over the {@code Constructor} objects the reflection table
 * hands out for stubbed {@code <init>} members. The returned object is the
 * <em>target</em> ctor's own {@code Constructor} — the real JDK member the
 * {@code @InitRewrite} retargets to — so call sites on {@code Constructor}
 * route here through the {@code java/lang/reflect/Constructor} stubs and,
 * for registered instances, present the member the source API would have:
 * the original (pre-retarget) parameter list, the JDK declaration flags,
 * and {@code newInstance} evaluated through the synthesized handle that
 * reproduces the callsite rewrite (kept args, {@code @InitArg} converters,
 * {@code @InitPre}/{@code @InitPost} hooks). Unregistered ctors pass
 * straight through.
 * <p>
 * The map keys on <em>identity</em>: {@code Constructor} objects are copied
 * per {@code getConstructor}/{@code getDeclaredConstructor} fetch, so only
 * the exact instance the table handed out carries the view — an equal ctor
 * obtained natively keeps native behavior, and two source ctors retargeting
 * to the same member can never collide on the shared object.
 */
public final class StubCtorView {

    private StubCtorView() {
    }

    static final class View {
        /**
         * the synthesized handle — {@code (origArgs) -> owner} — reproducing
         * the callsite rewrite: kept args permuted, converter slots filled
         * by their {@code @InitArg} methods, pre/post hooks folded around
         * the target {@code <init>}
         */
        final MethodHandle handle;
        /** the source (pre-retarget) parameter types, in runtime-facing types */
        final Class<?>[] params;
        /** the source ctor's signature as a {@code (origArgs)void} method type */
        final java.lang.invoke.MethodType type;
        /** JDK declaration flags carried by the C row */
        final int mods;

        View(MethodHandle handle, Class<?>[] params, java.lang.invoke.MethodType type, int mods) {
            this.handle = handle;
            this.params = params;
            this.type = type;
            this.mods = mods;
        }
    }

    private static final Map<Constructor<?>, View> VIEWS =
        Collections.synchronizedMap(new java.util.IdentityHashMap<Constructor<?>, View>());

    /**
     * Called by the reflection table as it hands a target {@code Constructor}
     * out — {@code handle} is the fully-composed {@code (origArgs) -> owner}
     * handle, {@code params} the source parameter types the view reports,
     * {@code type} the source signature as {@code (origArgs)void} for the
     * handle-info view, and {@code mods} the row's JDK declaration flags.
     */
    public static void register(Constructor<?> ctor, MethodHandle handle, Class<?>[] params,
                                java.lang.invoke.MethodType type, int mods) {
        VIEWS.put(ctor, new View(handle, params, type, mods));
    }

    /** the registered view's backing data, or {@code null} for an unregistered ctor */
    static View viewOf(Constructor<?> self) {
        return VIEWS.get(self);
    }

    /** whether {@code self} is a table-registered ctor — the gate the {@code unreflectConstructor} path uses */
    public static boolean hasView(Constructor<?> self) {
        return VIEWS.containsKey(self);
    }

    /**
     * {@code Constructor.newInstance} — view-registered ctors evaluate
     * through the synthesized handle, spreading the varargs array across
     * the source parameter count. Constructor-thrown exceptions surface in
     * {@link java.lang.reflect.InvocationTargetException}, matching the
     * native contract; argument-shape failures report {@link
     * IllegalArgumentException}; the original
     * {@link InstantiationException}/{@link IllegalAccessException} gates
     * are the caller lookup's business — the member was already resolved
     * when the view registered.
     */
    public static Object newInstance(Constructor<?> self, Object[] initargs)
        throws InstantiationException, IllegalAccessException,
        IllegalArgumentException, java.lang.reflect.InvocationTargetException {
        View v = VIEWS.get(self);
        if (v == null) {
            return self.newInstance(initargs);
        }
        if (initargs == null) {
            initargs = new Object[0];
        }
        try {
            return v.handle.asSpreader(Object[].class, v.params.length)
                .invokeWithArguments((Object) initargs);
        } catch (java.lang.invoke.WrongMethodTypeException | ClassCastException | NullPointerException e) {
            throw new IllegalArgumentException("wrong number of arguments", e);
        } catch (Throwable t) {
            // the ctor (or its rewrite hooks) threw — newInstance wraps in
            // InvocationTargetException just like the native path
            throw new java.lang.reflect.InvocationTargetException(t);
        }
    }

    /** {@code Constructor.getParameterCount} — the source signature's arity */
    public static int getParameterCount(Constructor<?> self) {
        View v = VIEWS.get(self);
        return v == null ? self.getParameterTypes().length : v.params.length;
    }

    /** {@code Constructor.getParameterTypes} — the source signature's types */
    public static Class<?>[] getParameterTypes(Constructor<?> self) {
        View v = VIEWS.get(self);
        return v == null ? self.getParameterTypes() : v.params.clone();
    }

    /** {@code Constructor.getModifiers} — the JDK declaration flags the source ctor carried */
    public static int getModifiers(Constructor<?> self) {
        View v = VIEWS.get(self);
        return v == null ? self.getModifiers() : v.mods & java.lang.reflect.Modifier.constructorModifiers();
    }
}
