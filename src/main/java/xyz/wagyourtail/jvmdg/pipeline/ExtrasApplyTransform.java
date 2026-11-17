package xyz.wagyourtail.jvmdg.pipeline;

import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.List;

/**
 * Wraps a delegate transform so it also applies to every class currently in
 * the worklist — the {@code stubWithExtras} semantic: apply to the primary
 * class first, then to a snapshot of everything emitted so far (which
 * includes earlier primaries' outputs and emitted helper classes).
 * <p>
 * Classes enqueued during the extras pass don't see this transform (the
 * snapshot hides them); they see the stage's subsequent transforms via their
 * own passes. A null result on an extra drops it from the worklist.
 */
public final class ExtrasApplyTransform extends FusibleTransform {

    private final ClassTransform delegate;

    public ExtrasApplyTransform(ClassTransform delegate) {
        this.delegate = delegate;
    }

    /**
     * This transform is an indivisible unit — its primary-then-extras pass
     * order is preserved internally — so it composes with any fusible
     * neighbor by straight sequence.
     */
    @Override
    public boolean canFuseWith(FusibleTransform next) {
        return true;
    }

    @Override
    public FusibleTransform fuse(FusibleTransform next) {
        return new FusedTransform(java.util.Arrays.asList(this, next));
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) throws IOException {
        ClassNode out = delegate.apply(node, ctx);
        if (out == null) {
            // stubWithExtras semantics: a dropped primary skips the extras pass
            return null;
        }
        List<ClassNode> extras = ctx.extras.snapshot();
        for (ClassNode extra : extras) {
            ClassNode transformed = delegate.apply(extra, ctx);
            ctx.extras.replace(extra, transformed);
        }
        return out;
    }

}
