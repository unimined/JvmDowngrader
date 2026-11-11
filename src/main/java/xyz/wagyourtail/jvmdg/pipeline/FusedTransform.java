package xyz.wagyourtail.jvmdg.pipeline;

import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A fused run of adjacent {@link FusibleTransform}s: the delegates apply in
 * list order inside one {@link #apply} call — null short-circuits exactly as
 * the engine's transform loop would. Composition preserves semantics by
 * construction; participants that restructure execution (e.g. an insn-level
 * pair cascade) arrive as their own {@link FusibleTransform} fused forms.
 */
public final class FusedTransform extends FusibleTransform {

    private final List<FusibleTransform> delegates;

    public FusedTransform(List<FusibleTransform> delegates) {
        this.delegates = new ArrayList<FusibleTransform>(delegates);
    }

    /** The ordered participants — diagnostics and fuse-transitivity. */
    public List<FusibleTransform> delegates() {
        return delegates;
    }

    @Override
    public boolean canFuseWith(FusibleTransform next) {
        return true;
    }

    @Override
    public FusibleTransform fuse(FusibleTransform next) {
        List<FusibleTransform> merged = new ArrayList<FusibleTransform>(delegates);
        if (next instanceof FusedTransform) {
            merged.addAll(((FusedTransform) next).delegates);
        } else {
            merged.add(next);
        }
        return new FusedTransform(merged);
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) throws IOException {
        for (ClassTransform t : delegates) {
            node = t.apply(node, ctx);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FusedTransform[");
        for (int i = 0; i < delegates.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(delegates.get(i).getClass().getSimpleName());
        }
        return sb.append(']').toString();
    }

}
