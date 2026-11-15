package xyz.wagyourtail.jvmdg.stub;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Source-API view over the {@code Method} objects the reflection table
 * hands out for stubbed members. The returned object is the impl's own
 * {@code Method} — a {@code public static} handler whose signature carries
 * the receiver in arg0 — so call sites on {@code Method} route here through
 * the {@code java/lang/reflect/Method} stubs and, for registered impls,
 * present the member the source API would have: instance-shaped invoke,
 * receiver shifted back out of the args, original declaring class, name and
 * modifiers. Methods never registered by the table pass straight through —
 * the lookup is one {@link IdentityHashMap} probe.
 */
public final class StubMethodView {

    private StubMethodView() {
    }

    static final class View {
        /** the runtime-facing declaring class (the source owner's stand-in) */
        final Class<?> declaring;
        /** the member name the source API reports */
        final String name;
        /** arg0 of the impl is the receiver */
        final boolean instance;
        /** JDK declaration flags carried by the M row */
        final int mods;
        /** the source signature in runtime-facing types — {@code getParameterTypes}/handle-info view */
        final java.lang.invoke.MethodType type;
        /**
         * trailing args the impl handler takes past the member's own
         * signature — transform-injected values ({@code downgradeVersion}'s
         * int, {@code callerClass}'s Class) resolved at registration to the
         * same values the callsite {@code ldc}s would have carried
         */
        final Object[] tail;

        View(Class<?> declaring, String name, boolean instance, int mods,
             java.lang.invoke.MethodType type, Object[] tail) {
            this.declaring = declaring;
            this.name = name;
            this.instance = instance;
            this.mods = mods;
            this.type = type;
            this.tail = tail;
        }
    }

    /**
     * impl Method → its source view. Identity-keyed: {@code Method.equals}
     * is member-structural, and reflecting on the api impl class directly
     * must not conjure a view it never registered — only the instance the
     * table hands out carries one.
     */
    private static final Map<Method, View> VIEWS =
        Collections.synchronizedMap(new IdentityHashMap<Method, View>());

    /**
     * Called by the reflection table as it hands an impl {@code Method} out.
     * {@code declaring} is pre-resolved (the owner's runtime stand-in —
     * what {@code getDeclaringClass} reports); a null falls back to the
     * impl's own declaring class. {@code type} is the source signature in
     * runtime-facing types; {@code tail} the resolved transform-injected
     * args the impl takes past it (may be empty, not null).
     */
    public static void register(Method impl, Class<?> declaring, String name, boolean instance,
                                int mods, java.lang.invoke.MethodType type, Object[] tail) {
        VIEWS.put(impl, new View(declaring == null ? impl.getDeclaringClass() : declaring,
            name, instance, mods, type, tail));
    }

    /** the registered view's backing data — the handle-view layer shares it */
    static View viewOf(Method self) {
        return VIEWS.get(self);
    }

    /** {@code Method.invoke} — instance views shift the receiver back into arg0; transform args append at the tail */
    public static Object invoke(Method self, Object recv, Object[] args)
        throws InvocationTargetException, IllegalAccessException {
        View v = VIEWS.get(self);
        if (v == null) {
            return self.invoke(recv, args);
        }
        int argc = (args == null ? 0 : args.length) + (v.instance ? 1 : 0);
        Object[] a = new Object[argc + v.tail.length];
        int i = 0;
        if (v.instance) {
            a[i++] = recv;
        }
        if (args != null) {
            for (Object arg : args) {
                a[i++] = arg;
            }
        }
        for (Object t : v.tail) {
            a[i++] = t;
        }
        return self.invoke(null, a);
    }

    /** whether {@code self} is a table-registered impl {@code Method} — the gate the {@code unreflect*} path uses */
    public static boolean hasView(Method self) {
        return VIEWS.containsKey(self);
    }

    /** {@code Method.getDeclaringClass} — the source owner's runtime stand-in */
    public static Class<?> getDeclaringClass(Method self) {
        View v = VIEWS.get(self);
        return v == null ? self.getDeclaringClass() : v.declaring;
    }

    /** {@code Method.getName} — the member name, not the handler's */
    public static String getName(Method self) {
        View v = VIEWS.get(self);
        return v == null ? self.getName() : v.name;
    }

    /** {@code Method.getParameterCount} — the source signature's arity */
    public static int getParameterCount(Method self) {
        View v = VIEWS.get(self);
        // getParameterTypes().length rather than self.getParameterCount():
        // the member itself is j8+, so the all-version impl must stay
        // reachable below 52 where this stub is the only shim.
        return v == null ? self.getParameterTypes().length : v.type.parameterCount();
    }

    /** {@code Method.getParameterTypes} — the source signature's types */
    public static Class<?>[] getParameterTypes(Method self) {
        View v = VIEWS.get(self);
        return v == null ? self.getParameterTypes() : v.type.parameterArray();
    }

    /** {@code Method.getReturnType} — the source signature's return type */
    public static Class<?> getReturnType(Method self) {
        View v = VIEWS.get(self);
        return v == null ? self.getReturnType() : v.type.returnType();
    }

    /** {@code Method.getModifiers} — the JDK declaration flags the member carried */
    public static int getModifiers(Method self) {
        View v = VIEWS.get(self);
        return v == null ? self.getModifiers() : v.mods & Modifier.methodModifiers();
    }
}
