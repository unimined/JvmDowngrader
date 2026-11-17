package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.JEP;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One {@code @Adapter} class-type registration, declaratively: the
 * {@code target} type the source type maps to, {@code keepInterface}, the
 * retro-supertype {@link #mode} + {@link #implementedBy}/{@link #wrap}
 * membership data, and the {@code jvmdg$instanceof}/{@code jvmdg$checkcast}
 * hook members (nullable) the stub class declares — baked at registration so
 * emitters never reflect on the stub class. The map key is the source (JDK)
 * type.
 */
public class ClassStubEntry {

    /** the type the source type remaps to (usually the adapter/stub class itself) */
    public final Type target;
    /** the {@code @Adapter}-annotated stub class's own type */
    public final Type stubType;
    /** keep the source type in {@code implements} clauses instead of dropping it */
    public final boolean keepInterface;
    /**
     * {@link #stubType} is an interface — the {@code itf} bit for emitted
     * invokes on hook members. Baked at registration like the hooks: class
     * loading is unsafe in the transform path (runtime loaders see
     * pre-downgrade class versions).
     */
    public final boolean itf;
    /** {@code jvmdg$instanceof} member on the stub class, or null — emitted as INVOKESTATIC */
    public final FullyQualifiedMemberNameAndDesc instanceofHook;
    /** {@code jvmdg$checkcast} member on the stub class, or null — emitted as INVOKESTATIC */
    public final FullyQualifiedMemberNameAndDesc checkcastHook;
    /** {@code @JEP} tags on the stub class filtering this registration (empty = none) */
    public final int[] jeps;
    /**
     * retro-supertype mode — resolved at construction, never
     * {@link Adapter.Mode#AUTO}: an explicit {@code target} different from
     * {@link #stubType} is {@code SUPERTYPE}, otherwise {@code OWN_INTERFACE}.
     */
    public final Adapter.Mode mode;
    /**
     * internal names the source type is retro-implemented by — from
     * {@code @Adapter(implementedBy=…)} or the build-time JDK derivation
     * loaded through {@code stubs.idx}; empty means "unhandled by the emitted
     * {@code RetroTypes} path" until derivation lands. The marker interface
     * is registered under {@code <stub internal name>$Marker}.
     */
    public Set<String> implementedBy;
    /** {@code @Adapter(wrap=…)} adaptation factory for OWN_INTERFACE checkcast, or null */
    public final FullyQualifiedMemberNameAndDesc wrap;
    /**
     * {@code jvmdg$bootstrap} hook member on the stub class, or null — the
     * invokedynamic bootstrap for signature-polymorphic callsites. Baked at
     * registration like the other hooks.
     */
    public final FullyQualifiedMemberNameAndDesc polymorphic;
    /**
     * member names {@link #polymorphic} serves — {@code @Adapter(signaturePolymorphic=…)};
     * only a callsite whose member-stub lookup misses <em>and</em> whose name
     * is listed emits an indy (concrete members such as {@code equals} stay
     * on the normal path).
     */
    public final Set<String> polymorphicNames;

    private Lazy<Class<?>> clazz;

    /** index-loading form — resolves the stub class lazily if a consumer reflects on it */
    public ClassStubEntry(Type target, Type stubType, boolean keepInterface, boolean itf, FullyQualifiedMemberNameAndDesc instanceofHook, FullyQualifiedMemberNameAndDesc checkcastHook, int[] jeps, Adapter.Mode mode, Set<String> implementedBy, FullyQualifiedMemberNameAndDesc wrap, FullyQualifiedMemberNameAndDesc polymorphic, Set<String> polymorphicNames) {
        this.target = target;
        this.stubType = stubType;
        this.keepInterface = keepInterface;
        this.itf = itf;
        this.instanceofHook = instanceofHook;
        this.checkcastHook = checkcastHook;
        this.jeps = jeps;
        this.mode = mode == Adapter.Mode.AUTO
            ? (target.equals(stubType) ? Adapter.Mode.OWN_INTERFACE : Adapter.Mode.SUPERTYPE)
            : mode;
        this.implementedBy = implementedBy == null
            ? Collections.<String>emptySet()
            : Collections.unmodifiableSet(new LinkedHashSet<>(implementedBy));
        this.wrap = wrap;
        this.polymorphic = polymorphic;
        this.polymorphicNames = polymorphicNames == null
            ? Collections.<String>emptySet()
            : Collections.unmodifiableSet(new LinkedHashSet<>(polymorphicNames));
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
    public ClassStubEntry(Type target, final Class<?> stubClass, boolean keepInterface, Adapter.Mode mode, Set<String> implementedBy, FullyQualifiedMemberNameAndDesc wrap, Set<String> polymorphicNames) {
        this(target, Type.getType(stubClass), keepInterface, stubClass.isInterface(), find(stubClass, "jvmdg$instanceof"), find(stubClass, "jvmdg$checkcast"),
            stubClass.getAnnotation(JEP.class) == null ? new int[0] : new int[]{stubClass.getAnnotation(JEP.class).value()},
            mode, implementedBy, wrap,
            polymorphicNames != null && !polymorphicNames.isEmpty()
                ? find(stubClass, "jvmdg$bootstrap") : null,
            polymorphicNames);
        if (!this.polymorphicNames.isEmpty() && this.polymorphic == null) {
            throw new IllegalArgumentException(stubClass.getName() +
                " declares @Adapter(signaturePolymorphic=…) but has no jvmdg$bootstrap member");
        }
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

    /**
     * Build-time derivation backfill: replaces the (usually annotation-empty)
     * membership set before {@code stubs.idx} serialization. Not for runtime
     * use — the index carries the result.
     */
    public void setImplementedBy(Set<String> implementedBy) {
        this.implementedBy = Collections.unmodifiableSet(new LinkedHashSet<>(implementedBy));
    }
}
