package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.pipeline.ClassTransform;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

import java.util.Map;
import java.util.Set;

/**
 * Inlines api members that exactly one input class consumes: every insn the
 * index recorded as referencing the api member is retargeted to the input
 * class ({@code jvmdg$inlined$<name>}), and a private copy of the api member
 * is appended — the same recipe {@code ApiShader} applies at write time.
 * <p>
 * {@link #inline} is the shared core — the closure barrier calls it directly
 * for captured variants, which can't flow through the name-keyed worklist.
 * Must run before the prefix remap: the appended method's own api refs get
 * prefixed by that stage, matching the jar flow's order.
 */
public class ShadeInlineTransform implements ClassTransform {

    private final ShadeContext context;

    public ShadeInlineTransform(ShadeContext context) {
        this.context = context;
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) {
        return inline(node, context.positionOf(node, ctx.flags), context, ctx);
    }

    /**
     * Applies this position's inline candidates to {@code node}: rewrites the
     * referencing insns the index retained and appends the api method copy.
     * Identity for classes with no candidates (and for prefixed api classes,
     * which never appear in the input index).
     */
    public static ClassNode inline(ClassNode node, int position, ShadeContext context, TransformContext ctx) {
        Map<Type, Set<FullyQualifiedMemberNameAndDesc>> targets = context.inlineTargets.get(position);
        if (targets == null) {
            return node;
        }
        Type owner = Type.getObjectType(node.name);
        Set<FullyQualifiedMemberNameAndDesc> members = targets.get(owner);
        if (members == null) {
            return node;
        }
        boolean isInterface = (node.access & Opcodes.ACC_INTERFACE) != 0;
        for (FullyQualifiedMemberNameAndDesc apiMember : members) {
            // the retained insns in this class that reference the api member
            for (AbstractInsnNode insn : context.inputIndex.insnsFor(apiMember, position, owner)) {
                ApiShader.inlineRef(apiMember, insn, owner, isInterface);
            }
            appendApiMethod(node, apiMember, position, context, ctx);
        }
        return node;
    }

    /**
     * Appends a private copy of the api method as {@code jvmdg$inlined$<name>}
     * — the target the rewritten insns now call. The api node is resolved at
     * this position's slot so multi-release api jars contribute the right body.
     */
    private static void appendApiMethod(ClassNode node, FullyQualifiedMemberNameAndDesc apiMember, int position, ShadeContext context, TransformContext ctx) {
        ClassNode apiNode = context.apiIndex.nodeFor(apiMember.getOwner(), position);
        if (apiNode == null) {
            ctx.diagnostics.warn("shade", "inline candidate without api class at slot " + position + ": " + apiMember);
            return;
        }
        MethodNode method = null;
        for (MethodNode m : apiNode.methods) {
            if (m.name.equals(apiMember.getName()) && m.desc.equals(apiMember.getDesc().getDescriptor())) {
                method = m;
                break;
            }
        }
        if (method == null) {
            // fields aren't appended — their insns were already retargeted to a
            // member that can't exist; surface instead of silently producing
            // a dangling reference (jar flow throws for the same reason)
            throw new IllegalStateException("Method not found in class: " + apiMember);
        }
        MethodNode copy = new MethodNode(
            (method.access & ~Opcodes.ACC_PUBLIC) | Opcodes.ACC_PRIVATE | (ctx.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC),
            "jvmdg$inlined$" + method.name, method.desc, method.signature,
            method.exceptions.toArray(new String[0]));
        method.accept(copy);
        node.methods.add(copy);
    }

    @Override
    public String toString() {
        return "shade-inline";
    }

}
