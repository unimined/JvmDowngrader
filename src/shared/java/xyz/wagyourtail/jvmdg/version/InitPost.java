package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A post-constructor hook for a class-level {@link InitRewrite}:
 * {@code static void f(Self, origArgs…)} runs after the retargeted
 * {@code <init>} returns, on the now-initialized object — the
 * "extra work during construction" half of ctor {@code @Modify} handlers:
 * {@code initCause(self, throwable)}, {@code setCharset(self, cs)},
 * {@code register(self)}.
 * <p>
 * The first param is the constructed object — the {@code DUP}'ed ref on a
 * {@code new} site, {@code this} on a {@code this()}/{@code super()}
 * delegation (legal once the delegated {@code <init>} has run; on delegation
 * any subclass-only state may not be set yet — inherent, there is nowhere
 * earlier the work can go). Remaining params bind to original-ctor args via
 * {@link #from} or unique type-match — kept and dropped args alike, so the
 * tail can capture args the new ctor never saw.
 * <p>
 * At most one {@code @InitPost} per rewrite — see {@link InitPre}.
 * Repeatable for tails shared across constructor overloads — javac boxes
 * repeats into {@link InitPosts}.
 */
@Repeatable(InitPosts.class)
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InitPost {

    /**
     * the {@link InitRewrite} this follows: the <em>original</em> ctor's
     * descriptor.
     */
    String target();

    /**
     * original-arg indices feeding this tail's params <em>after</em> the
     * leading receiver, in param order. Length must equal the param count
     * minus one. Empty derives each param by unique type-match — a
     * registration error on ambiguity or no match.
     */
    int[] from() default {};

    /**
     * disambiguates {@link #target} when two declared rewrites share the
     * original descriptor across owners — see {@link InitArg#owner()}.
     */
    String owner() default "";

}
