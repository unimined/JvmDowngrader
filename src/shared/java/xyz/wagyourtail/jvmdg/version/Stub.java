package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code public static} method as the downgrade replacement for a
 * target member: calls to the target are rewritten to an
 * {@code INVOKESTATIC} of the annotated method.
 * <p>
 * Constructor work is deliberately not here — it carries a different shape
 * (argument synthesis/conversion, pre- and post-construction hooks,
 * delegation) and lives in its own family: {@link InitRewrite} retargets a
 * constructor call to another ctor of the same class,
 * {@link InitFactory} replaces {@code new X(args)} with a static factory,
 * and {@link InitArg}/{@link InitPre}/{@link InitPost} supply synthesized
 * arguments and pre/post hooks for a rewrite. A {@code <init>} {@code @Stub}
 * is a registration error pointing at those.
 * <p>
 * Interface methods that need their body materialized into implementors
 * rather than their callsites redirected use {@link DefaultImpl}.
 */
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
     * if the {@code Class} of the class containing the callsite should be
     * pushed onto the stack before the stub is called — emitted as an
     * {@code ldc} of the containing class, before {@link #downgradeVersion}'s
     * trailing int. The handler signature carries it as a {@code Class}
     * parameter so caller-sensitive stubs (e.g. {@code Class.forName})
     * resolve through the callsite's own class loader.
     */
    boolean callerClass() default false;

    /**
     * if the stub requires the runtime to be loaded to work properly.
     * Equivalent to {@code condition = StubCondition.REQUIRES_RUNTIME} —
     * setting both a non-{@link StubCondition#ALWAYS} condition and this flag
     * is a registration error.
     */
    boolean requiresRuntime() default false;

    /**
     * activation condition for the redirect — see {@link StubCondition}.
     * {@link #PREVIEW} entries register but never fire unless the run opted
     * in via {@code --allowPreview}.
     */
    StubCondition condition() default StubCondition.ALWAYS;

    /**
     * Never rewrite {@code INVOKESPECIAL}/{@code NEWINVOKESPECIAL} sites —
     * a {@code super.m()} call keeps its native super-dispatch semantics
     * instead of being rerouted through the stub. Derived automatically for
     * members of {@code OWN_INTERFACE}-mode {@link Adapter} targets (the
     * stand-in interface carries real default bodies, so the super call
     * stays legal and correct); declare it only for members whose owner
     * isn't a class-stubbed type.
     */
    boolean noSpecial() default false;

    String[] excludeChild() default {};

    /**
     * Force the {@link StubKind} classification for this member — the default
     * {@link StubKind#AUTO} derives it: member stubs whose target exists on
     * the downgrade target classify {@link StubKind#BEHAVIOR}, else
     * {@link StubKind#MISSING_API}. Override when the member's JDK-presence
     * data can't express the intent (e.g. a stub deliberately shadowing an
     * existing member that {@code ct.sym} reports absent).
     */
    StubKind kind() default StubKind.AUTO;

}