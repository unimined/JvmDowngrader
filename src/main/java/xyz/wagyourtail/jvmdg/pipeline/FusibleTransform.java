package xyz.wagyourtail.jvmdg.pipeline;

import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;

/**
 * A {@link ClassTransform} that can declare itself mergeable with an adjacent
 * fusible transform — the fusion seam. Fusion merges the <i>traversal</i>,
 * never the resolution tables: the fused form keeps each participant's own
 * (remap, resolver) pair position-correct and applies them in order inside
 * one class walk.
 * <p>
 * Whether two adjacent fusible transforms may actually merge is decided by
 * the engine from the barrier taxonomy — e.g. a class-local transform and a
 * transformed-graph barrier can't fuse across it. Implementations only answer
 * "could I combine with this neighbor if ordering allows".
 */
public abstract class FusibleTransform implements ClassTransform {

    /**
     * @return true if this transform can combine with {@code next} into a
     *         single traversal producing identical output.
     */
    public abstract boolean canFuseWith(FusibleTransform next);

    /**
     * The merged transform — must produce output identical to running
     * {@code this} then {@code next} sequentially.
     */
    public abstract FusibleTransform fuse(FusibleTransform next);

    @Override
    public abstract ClassNode apply(ClassNode node, TransformContext ctx) throws IOException;

}
