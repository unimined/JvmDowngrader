package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.pipeline.ClassTransform;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

/**
 * Feeds each post-version-chain class into the input {@link ReferenceIndex}
 * at its jar slot — the api references the shade hunts are the ones the
 * downgrade introduced, so the scan must anchor after every stage that can
 * emit api refs. Only references into the api set are retained, matching the
 * jar-level {@code ApiShader} scan filter.
 * <p>
 * The class passes through unchanged; indexing is the side effect.
 */
public class ReferenceScanTransform implements ClassTransform {

    private final ShadeContext context;

    public ReferenceScanTransform(ShadeContext context) {
        this.context = context;
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) {
        // module-info carries no references the index tracks — the jar-level
        // IndexScanner skips it for the same reason
        if ("module-info".equals(node.name)) {
            return node;
        }
        final ShadeContext shade = context;
        context.inputIndex.add(node, context.positionOf(node, ctx.flags), new RefExtractor.Filter() {
            @Override
            public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
                return shade.apiClasses.contains(member.getOwner());
            }
        });
        return node;
    }

    @Override
    public String toString() {
        return "reference-scan";
    }

}
