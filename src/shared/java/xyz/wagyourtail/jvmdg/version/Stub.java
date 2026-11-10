package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Stub {

    /**
     * reference to the stub target, if it can't be auto-resolved.
     */
    Ref ref() default @Ref();

    /**
     * if the input version in the VersionProvider should be pushed onto the stack before the stub is called.
     */
    boolean downgradeVersion() default false;

    /**
     * if the stub requires the runtime to be loaded to work properly.
     */
    boolean requiresRuntime() default false;

    /**
     * if the stub should be skipped when the runtime transformer is available
     * (inverse of {@link #requiresRuntime()}) — for stubs that are only
     * ordering guards or static-mode workarounds, where the real member is
     * preferable once the runtime has already set up its state.
     */
    boolean excludeWithRuntime() default false;

    /**
     * enum descriptor (e.g. {@code "Ljava/net/StandardProtocolFamily;"}) for
     * enum-extension reader stubs — when non-empty, the enum {@link Class}
     * and {@link #extendEnumMember()} name are pushed as trailing args before
     * the stub call. Set by stubs synthesized for {@link ExtendEnum}; not
     * intended for hand-written stubs.
     */
    String extendEnum() default "";

    /**
     * the injected enum constant's name, pushed after {@link #extendEnum()}'s
     * class when both are non-empty.
     */
    String extendEnumMember() default "";

    /**
     * if was abstract/default and need to insert as default implementation while downgrading.
     */
    boolean abstractDefault() default false;

    boolean noSpecial() default false;

    String[] excludeChild() default {};

}