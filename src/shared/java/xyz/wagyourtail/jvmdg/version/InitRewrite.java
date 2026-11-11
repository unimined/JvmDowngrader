package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constructor rewrite: every {@code owner.<init>(origDesc)} call
 * site is retargeted to {@code owner.<init>(to)}, with argument mapping,
 * argument synthesis, and pre/post hooks declared by method-level
 * {@link InitArg}, {@link InitPre}, and {@link InitPost} handlers that name
 * this rewrite by {@link #ref}'s desc.
 * <p>
 * <b>Why it exists:</b> most constructor stubs are not replacements — they
 * are signature changes. The JDK adds an overload whose args are a subset
 * ({@code FileReader(String,Charset) → FileReader(String)} + capture the
 * charset), a transform ({@code Charset → name()}), or a pre-check. Writing
 * those as {@code @Modify} insn surgery meant hand-emitting
 * {@code DUP_X2}/{@code POP}/{@code SWAP} juggling against the
 * {@code NEW}/{@code DUP} shape — fragile, wide-type-hostile, and unable to
 * touch {@code this()}/{@code super()} delegation calls at all. This
 * annotation declares the rewrite and lets the transform emit it on both
 * shapes, verifier-gated per call site.
 * <p>
 * <b>Repeatability:</b> one class may declare several rewrites (for several
 * ctors of the same or different owners) by repeating the annotation — Java 8
 * sugar that javac boxes into {@link InitRewrites}; on older source levels
 * write {@code @InitRewrites({ ... })} explicitly — the scanner reads both.
 * Rewrites coexist with normal {@link Stub}/{@link DefaultImpl}/
 * {@link InitFactory}/{@code @Modify} methods in the same class.
 * <p>
 * <b>Emission:</b> original args are spilled to fresh locals (preserving
 * evaluation order and cat-2 widths), the optional {@code @InitPre} runs,
 * each {@code to} slot is loaded from a kept arg or produced by its
 * {@code @InitArg} converter, the retargeted {@code INVOKESPECIAL} runs, and
 * the optional {@code @InitPost} tail runs on the initialized object
 * ({@code DUP} on a {@code new} site, {@code ALOAD 0} on delegation).
 * Callsites the bounded stack verification can't prove are left untouched
 * with a warning.
 */
@Repeatable(InitRewrites.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InitRewrite {

    /**
     * the original constructor: {@code ref.value} is the owning type,
     * {@code ref.desc} its descriptor. {@code ref.member} is implied
     * {@code "<init>"} and may be omitted or stated.
     */
    Ref ref();

    /**
     * the replacement constructor's descriptor — same owner, must return
     * {@code void}, e.g. {@code "(Ljava/lang/String;)V"}.
     */
    String to();

    /**
     * per-{@link #to}-slot argument source: index of the original arg to
     * forward, or {@code -1} for a slot an {@link InitArg} converter fills.
     * Length must equal the {@link #to} argument count. Empty derives each
     * non-{@code @InitArg} slot by matching its type to a unique original
     * arg — a registration error when no arg or several match.
     */
    int[] keep() default {};

    /**
     * the {@link InitPost} tail takes the input class-file version as a
     * trailing {@code int} param — same flag as {@link Stub#downgradeVersion},
     * narrowed to the tail since it is the post-construction hook. A
     * registration error when set without an {@code @InitPost}.
     */
    boolean downgradeVersion() default false;

    /**
     * call sites may only rewrite when the runtime transform is available —
     * see {@link Stub#requiresRuntime}.
     */
    boolean requiresRuntime() default false;

}
