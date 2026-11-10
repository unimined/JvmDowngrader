package xyz.wagyourtail.jvmdg.stub;

import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.EnumSet;

/**
 * Runtime enum-constant injection (the Forge {@code RuntimeEnumExtender}
 * approach). Inserts a real constant into a real enum's universe so
 * {@code values()}, {@code valueOf}, {@code switch}, {@code instanceof},
 * {@code ==}, {@code compareTo}, serialization and annotation-enum
 * materialization all behave natively.
 * <p>
 * Only the <em>universe</em> is extended — a static field named for the
 * constant can never be created (JVMTI can't add fields to loaded classes
 * either), so {@code getField("UNIX")}-style lookups keep failing.
 * <p>
 * Built on {@link Utils#getImplLookup()} and {@link Utils#getUnsafe()}; every
 * step has an independent fallback so partial support still succeeds. All
 * methods are idempotent and safe on {@code 8}-through-{@code 15} targets
 * (≥16 targets don't need injection — the constants exist natively, and
 * {@code valueOf} short-circuits before any internals are touched).
 */
public final class EnumExtender {

    private EnumExtender() {
    }

    /**
     * Inject {@code name} into {@code enumClass}'s universe, appended after the
     * existing constants.
     *
     * @return the constant (pre-existing or newly injected), or {@code null}
     * if injection was impossible on this JVM.
     */
    public static <T extends Enum<T>> T extend(Class<T> enumClass, String name) {
        T[] current = enumClass.getEnumConstants();
        return extend(enumClass, name, current == null ? 0 : current.length);
    }

    /**
     * Inject {@code name} into {@code enumClass}'s universe at {@code ordinal}.
     * Idempotent — returns the existing constant if already present.
     */
    public static synchronized <T extends Enum<T>> T extend(Class<T> enumClass, String name, int ordinal) {
        // already there? (also the native-API short-circuit)
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException ignored) {
        }

