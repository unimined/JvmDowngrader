package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.version.JEP;

/**
 * One {@code @Adapter} class-type registration, declaratively: the
 * {@code target} type the source type maps to, {@code keepInterface}, and the
 * {@code jvmdg$instanceof}/{@code jvmdg$checkcast} hook members (nullable) the
 * stub class declares — baked at registration so emitters never reflect on
 * the stub class. The map key is the source (JDK) type.
 */
public class ClassStubEntry {

    /** the type the source type remaps to (usually the adapter/stub class itself) */
    public final Type target;
    /** the {@code @Adapter}-annotated stub class's own type */
    public final Type stubType;
    /** keep the source type in {@code implements} clauses instead of dropping it */
    public final boolean keepInterface;
    /** {@code jvmdg$instanceof} member on the stub class, or null — emitted as INVOKESTATIC */
    public final FullyQualifiedMemberNameAndDesc instanceofHook;
    /** {@code jvmdg$checkcast} member on the stub class, or null — emitted as INVOKESTATIC */
    public final FullyQualifiedMemberNameAndDesc checkcastHook;
    /** {@code @JEP} tags on the stub class filtering this registration (empty = none) */
    public final int[] jeps;
    private Lazy<Class<?>> clazz;

    /** index-loading form — resolves the stub class lazily if a consumer reflects on it */
    public ClassStubEntry(Type target, Type stubType, boolean keepInterface, FullyQualifiedMemberNameAndDesc instanceofHook, FullyQualifiedMemberNameAndDesc checkcastHook, int[] jeps) {
        this.target = target;
        this.stubType = stubType;
        this.keepInterface = keepInterface;
        this.instanceofHook = instanceofHook;
        this.checkcastHook = checkcastHook;
        this.jeps = jeps;
        this.clazz = new Lazy<Class<?>>() {
            @Override
            protected Class<?> init() {
                try {
                    return Class.forName(ClassStubEntry.this.stubType.getClassName());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("cannot resolve stub class " + ClassStubEntry.this.stubType, e);
                }
            }
        };
    }

    /** reflective-scan constructor — finds the jvmdg$ hooks on the stub class */
    public ClassStubEntry(Type target, final Class<?> stubClass, boolean keepInterface) {
        this(target, Type.getType(stubClass), keepInterface, find(stubClass, "jvmdg$instanceof"), find(stubClass, "jvmdg$checkcast"),
            stubClass.getAnnotation(JEP.class) == null ? new int[0] : new int[]{stubClass.getAnnotation(JEP.class).value()});
        this.clazz = new Lazy<Class<?>>() {
            @Override
            protected Class<?> init() {
                return stubClass;
            }
        };
    }

    private static FullyQualifiedMemberNameAndDesc find(Class<?> stubClass, String name) {
        for (java.lang.reflect.Method m : stubClass.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return FullyQualifiedMemberNameAndDesc.of(m);
            }
        }
        return null;
    }

    /** the {@code @Adapter}-annotated class — reflective use only (coverage tooling, tests) */
    public Class<?> clazz() {
        return clazz.get();
    }
}
