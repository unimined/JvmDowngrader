package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;

/**
 * {@code @InitRewrite} registration — the generalized ctor retarget. A
 * callsite {@code owner.<init>(origArgs)} is retargeted to
 * {@code owner.<init>(toArgs)}, where each {@code to} slot is either a kept
 * original arg or the product of an {@code @InitArg} converter; an optional
 * {@code @InitPre} runs before the call and an optional {@code @InitPost}
 * tail after it, on both {@code NEW} sites and {@code this()}/{@code super()}
 * delegation calls.
 * <p>
 * All indices refer to the <em>original</em> ctor's argument list. The object
 * itself supplies {@link #post}'s leading receiver parameter, so
 * {@link #postFrom} indexes only its remaining params.
 */
public class CtorRewrite extends CtorEntry {

    /** the target ctor's argument types ({@code to} desc, same owner, void) */
    public final Type[] toArgs;
    /**
     * per-{@code toArgs}-slot source: the original-arg index to forward, or
     * {@code -1} when {@link #argMethods}[i] produces the value. Same length
     * as {@link #toArgs}.
     */
    public final int[] keep;
    /**
     * per-slot {@code @InitArg} converter ({@code static T f(origArgs…)}),
     * non-null exactly where {@link #keep}[i] == -1. Same length as
     * {@link #toArgs}.
     */
    public final MethodRef[] argMethods;
    /**
     * per-slot original-arg indices feeding {@link #argMethods}[i]'s params,
     * in param order. Same length as {@link #toArgs}; null where unused.
     */
    public final int[][] argFrom;
    /** optional {@code static void f(origArgs…)} run before the ctor call, or null */
    public final MethodRef pre;
    /** original-arg indices for {@link #pre}'s params, in param order */
    public final int[] preFrom;
    /** optional {@code static void f(Self, origArgs…[, int version])} tail on the initialized object, or null */
    public final MethodRef post;
    /** original-arg indices for {@link #post}'s params after the receiver (and before {@link #downgradeVersion}'s int), in param order */
    public final int[] postFrom;
    /** {@link #post}'s trailing {@code int} param receives the input class-file version */
    public final boolean downgradeVersion;

    public CtorRewrite(Type[] toArgs, int[] keep, Method[] argMethods, int[][] argFrom,
                       Method pre, int[] preFrom, Method post, int[] postFrom,
                       boolean downgradeVersion, boolean requiresRuntime) {
        this(toArgs, keep, wrap(argMethods), argFrom,
            pre == null ? null : new MethodRef(pre), preFrom,
            post == null ? null : new MethodRef(post), postFrom,
            downgradeVersion, requiresRuntime);
    }

    /** index-loading form — no reflective objects needed */
    public CtorRewrite(Type[] toArgs, int[] keep, MethodRef[] argMethods, int[][] argFrom,
                       MethodRef pre, int[] preFrom, MethodRef post, int[] postFrom,
                       boolean downgradeVersion, boolean requiresRuntime) {
        super(requiresRuntime);
        this.downgradeVersion = downgradeVersion;
        this.toArgs = toArgs;
        this.keep = keep;
        this.argMethods = argMethods;
        this.argFrom = argFrom;
        this.pre = pre;
        this.preFrom = preFrom;
        this.post = post;
        this.postFrom = postFrom;
    }

    private static MethodRef[] wrap(Method[] methods) {
        MethodRef[] out = new MethodRef[methods.length];
        for (int i = 0; i < methods.length; i++) {
            out[i] = methods[i] == null ? null : new MethodRef(methods[i]);
        }
        return out;
    }

    @Override
    public MethodRef[] methods() {
        int n = (pre != null ? 1 : 0) + (post != null ? 1 : 0);
        for (MethodRef m : argMethods) {
            if (m != null) {
                n++;
            }
        }
        MethodRef[] out = new MethodRef[n];
        int i = 0;
        for (MethodRef m : argMethods) {
            if (m != null) {
                out[i++] = m;
            }
        }
        if (pre != null) {
            out[i++] = pre;
        }
        if (post != null) {
            out[i] = post;
        }
        return out;
    }
}
