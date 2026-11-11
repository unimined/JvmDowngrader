package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A pre-constructor hook for a class-level {@link InitRewrite}:
 * {@code static void f(origArgs…)} runs before the retargeted {@code <init>}
 * call, after the original args have been evaluated and spilled. Use it for
 * argument validation and setup that must happen before construction —
 * {@code checkRange}, {@code Objects.requireNonNull}, capture-before-mutate —
 * the work ctor {@code @Modify} handlers emitted ahead of the call.
 * <p>
 * On {@code this()}/{@code super()} delegation sites the hook runs before
 * the delegation {@code invokespecial} — legal because it is static and
 * never touches {@code this} — so its checks still fire even when the
 * object isn't constructed yet.
 * <p>
 * At most one {@code @InitPre} per rewrite: several would make emission
 * order depend on unspecified {@code getDeclaredMethods} ordering. Params
 * bind like {@link InitArg}'s. Repeatable for hooks shared across
 * constructor overloads — javac boxes repeats into {@link InitPres}.
 */
@Repeatable(InitPres.class)
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InitPre {

    /**
     * the {@link InitRewrite} this precedes: the <em>original</em> ctor's
     * descriptor.
     */
    String target();

    /**
     * original-arg indices feeding this hook's params, in param order.
     * Length must equal the param count. Empty derives each param by unique
     * type-match — a registration error on ambiguity or no match.
     */
    int[] from() default {};

    /**
     * disambiguates {@link #target} when two declared rewrites share the
     * original descriptor across owners — see {@link InitArg#owner()}.
     */
    String owner() default "";

}
