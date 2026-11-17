package xyz.wagyourtail.jvmdg.version;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.cli.Flags;

import java.util.Set;
import java.util.function.Function;

/**
 * Per-invocation context handed to {@link Modify} and {@link IndyDesugar}
 * handlers — declared as a trailing parameter on {@code @Modify} handlers
 * (see {@link Modify#MODIFY_SIG} / {@link ModifyUtils#MODIFY_SIG}) and as
 * the sole parameter of {@code @IndyDesugar} handlers. Carries the run's
 * {@link Flags} plus the call-site state a handler needs, so handlers can
 * branch on output-shaping options (e.g. {@code noIndy}) without signature
 * gymnastics.
 * <p>
 * Lives in {@code src/main} (not shared) because {@link Flags} does —
 * handlers are transform-side code and never execute on the target JVM, so
 * the core-type reference in their signatures is consistent with the ASM
 * types already present in {@code MODIFY_SIG}.
 */
public final class ModifyContext {

    public final Flags flags;
    /** the class being transformed */
    public final ClassNode node;
    /** the method containing the site */
    public final MethodNode method;
    /** index of the site insn within {@link #method}'s instruction list */
    public final int insnIdx;
    /** sink for classes the handler generates (e.g. lambda impl classes) */
    public final Set<ClassNode> extras;
    /** {@code flags.debugNoSynthetic} — suppresses ACC_SYNTHETIC on emitted members */
    public final boolean noSynthetic;
    /** the site's indy insn — non-null only for indy-site invocations */
    public final InvokeDynamicInsnNode indy;
    private final Function<Type, Type> unstubFn;

    public ModifyContext(Flags flags, ClassNode node, MethodNode method, int insnIdx,
                         Set<ClassNode> extras, boolean noSynthetic, InvokeDynamicInsnNode indy,
                         Function<Type, Type> unstub) {
        this.flags = flags;
        this.node = node;
        this.method = method;
        this.insnIdx = insnIdx;
        this.extras = extras;
        this.noSynthetic = noSynthetic;
        this.indy = indy;
        this.unstubFn = unstub;
    }

    /**
     * adapter-type → real-JDK-type translation over the whole provider
     * chain — for emitted data that must carry source-vocabulary names
     * (e.g. {@code SerializedLambda} identity fields on the wire), since
     * the site's types may already be remapped when the handler runs.
     * Identity when no provider context is available.
     */
    public Type unstub(Type t) {
        return unstubFn == null ? t : unstubFn.apply(t);
    }

}
