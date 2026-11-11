package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.version.JEP;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;

/**
 * Declarative reference to a handler {@link Method} — the data a registry
 * entry actually needs ({@code member} owner/name/desc, {@code modifiers} for
 * validation, {@code itf} for the {@code INVOKESTATIC} interface bit) without
 * eagerly holding the reflective object, so a serialized index can
 * reconstruct it. {@link #method()} resolves the {@code Method} lazily on
 * first reflective use ({@code @Modify} invocation, api-side masking); the
 * default lookup is {@code Class.forName} in this class's loader — a
 * shaded api jar must supply a remapped lookup when building entries.
 */
public class MethodRef {

    public final FullyQualifiedMemberNameAndDesc member;
    public final int modifiers;
    /** whether the declaring type is an interface (INVOKESTATIC itf bit) */
    public final boolean itf;
    /** {@code @JEP} tags filtering this handler — declaring-class ∪ method (declared only, matching scan semantics) */
    public final int[] jeps;
    private final Lazy<Method> method;

    public MethodRef(final Method m) {
        this.member = FullyQualifiedMemberNameAndDesc.of(m);
        this.modifiers = m.getModifiers();
        this.itf = m.getDeclaringClass().isInterface();
        JEP classJep = m.getDeclaringClass().getAnnotation(JEP.class);
        JEP methodJep = m.getAnnotation(JEP.class);
        this.jeps = jeps(classJep, methodJep);
        this.method = new Lazy<Method>() {
            @Override
            protected Method init() {
                return m;
            }
        };
    }

    public MethodRef(final FullyQualifiedMemberNameAndDesc member, int modifiers, boolean itf, int[] jeps) {
        this(member, modifiers, itf, jeps, null);
    }

    /** index-loading variant — explicit lookup for api jars whose stub classes are shaded/prefixed */
    public MethodRef(final FullyQualifiedMemberNameAndDesc member, int modifiers, boolean itf, int[] jeps, final Function<String, Class<?>> classLookup) {
        this.member = member;
        this.modifiers = modifiers;
        this.itf = itf;
        this.jeps = jeps;
        this.method = new Lazy<Method>() {
            @Override
            protected Method init() {
                return resolve(member, classLookup);
            }
        };
    }

    private static int[] jeps(JEP classJep, JEP methodJep) {
        if (classJep == null) {
            return methodJep == null ? new int[0] : new int[]{methodJep.value()};
        }
        if (methodJep == null) {
            return new int[]{classJep.value()};
        }
        return classJep.value() == methodJep.value() ? new int[]{classJep.value()}
            : new int[]{classJep.value(), methodJep.value()};
    }

    /** the reflective method — resolves (and loads the declaring class) on first call */
    public Method method() {
        return method.get();
    }

    private static Method resolve(FullyQualifiedMemberNameAndDesc member, Function<String, Class<?>> lookup) {
        try {
            Class<?> owner = lookup != null ? lookup.apply(member.getOwner().getClassName())
                : Class.forName(member.getOwner().getClassName());
            Type[] params = member.getDesc().getArgumentTypes();
            Class<?>[] paramClasses = new Class<?>[params.length];
            for (int i = 0; i < params.length; i++) {
                paramClasses[i] = toClass(params[i], lookup);
            }
            return owner.getDeclaredMethod(member.getName(), paramClasses);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot resolve stub handler " + member, e);
        }
    }

    private static Class<?> toClass(Type t, Function<String, Class<?>> lookup) throws ClassNotFoundException {
        switch (t.getSort()) {
            case Type.VOID: return void.class;
            case Type.BOOLEAN: return boolean.class;
            case Type.CHAR: return char.class;
            case Type.BYTE: return byte.class;
            case Type.SHORT: return short.class;
            case Type.INT: return int.class;
            case Type.FLOAT: return float.class;
            case Type.LONG: return long.class;
            case Type.DOUBLE: return double.class;
            case Type.ARRAY: return lookup != null ? lookup.apply(t.getInternalName().replace('/', '.'))
                : Class.forName(t.getInternalName().replace('/', '.'));
            default: return lookup != null ? lookup.apply(t.getClassName()) : Class.forName(t.getClassName());
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MethodRef && member.equals(((MethodRef) o).member);
    }

    @Override
    public int hashCode() {
        return Objects.hash(member);
    }

    @Override
    public String toString() {
        return member.toString();
    }
}
