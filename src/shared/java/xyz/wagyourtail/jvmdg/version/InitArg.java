package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An argument converter for a class-level {@link InitRewrite}: produces one
 * of the target constructor's arguments from the original constructor's
 * arguments. This is the declarative form of the arg-massaging half of ctor
 * {@code @Modify} handlers — {@code Charset → name()},
 * {@code causeMessage(Throwable)}, {@code copyOfRange}, building an
 * intermediate {@code OutputStreamWriter}, or a zero-arg constant.
 * <p>
 * <b>Shape:</b> {@code static T f(origArgs…)} — {@code T} must be assignable
 * to the {@link InitRewrite#to} argument at {@link #slot}. The params bind
 * to original-ctor args: explicitly via {@link #from}, or by unique
 * type-match when omitted (a registration error on ambiguity). A param may
 * also be {@code @Coerce}/{@code @Adapter}-typed; it binds against the real
 * JDK type like a {@link Stub} signature.
 * <p>
 * The converter runs <em>before</em> the retargeted {@code <init>} call, so
 * throwing inside it abandons an uninitialized object — legal, and
 * indistinguishable from an argument expression throwing.
 * <p>
 * Repeatable: one method may fill slots for several rewrites — repeat the
 * annotation (javac boxes into {@link InitArgs}, or write the container
 * explicitly on pre-Java-8 source).
 */
@Repeatable(InitArgs.class)
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InitArg {

    /**
     * the {@link InitRewrite} this fills: the <em>original</em> ctor's
     * descriptor ({@code ref.desc} of the rewrite).
     */
    String target();

    /**
     * which {@link InitRewrite#to} argument slot this method produces
     * ({@code 0}-based).
     */
    int slot();

    /**
     * original-arg indices feeding this converter's params, in param order.
     * Length must equal the param count. Empty derives each param by unique
     * type-match against the original args — a registration error on
     * ambiguity or no match.
     */
    int[] from() default {};

    /**
     * disambiguates {@link #target} when this class declares rewrites for
     * two owners sharing the same original descriptor; a type descriptor or
     * internal name matching the rewrite's {@code ref.value}. Unneeded (and
     * an error to omit) only in that collision case.
     */
    String owner() default "";

}
