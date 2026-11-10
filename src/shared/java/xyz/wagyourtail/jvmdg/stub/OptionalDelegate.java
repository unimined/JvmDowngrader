package xyz.wagyourtail.jvmdg.stub;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for {@link OptionalDependency}-flagged delegates ("when present"
 * stub support). {@link #get(Class)} returns a cached singleton when the
 * delegate's probe classes resolve and it instantiates cleanly, or
 * {@code null} when the optional library is absent — callers then throw
 * {@link #unsupported(Class, String)} (or degrade).
 * <p>
 * Probe failures are quiet by design: a missing library is the normal case.
 * Instantiation is wrapped in {@code catch (Throwable)} because the delegate's
 * own linkage references the optional types — verification-time
 * {@link NoClassDefFoundError} and native-load failures count as absent.
 */
public final class OptionalDelegate {

    private static final Object ABSENT = new Object();
    private static final Map<Class<?>, Object> CACHE = new ConcurrentHashMap<Class<?>, Object>();

    private OptionalDelegate() {
    }

    /**
     * @return the cached delegate instance, or {@code null} if the optional
     * dependency is absent. Idempotent, thread-safe.
     */
    public static <T> T get(Class<T> delegateClass) {
        Object o = CACHE.get(delegateClass);
        if (o == ABSENT) {
            return null;
        }
        if (o != null) {
            return delegateClass.cast(o);
        }
        synchronized (delegateClass) {
            o = CACHE.get(delegateClass);
            if (o != null) {
                return o == ABSENT ? null : delegateClass.cast(o);
            }
            T instance = tryInstantiate(delegateClass);
            CACHE.put(delegateClass, instance == null ? ABSENT : (Object) instance);
            return instance;
        }
    }

    /**
     * @return whether the delegate's optional dependency is present, without
     * surfacing the instance.
     */
    public static boolean available(Class<?> delegateClass) {
        return get(delegateClass) != null;
    }

    /**
     * Name-based {@link #get(Class)} for delegates the caller cannot link
     * statically (e.g. an api-jar implementation probed from shared code).
     *
     * @return the delegate cast to {@code type}, or {@code null} if the class
     * or its optional dependency is absent.
     */
    public static <T> T get(String delegateClassName, Class<T> type) {
        Class<?> cls;
        try {
            cls = Class.forName(delegateClassName, false, type.getClassLoader());
        } catch (Throwable t) {
            return null;
        }
        return type.cast(get(cls));
    }

    private static <T> T tryInstantiate(Class<T> delegateClass) {
        OptionalDependency dep = delegateClass.getAnnotation(OptionalDependency.class);
        if (dep == null) {
            return null;
        }
        for (String probe : dep.value()) {
            try {
                Class.forName(probe, false, delegateClass.getClassLoader());
            } catch (Throwable t) {
                return null;
            }
        }
        try {
            Constructor<T> ctor = delegateClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Build a UOE that tells the consumer exactly which dependency to add.
     */
    public static UnsupportedOperationException unsupported(Class<?> delegateClass, String capability) {
        OptionalDependency dep = delegateClass.getAnnotation(OptionalDependency.class);
        String name = dep != null && !dep.name().isEmpty() ? dep.name() : delegateClass.getSimpleName();
        String maven = dep != null && !dep.maven().isEmpty() ? dep.maven() : null;
        StringBuilder sb = new StringBuilder(capability).append(" requires optional dependency ").append(name);
        if (maven != null) {
            sb.append(" (add ").append(maven).append(" to the classpath)");
        } else {
            sb.append(" (not on the classpath)");
        }
        return new UnsupportedOperationException(sb.toString());
    }

    /**
     * Name-based {@link #unsupported(Class, String)} — resolves the
     * delegate's annotation by class name; falls back to the name itself.
     */
    public static UnsupportedOperationException unsupported(String delegateClassName, String capability) {
        try {
            return unsupported(Class.forName(delegateClassName), capability);
        } catch (Throwable t) {
            return new UnsupportedOperationException(capability + " requires optional dependency "
                + delegateClassName + " (not on the classpath)");
        }
    }

}