        T instance = makeInstance(enumClass, name, ordinal);
        if (instance == null) {
            return null;
        }
        if (!appendToValues(enumClass, instance)) {
            return null;
        }
        clearEnumCaches(enumClass);
        return instance;
    }

    /**
     * Build the constant via the enum's synthetic {@code (String, int)}
     * constructor through the trusted lookup — the ctor assigns real
     * {@code name}/{@code ordinal}, so no field writes are needed. Falls back
     * to {@code Unsafe.allocateInstance} + field writes, then plain reflection.
     */
    private static <T extends Enum<T>> T makeInstance(Class<T> enumClass, String name, int ordinal) {
        // preferred: IMPL_LOOKUP on the private synthetic ctor
        try {
            MethodHandles.Lookup lookup = Utils.getImplLookup();
            Constructor<T> ctor = findEnumCtor(enumClass);
            MethodHandle handle = lookup.findConstructor(enumClass, MethodType.methodType(void.class, ctor.getParameterTypes()));
            // javac adds synthetic trailing params for constant-specific bodies;
            // fill with type-appropriate defaults
            Object[] args = defaultCtorArgs(ctor.getParameterTypes());
            args[0] = name;
            args[1] = ordinal;
            return (T) handle.invokeWithArguments(args);
        } catch (Throwable ignored) {
        }
        // fallback: allocate + write Enum.name/ordinal directly
        try {
            Unsafe unsafe = Utils.getUnsafe();
            Object inst = unsafe.allocateInstance(enumClass);
            unsafe.putObject(inst, unsafe.objectFieldOffset(Enum.class.getDeclaredField("name")), name);
            unsafe.putInt(inst, unsafe.objectFieldOffset(Enum.class.getDeclaredField("ordinal")), ordinal);
            return enumClass.cast(inst);
        } catch (Throwable ignored) {
        }
        // last resort: reflective ctor + setAccessible (j8–j15 reachable)
        try {
            Constructor<T> ctor = findEnumCtor(enumClass);
            ctor.setAccessible(true);
            Object[] args = defaultCtorArgs(ctor.getParameterTypes());
            args[0] = name;
            args[1] = ordinal;
            return ctor.newInstance(args);
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Find the enum's synthetic constructor — always {@code (String, int, ...)}
     * with possible trailing synthetic params for constant-specific bodies.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> Constructor<T> findEnumCtor(Class<T> enumClass) throws NoSuchMethodException {
        for (Constructor<?> ctor : enumClass.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length >= 2 && params[0] == String.class && params[1] == int.class) {
                return (Constructor<T>) ctor;
            }
        }
        throw new NoSuchMethodException("no synthetic enum ctor on " + enumClass);
    }

    private static Object[] defaultCtorArgs(Class<?>[] params) {
        Object[] args = new Object[params.length];
        for (int i = 2; i < params.length; i++) {
            Class<?> p = params[i];
            if (!p.isPrimitive()) {
                args[i] = null;
            } else if (p == boolean.class) {
                args[i] = false;
            } else if (p == char.class) {
                args[i] = '\0';
            } else if (p == long.class) {
                args[i] = 0L;
            } else if (p == float.class) {
                args[i] = 0f;
            } else if (p == double.class) {
                args[i] = 0d;
            } else {
                args[i] = 0;
            }
        }
        return args;
    }

    /**
     * Grow the synthetic {@code $VALUES} array and write it back via Unsafe
     * (the field is {@code static final} — {@code findStaticSetter} and
     * {@code Field.set} both refuse it). Located by type, not name: javac emits
     * {@code $VALUES}, ECJ emits {@code ENUM$VALUES}.
     */
    private static <T extends Enum<T>> boolean appendToValues(Class<T> enumClass, T instance) {
        try {
            Unsafe unsafe = Utils.getUnsafe();
            for (Field field : enumClass.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !field.getType().isArray()
                    || field.getType().getComponentType() != enumClass) {
                    continue;
                }
                Object base = unsafe.staticFieldBase(field);
                long offset = unsafe.staticFieldOffset(field);
                T[] old = (T[]) unsafe.getObject(base, offset);
                T[] grown = java.util.Arrays.copyOf(old, old.length + 1);
                grown[old.length] = instance;
                unsafe.putObject(base, offset, grown);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Null the per-class enum caches so {@code valueOf}, {@code getEnumConstants},
     * {@code EnumSet} and {@code EnumMap} rebuild their universes. j8 keeps
     * {@code enumConstants}/{@code enumConstantDirectory} directly on
     * {@code Class}; 9+ moved them into {@code Class.ReflectionData} (soft-ref).
     */
    private static void clearEnumCaches(Class<?> enumClass) {
        try {
            Unsafe unsafe = Utils.getUnsafe();
            // j8 direct fields
            nullField(unsafe, enumClass, Class.class.getDeclaredField("enumConstants"));
            nullField(unsafe, enumClass, Class.class.getDeclaredField("enumConstantDirectory"));
            // 9+: Class.reflectionData (SoftReference<ReflectionData>)
            Field rdField = Class.class.getDeclaredField("reflectionData");
            Object rdRef = unsafe.getObject(enumClass, unsafe.objectFieldOffset(rdField));
            if (rdRef != null) {
                Object rd = ((java.lang.ref.SoftReference<?>) rdRef).get();
                if (rd != null) {
                    for (Field f : rd.getClass().getDeclaredFields()) {
                        if (f.getName().equals("enumConstants") || f.getName().equals("enumConstantDirectory")) {
                            nullField(unsafe, rd, f);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void nullField(Unsafe unsafe, Object base, Field field) {
        unsafe.putObject(base, unsafe.objectFieldOffset(field), null);
    }

    // ---- generic inject-then-delegate universe readers ---------------------
    // Wired by @ExtendEnum registration as excludeWithRuntime stubs: they
    // shadow the real universe readers so the target enum is extended before
    // the consumer can observe a stale universe on the non-agent path. The
    // trailing (Class, String) params are pushed by the transformer.

    /**
     * {@code <enum>.values()} shadow.
     */
    public static Enum<?>[] values(Class<?> target, String member) {
        extendEnum(target, member);
        return (Enum<?>[]) target.getEnumConstants();
    }

    /**
     * {@code <enum>.valueOf(String)} shadow.
     */
    public static Enum<?> valueOf(String name, Class<?> target, String member) {
        extendEnum(target, member);
        return enumValueOf(target, name);
    }

    /**
     * {@code Enum.valueOf(Class, String)} shadow — injects only when the
     * queried class is the extended enum.
     */
    public static Enum<?> enumValueOf(Class<?> clazz, String name, Class<?> target, String member) {
        if (clazz == target) {
            extendEnum(target, member);
        }
        return enumValueOf(clazz, name);
    }

    /**
     * {@code Class.getEnumConstants()} shadow.
     */
    public static Enum<?>[] getEnumConstants(Class<?> clazz, Class<?> target, String member) {
        if (clazz == target) {
            extendEnum(target, member);
        }
        return (Enum<?>[]) clazz.getEnumConstants();
    }

    /**
     * {@code EnumSet.noneOf(Class)} shadow.
     */
    public static EnumSet<?> enumSetNoneOf(Class<?> clazz, Class<?> target, String member) {
        if (clazz == target) {
            extendEnum(target, member);
        }
        return EnumSet.noneOf(clazz.asSubclass(Enum.class));
    }

    /**
     * {@code EnumSet.allOf(Class)} shadow.
     */
    public static EnumSet<?> enumSetAllOf(Class<?> clazz, Class<?> target, String member) {
        if (clazz == target) {
            extendEnum(target, member);
        }
        return EnumSet.allOf(clazz.asSubclass(Enum.class));
    }

    /**
     * {@code new EnumMap(Class)} shadow.
     */
    public static EnumMap<?, ?> enumMap(Class<?> clazz, Class<?> target, String member) {
        if (clazz == target) {
            extendEnum(target, member);
        }
        return new EnumMap(clazz.asSubclass(Enum.class));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Enum<?> enumValueOf(Class<?> clazz, String name) {
        return Enum.valueOf((Class) clazz, name);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void extendEnum(Class<?> target, String member) {
        extend((Class) target, member);
    }

}
