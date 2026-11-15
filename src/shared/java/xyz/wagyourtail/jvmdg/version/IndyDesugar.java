package xyz.wagyourtail.jvmdg.version;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a transform-side handler that desugars an {@code invokedynamic}
 * site whose bootstrap method is {@link #ref}'s member — the indy-only
 * counterpart of {@link Modify}. The handler fires at the boundary where
 * the bootstrap API disappears (replacing the indy with plain bytecode)
 * and again from the post-chain {@code noIndy} sweep at any target.
 * <p>
 * Unlike {@code @Modify}, registration here never intercepts direct invoke
 * sites on the member: a real {@code invokestatic} call to the bootstrap
 * resolves {@link Stub} impls normally (or fails stub resolution), so a bsm
 * member can carry both an {@code @IndyDesugar} (indy sites) and a
 * {@code @Stub} (invoke sites) without the two colliding.
 * <p>
 * Handler signature is fixed: {@code public static void
 * name(xyz.wagyourtail.jvmdg.version.ModifyContext ctx)} — the site indy
 * node, containing method/class, extras sink, and flags all arrive on the
 * context ({@link ModifyContext#indy} is always non-null here).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IndyDesugar {
    /**
     * The bootstrap member this desugar handles — typically
     * {@code (Lookup,String,MethodType,...)CallSite} on the bootstrap
     * class, e.g. {@code java/lang/invoke/LambdaMetafactory.metafactory}.
     */
    Ref ref();
}
