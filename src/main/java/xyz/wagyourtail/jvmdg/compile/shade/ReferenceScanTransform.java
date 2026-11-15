package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.pipeline.ClassTransform;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

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
        final int position = context.positionOf(node, ctx.flags);
        context.inputIndex.add(node, position, new RefExtractor.Filter() {
            @Override
            public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
                // side-channel: every extracted ref — jdk ones included —
                // feeds Shade-Reflection: usedOwners rooting at closure time
                shade.noteInputRef(position, member);
                return shade.apiClasses.contains(member.getOwner());
            }
        });
        noteDynamicSites(shade, node, position);
        return node;
    }

    /**
     * Records members containing calls into a dynamic reflection entry
     * point ({@code Class.forName}, {@code Lookup.find*}, reflective
     * {@code invoke}/{@code newInstance}) — {@code Shade-Reflection} below
     * {@code all} warns on these at the closure barrier.
     */
    public static void noteDynamicSites(ShadeContext context, ClassNode node, int position) {
        Type owner = Type.getObjectType(node.name);
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if ((insn instanceof MethodInsnNode || insn instanceof FieldInsnNode)
                    && ShadeContext.REFL_DISPATCH_OWNERS.contains(
                        insn instanceof MethodInsnNode ? ((MethodInsnNode) insn).owner
                            : ((FieldInsnNode) insn).owner)) {
                    context.noteDynamicSite(position,
                        new MemberNameAndDesc(m.name, Type.getMethodType(m.desc))
                            .toFullyQualified(owner));
                    break;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "reference-scan";
    }

}
