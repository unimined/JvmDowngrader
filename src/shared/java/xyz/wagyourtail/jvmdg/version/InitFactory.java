package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code public static} method as a factory replacement for a
 * constructor: {@code new X(args)} construction sites are rewritten to
 * {@code X factory(args)} where the annotated method's return supplies the
 * object.
 * <p>
 * <b>Why it exists:</b> some constructors can't be emulated by retargeting to
 * another {@code <init>} — the replacement may need to return a cached or
 * differently-typed instance, avoid exposing {@code this} during construction,
 * or route through a private constructor. A static factory expresses all of
 * those shapes declaratively.
 * <p>
 * <b>What it does to bytecode:</b> at each call site the {@code NEW}/{@code DUP}
 * pair is proven by bounded stack simulation, removed, and the
 * {@code INVOKESPECIAL X.<init>} is replaced by {@code INVOKESTATIC} of the
 * annotated method (with {@code ARETURN}-compatible output already where the
 * reference is expected). Call sites the verifier can't prove — most notably
 * {@code this(...)}/{@code super(...)} delegation calls, where an
 * uninitialised {@code this} cannot be passed out to a static factory — are
 * left unchanged with a warning.
 * <p>
 * <b>Where to use it:</b> on a {@code public static} method in a
 * {@code stub(Class)}-registered provider class. {@link #ref} must name
 * {@code member="<init>"}; {@code ref.desc} declares the original ctor's
 * descriptor (required when it can't be derived from the factory's own
 * signature). When {@code ref.value} is empty the owner is derived from the
 * method's return type. The method must return an object type — the
 * constructed value.
 * <p>
 * <b>Contrast:</b> when the goal is to call a <em>different constructor of the
 * same class</em> and then finish setup, a ctor retarget
 * ({@link Stub#init()} → {@code @InitRewrite}) is the delegation-capable form —
 * use that instead whenever {@code this(...)}/{@code super(...)} call sites
 * must also rewrite.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InitFactory {

    /**
     * reference to the {@code <init>} target: {@code member} must be
     * {@code "<init>"}, {@code desc} is the replaced ctor's descriptor,
     * {@code value} the owning type (derived from the return type when empty).
     */
    Ref ref() default @Ref();

    /**
     * if the input version in the VersionProvider should be pushed onto the stack before the stub is called.
     */
    boolean downgradeVersion() default false;

    /**
     * if the stub requires the runtime to be loaded to work properly.
     * Equivalent to {@code condition = StubCondition.REQUIRES_RUNTIME}.
     */
    boolean requiresRuntime() default false;

    /**
     * activation condition for the rewrite — see {@link StubCondition}.
     */
    StubCondition condition() default StubCondition.ALWAYS;

}
