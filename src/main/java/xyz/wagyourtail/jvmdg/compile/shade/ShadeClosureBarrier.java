package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.compile.ManifestUtils;
import xyz.wagyourtail.jvmdg.pipeline.StageHook;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The post-scan global barrier: with every post-transform class and captured
 * variant indexed, computes the api closure per jar slot and materializes
 * everything downstream stages and the writer consume —
 * <ul>
 *   <li>per-slot {@code required} closure into {@link ShadeContext#closures}
 *       (the work {@code ApiShader} does per jar version)</li>
 *   <li>inline candidates inverted to per-slot {@link ShadeContext#inlineTargets}</li>
 *   <li>per-slot prefix {@link ShadeContext#remappers}</li>
 *   <li>pruned + prefixed api {@link ClassNode}s into {@link ShadeContext#emitted}</li>
 *   <li>api resource copies into {@link TransformContext#emitResource}</li>
 *   <li>captured variants shaded in place (inline + prefix) — they can't flow
 *       through the class loop because the worklist is name-keyed</li>
 * </ul>
 * Runs on the scan stage's successor; the class list it sees is unchanged.
 */
public class ShadeClosureBarrier implements StageHook {

    /** Deterministic ordering for entry types that aren't Comparable — by their string forms. */
    private static final Comparator<FullyQualifiedMemberNameAndDesc> FQN_ORDER =
        new Comparator<FullyQualifiedMemberNameAndDesc>() {
            @Override
            public int compare(FullyQualifiedMemberNameAndDesc a, FullyQualifiedMemberNameAndDesc b) {
                return a.toString().compareTo(b.toString());
            }
        };

    private static List<FullyQualifiedMemberNameAndDesc> sortFqns(Set<FullyQualifiedMemberNameAndDesc> in) {
        List<FullyQualifiedMemberNameAndDesc> out = new ArrayList<FullyQualifiedMemberNameAndDesc>(in);
        Collections.sort(out, FQN_ORDER);
        return out;
    }

    /** byType entries sorted by owner internal name — FQN/Type aren't Comparable. */
    private static List<Map.Entry<Type, Set<MemberNameAndDesc>>> sortTypes(Map<Type, Set<MemberNameAndDesc>> byType) {
        List<Map.Entry<Type, Set<MemberNameAndDesc>>> out = new ArrayList<Map.Entry<Type, Set<MemberNameAndDesc>>>(byType.entrySet());
        Collections.sort(out, new Comparator<Map.Entry<Type, Set<MemberNameAndDesc>>>() {
            @Override
            public int compare(Map.Entry<Type, Set<MemberNameAndDesc>> a, Map.Entry<Type, Set<MemberNameAndDesc>> b) {
                return a.getKey().getInternalName().compareTo(b.getKey().getInternalName());
            }
        });
        return out;
    }

    private final ShadeContext context;

    public ShadeClosureBarrier(ShadeContext context) {
        this.context = context;
    }

    @Override
    public void run(TransformContext ctx) {
        indexVariants(ctx);
        for (int position : context.inputIndex.versions()) {
            computePosition(ctx, position);
        }
        shadeVariants(ctx);
    }

    /** Indexes captured variants at their slots — they never reach the scan transform. */
    private void indexVariants(TransformContext ctx) {
        if (ctx.variants == null) {
            return;
        }
        for (List<ClassNode> variantSet : ctx.variants.values()) {
            for (ClassNode node : variantSet) {
                final ShadeContext shade = context;
                context.inputIndex.add(node, context.positionOf(node, ctx.flags), new RefExtractor.Filter() {
                    @Override
                    public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
                        return shade.apiClasses.contains(member.getOwner());
                    }
                });
            }
        }
    }

    /**
     * One jar slot's closure product — mirrors {@code ApiShader.shadeApis}'s
     * per-version body minus the writes: closure, inline candidates, prefix
     * remap, api-class emission, resource copies.
     */
    private void computePosition(TransformContext ctx, final int position) {
        Pair<Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>, Set<String>> required =
            context.apiIndex.closureWithUsages(context.inputIndex.usages(position), position);
        context.closures.put(position, required);

        Set<Type> keys = new HashSet<Type>();
        for (Map.Entry<Type, Set<Integer>> e : context.inputIndex.keys().entrySet()) {
            if (e.getValue().contains(position)) {
                keys.add(e.getKey());
            }
        }

        if (ctx.flags.shadeInlining) {
            // api member -> sole consuming input class; inverted for per-class lookup
            Map<FullyQualifiedMemberNameAndDesc, Type> candidates =
                ApiShader.inlineCandidates(ctx.logger, required.getFirst(), keys);
            Map<Type, Set<FullyQualifiedMemberNameAndDesc>> targets =
                new HashMap<Type, Set<FullyQualifiedMemberNameAndDesc>>();
            for (FullyQualifiedMemberNameAndDesc apiMember : sortFqns(candidates.keySet())) {
                required.getFirst().remove(apiMember);
                required.getFirst().remove(new FullyQualifiedMemberNameAndDesc(apiMember.getOwner(), null, null));
                Set<FullyQualifiedMemberNameAndDesc> members = targets.get(candidates.get(apiMember));
                if (members == null) {
                    members = new TreeSet<FullyQualifiedMemberNameAndDesc>(FQN_ORDER);
                    targets.put(candidates.get(apiMember), members);
                }
                members.add(apiMember);
            }
            context.inlineTargets.put(position, targets);
        }

        Map<Type, Set<MemberNameAndDesc>> byType = ApiShader.byType(required.getFirst().keySet());
        Map<String, String> remap = new HashMap<String, String>();
        for (Type type : byType.keySet()) {
            remap.put(type.getInternalName(), context.prefix + type.getInternalName());
        }
        SimpleRemapper remapper = new SimpleRemapper(remap);
        context.remappers.put(position, remapper);

        emitApiClasses(ctx, position, byType, remapper);
        emitResources(ctx, required.getSecond());
    }

    /**
     * The pruned, prefixed api classes for one slot — same recipe as the jar
     * flow: copy the retained node, strip unused members (non-enum), visit
     * through the prefix remapper. The copy is new in this flow: the jar
     * code prunes the retained node itself, letting an earlier slot's
     * pruning leak into a later slot's resolution of the same type.
     */
    private void emitApiClasses(TransformContext ctx, int position, Map<Type, Set<MemberNameAndDesc>> byType, SimpleRemapper remapper) {
        for (Map.Entry<Type, Set<MemberNameAndDesc>> type : sortTypes(byType)) {
            ClassNode retained = context.apiIndex.nodeFor(type.getKey(), position);
            if (retained == null) {
                ctx.diagnostics.warn("shade", "no api class at slot " + position + " for required type " + type.getKey().getInternalName());
                continue;
            }
            ClassNode working = new ClassNode();
            retained.accept(working);
            if ((working.access & Opcodes.ACC_ENUM) == 0) {
                Set<MemberNameAndDesc> members = type.getValue();
                Iterator<MethodNode> iter = working.methods.iterator();
                while (iter.hasNext()) {
                    MethodNode method = iter.next();
                    if (!members.contains(new MemberNameAndDesc(method.name, Type.getMethodType(method.desc)))) {
                        iter.remove();
                    }
                }
                Iterator<FieldNode> fiter = working.fields.iterator();
                while (fiter.hasNext()) {
                    FieldNode field = fiter.next();
                    if (!members.contains(new MemberNameAndDesc(field.name, Type.getType(field.desc)))) {
                        fiter.remove();
                    }
                }
            }
            ClassNode prefixed = new ClassNode();
            working.accept(new ClassRemapper(prefixed, remapper));
            context.emit(prefixed, ctx.flags);
        }
    }

    /** Api resource copies for one slot — signature files never carried. */
    private void emitResources(TransformContext ctx, Set<String> resources) {
        for (String resource : new TreeSet<String>(resources)) {
            if (ManifestUtils.isSignatureFile(resource)) {
                continue;
            }
            for (Path apiRoot : context.apiRoots) {
                Path inPath = apiRoot.resolve(resource);
                try {
                    if (Files.exists(inPath)) {
                        ctx.emitResource(resource, Files.readAllBytes(inPath));
                        break;
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read api resource " + inPath, e);
                }
            }
        }
    }

    /**
     * Shades each captured variant in place — inline candidates and the
     * prefix remap for its own slot — and writes the transformed node back
     * into {@code ctx.variants} so the writer sees the shaded form.
     */
    private void shadeVariants(TransformContext ctx) {
        if (ctx.variants == null) {
            return;
        }
        for (Map.Entry<Object, List<ClassNode>> entry : ctx.variants.entrySet()) {
            List<ClassNode> nodes = entry.getValue();
            List<ClassNode> shaded = new ArrayList<ClassNode>(nodes.size());
            for (ClassNode node : nodes) {
                int position = context.positionOf(node, ctx.flags);
                ClassNode out = ShadeInlineTransform.inline(node, position, context, ctx);
                shaded.add(ShadePrefixTransform.remap(out, position, context));
            }
            nodes.clear();
            nodes.addAll(shaded);
        }
    }

}
