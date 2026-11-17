package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Adapter {

    String value();

    String target() default "";

    /**
     * if false, will completely remove interface, rather than remapping.
     * if this is on an annotation, it will retain the original annotation.
     */
    boolean keepInterface() default true;

    /**
     * Retro-supertype mode for {@code instanceof}/{@code checkcast} on the
     * source type. {@link Mode#AUTO} resolves from {@link #target}: an
     * explicit target names a real JDK supertype → {@link Mode#SUPERTYPE};
     * an empty target (remap to the adapter class itself) →
     * {@link Mode#OWN_INTERFACE}.
     */
    Mode mode() default Mode.AUTO;

    /**
     * Internal names ({@code java/util/List} form) of extra classes the source
     * type is retro-implemented by — members the build-time ct.sym derivation
     * can't see (jvmdg impls, third-party types), <em>unioned</em> with the
     * derived set into the membership table {@code RetroTypes.isInstance}/
     * {@code cast} consults. Declared names can add members, never veto them;
     * types that implement neither the real interface nor the stub opt in via
     * the adapter's {@code $Marker} instead. The adapter type and its
     * {@code $Marker} are always implicit members.
     */
    String[] implementedBy() default {};

    /**
     * Member adapting a member to the adapter type for {@code checkcast} in
     * {@link Mode#OWN_INTERFACE} — {@code "Lowner;name;(Ljava/lang/Object;)Ltype;"}.
     * The factory handles membership checking and must throw
     * {@link ClassCastException} for non-members. Empty = generated
     * {@code RetroTypes.wrap} dispatch over {@link #impls} + adapter
     * checkcast. A {@code jvmdg$checkcast} member on the class always
     * overrides this.
     */
    String wrap() default "";

    /**
     * Wrapper implementations for {@link Mode#OWN_INTERFACE}
     * {@code checkcast} — each class must implement the adapter and declare
     * exactly one single-argument constructor whose parameter type is the
     * retro member it wraps. Declaration order is dispatch order when member
     * types overlap. Impls flow <em>upward</em> through the adapter's own
     * interface hierarchy: an impl declared on a child adapter also serves
     * ancestor adapters' casts (it is assignable to them), ordered
     * most-specific-adapter first. The generated
     * {@link xyz.wagyourtail.jvmdg.stub.RetroTypes#wrap} emits for every
     * OWN_INTERFACE checkcast without a {@code jvmdg$checkcast} member or
     * {@link #wrap} factory; rows land in {@code META-INF/jvmdg/wraps}.
     */
    Class<?>[] impls() default {};

    /**
     * Member names on the source type that are signature-polymorphic —
     * javac accepts <em>any</em> descriptor at the callsite (VarHandle's
     * access modes, MethodHandle's invoke/invokeExact). A callsite
     * {@code INVOKEVIRTUAL}/{@code INVOKEINTERFACE} on a listed name that
     * resolves to no member stub is emitted as an {@code invokedynamic}
     * whose bootstrap is the adapter's {@code jvmdg$bootstrap} hook member
     * (required when this is non-empty): it derives the impl signature from
     * the callsite's name+desc at link time. Names not listed keep the
     * normal member-stub/native path, so concrete members such as
     * {@code equals} must not appear here.
     */
    String[] signaturePolymorphic() default {};

    /**
     * How {@code instanceof}/{@code checkcast} on the adapter's source type
     * behaves after the type remap.
     */
    enum Mode {
        /** resolve from {@link #target} — see {@link Adapter#mode()} */
        AUTO,
        /** the source type remaps to a real JDK supertype it was retro-added to */
        SUPERTYPE,
        /** the source type remaps to the adapter class itself */
        OWN_INTERFACE
    }

}
