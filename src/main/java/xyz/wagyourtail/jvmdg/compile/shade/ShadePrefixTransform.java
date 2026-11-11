package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.pipeline.ClassTransform;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;

/**
 * Rewrites every reference to a required api type to its prefixed name by
 * visiting the class through {@link ClassRemapper} into a fresh
 * {@link ClassNode} — the same visitor the jar flow runs into a
 * {@code ClassWriter} per class, so coverage of type slots (members,
 * signatures, annotations, nest/inner/permitted metadata, frames, indy,
 * ldc/condy) is identical rather than a parallel hand-rolled traversal.
 * <p>
 * Runs after {@link ShadeInlineTransform}: inlined method bodies' internal
 * api refs get prefixed here too, matching the jar flow's remap-on-write.
 * Input class names never appear in the remap — only api types do — so
 * primary classes keep their names while already-prefixed api classes the
 * barrier emitted pass through unchanged.
 */
public class ShadePrefixTransform implements ClassTransform {

    private final ShadeContext context;

    public ShadePrefixTransform(ShadeContext context) {
        this.context = context;
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) {
        return remap(node, context.positionOf(node, ctx.flags), context);
    }

    /**
     * The remapped copy of {@code node} under this position's prefix remap —
     * shared with the closure barrier for captured variants. No remap entry
     * for the position (nothing required) returns the node unchanged.
     */
    public static ClassNode remap(ClassNode node, int position, ShadeContext context) {
        SimpleRemapper remapper = context.remappers.get(position);
        if (remapper == null) {
            return node;
        }
        ClassNode out = new ClassNode();
        node.accept(new ClassRemapper(out, remapper));
        return out;
    }

    @Override
    public String toString() {
        return "shade-prefix";
    }

}
