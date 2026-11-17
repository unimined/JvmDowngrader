package xyz.wagyourtail.jvmdg.pipeline;

import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;

/**
 * One step in a {@link Stage}: transforms a class in place or by returning a
 * different node. Returning null drops the class from the pipeline (and from
 * the worklist when applied to an emitted class).
 * <p>
 * Version-agnostic: a transform gets everything it needs from
 * {@link TransformContext} — the classpath index, the stage-position worklist
 * it may {@link TransformWorklist#enqueue(ClassNode) enqueue} emitted classes
 * into, flags, and registrar-private data.
 */
public interface ClassTransform {

    /**
     * Transforms {@code node}.
     * @return the (possibly same) transformed node, or null to drop it.
     */
    ClassNode apply(ClassNode node, TransformContext ctx) throws IOException;

}
