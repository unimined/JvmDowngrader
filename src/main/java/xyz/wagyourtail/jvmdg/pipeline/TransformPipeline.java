package xyz.wagyourtail.jvmdg.pipeline;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The version-free engine: an ordered list of {@link Stage}s that classes
 * flow through. Knows nothing about Java versions — a registrar emits the
 * stage list and the pipeline executes it.
 * <p>
 * Execution model (sequential): each class flows through a stage's transforms
 * in order, emitting into the stage's shared {@link TransformWorklist}; the
 * whole worklist — primaries plus emitted classes — becomes the next stage's
 * input. Stage {@link Stage#captureLabels} deep-copy the class set at stage
 * entry into {@link #variants()} (the multi-release mechanism).
 * <p>
 * Parallel note: classes are pipelineable between barrier stages — each
 * class flows through contiguous barrier-free stages independently, syncing
 * only at true barriers. This run() is the sequential reference; the fusion
 * work builds on it without changing the semantics it encodes.
 */
public final class TransformPipeline {

    private final List<Stage> stages = new ArrayList<Stage>();
    private final Map<Object, List<ClassNode>> variants = new LinkedHashMap<Object, List<ClassNode>>();

    public TransformPipeline addStage(Stage stage) {
        stages.add(stage);
        return this;
    }

    public TransformPipeline addStages(List<Stage> list) {
        stages.addAll(list);
        return this;
    }

    /** Unmodifiable view of the registered stage list. */
    public List<Stage> stages() {
        return stages;
    }

    /**
     * Runs the pipeline.
     * @param input classes in processing order
     * @return the final class set, name-ordered
     */
    public List<ClassNode> run(List<ClassNode> input, TransformContext ctx) throws IOException {
        // barrier hooks see captures through the context's live variants view
        ctx.variants = variants;
        List<ClassNode> current = input;
        for (Stage stage : stages) {
            ctx.current = stage;
            // worklist binds before onEnter so barrier hooks can emit
            // classes that join the remaining stages
            TransformWorklist next = new TransformWorklist();
            ctx.extras = next;
            if (stage.onEnter != null) {
                stage.onEnter.run(ctx);
            }
            for (Object label : stage.captureLabels) {
                List<ClassNode> snapshot = new ArrayList<ClassNode>(current.size());
                for (ClassNode c : current) {
                    // Integer labels are class-file versions — capture only
                    // classes actually at that version (a mixed-version set
                    // carries others past the boundary untouched)
                    if (label instanceof Integer && c.version != (Integer) label) {
                        continue;
                    }
                    snapshot.add(copy(c));
                }
                variants.put(label, snapshot);
            }
            List<ClassTransform> transforms = flagsNoFuse(ctx) ? stage.transforms : fuse(stage.transforms);
            for (ClassNode clazz : current) {
                if (stage.filter != null && !stage.filter.apply(clazz)) {
                    next.enqueue(clazz);
                    continue;
                }
                if (stage.precondition != null) {
                    stage.precondition.check(clazz);
                }
                ClassNode node = clazz;
                for (ClassTransform t : transforms) {
                    try {
                        node = t.apply(node, ctx);
                    } catch (IOException e) {
                        throw new TransformException(stage.name, t, node.name, e);
                    } catch (RuntimeException e) {
                        throw new TransformException(stage.name, t, node.name, e);
                    }
                    if (node == null) {
                        break;
                    }
                }
                if (node != null) {
                    next.enqueue(node);
                }
            }
            current = next.drain();
            if (stage.onExit != null) {
                stage.onExit.run(ctx);
            }
        }
        ctx.current = null;
        return current;
    }

    /** Variant snapshots captured at stage entries, label → classes. */
    public Map<Object, List<ClassNode>> variants() {
        return variants;
    }

    private static boolean flagsNoFuse(TransformContext ctx) {
        return ctx.flags != null && ctx.flags.debugNoFuse;
    }

    /**
     * Composes runs of adjacent {@link FusibleTransform}s into
     * {@link FusedTransform}s. Fusion is strictly sequential composition —
     * each fused run applies its participants in list order — so the fused
     * and {@code debugNoFuse} paths produce identical output by
     * construction; participants that restructure execution opt in through
     * {@link FusibleTransform#canFuseWith}/{@link FusibleTransform#fuse}.
     */
    public static List<ClassTransform> fuse(List<ClassTransform> transforms) {
        List<ClassTransform> out = new ArrayList<ClassTransform>(transforms.size());
        FusibleTransform pending = null;
        for (ClassTransform t : transforms) {
            if (pending == null) {
                if (t instanceof FusibleTransform) {
                    pending = (FusibleTransform) t;
                } else {
                    out.add(t);
                }
            } else if (t instanceof FusibleTransform && pending.canFuseWith((FusibleTransform) t)) {
                pending = pending.fuse((FusibleTransform) t);
            } else {
                out.add(pending);
                pending = t instanceof FusibleTransform ? (FusibleTransform) t : null;
                if (pending == null) {
                    out.add(t);
                }
            }
        }
        if (pending != null) {
            out.add(pending);
        }
        return out;
    }

    /**
     * Deep-copies a class for variant capture. The indy bootstrap-args array
     * is cloned explicitly: {@code stubBSMArgs} mutates it in place, and a
     * plain {@code ClassNode.accept} copy would share it with the original.
     */
    public static ClassNode copy(ClassNode c) {
        final ClassNode copy = new ClassNode();
        c.accept(new ClassVisitor(Opcodes.ASM9, copy) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments.clone());
                    }
                };
            }
        });
        return copy;
    }

}
