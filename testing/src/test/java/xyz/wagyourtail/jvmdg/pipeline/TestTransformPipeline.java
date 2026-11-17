package xyz.wagyourtail.jvmdg.pipeline;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestTransformPipeline {

    private static ClassNode node(String name) {
        ClassNode n = new ClassNode();
        n.name = name;
        return n;
    }

    private static TransformContext ctx() {
        return new TransformContext(null, new Flags(), new Logger("test", Logger.Level.FATAL, false, System.out));
    }

    /** Records every class it sees, in visit order. */
    private static class Recorder implements ClassTransform {
        final String tag;
        final List<String> log;

        Recorder(String tag, List<String> log) {
            this.tag = tag;
            this.log = log;
        }

        @Override
        public ClassNode apply(ClassNode node, TransformContext ctx) {
            log.add(tag + ":" + node.name);
            return node;
        }
    }

    private static List<String> names(List<ClassNode> nodes) {
        List<String> out = new ArrayList<String>();
        for (ClassNode n : nodes) {
            out.add(n.name);
        }
        return out;
    }

    @Test
    public void classesFlowThroughTransformsPerClass() throws IOException {
        List<String> log = new ArrayList<String>();
        Stage s1 = new Stage("s1").transform(new Recorder("t1", log)).transform(new Recorder("t2", log));
        TransformPipeline p = new TransformPipeline().addStage(s1);
        p.run(Arrays.asList(node("a/A"), node("a/B")), ctx());
        // per-class inner loop: each class sees every transform before the next class
        assertEquals(Arrays.asList("t1:a/A", "t2:a/A", "t1:a/B", "t2:a/B"), log);
    }

    @Test
    public void stagesRunInRegistrationOrder() throws IOException {
        List<String> log = new ArrayList<String>();
        TransformPipeline p = new TransformPipeline()
            .addStage(new Stage("s1").transform(new Recorder("s1", log)))
            .addStage(new Stage("s2").transform(new Recorder("s2", log)));
        p.run(Collections.singletonList(node("a/A")), ctx());
        assertEquals(Arrays.asList("s1:a/A", "s2:a/A"), log);
    }

    @Test
    public void emittedClassJoinsNextStage() throws IOException {
        final List<String> log = new ArrayList<String>();
        Stage s1 = new Stage("s1").transform(new ClassTransform() {
            @Override
            public ClassNode apply(ClassNode node, TransformContext ctx) {
                ctx.extras.enqueue(node("b/Helper"));
                return node;
            }
        });
        Stage s2 = new Stage("s2").transform(new Recorder("s2", log));
        TransformPipeline p = new TransformPipeline().addStage(s1).addStage(s2);
        List<ClassNode> out = p.run(Collections.singletonList(node("a/A")), ctx());
        assertEquals(Arrays.asList("s2:a/A", "s2:b/Helper"), log);
        assertEquals(Arrays.asList("a/A", "b/Helper"), names(out));
    }

    @Test
    public void extrasPassAppliesToEmittedClasses() throws IOException {
        final List<String> log = new ArrayList<String>();
        ClassTransform emitOnce = new ClassTransform() {
            @Override
            public ClassNode apply(ClassNode node, TransformContext ctx) {
                log.add("t:" + node.name);
                if (node.name.equals("a/A")) {
                    ctx.extras.enqueue(node("b/Helper"));
                }
                return node;
            }
        };
        Stage s = new Stage("s").transform(new ExtrasApplyTransform(emitOnce));
        TransformPipeline p = new TransformPipeline().addStage(s);
        List<ClassNode> out = p.run(Collections.singletonList(node("a/A")), ctx());
        // primary first, then the emitted class — same transform, same pass
        assertEquals(Arrays.asList("t:a/A", "t:b/Helper"), log);
        assertEquals(Arrays.asList("a/A", "b/Helper"), names(out));
    }

    @Test
    public void classEmittedDuringExtrasPassSkipsCurrentTransform() throws IOException {
        final List<String> log = new ArrayList<String>();
        ClassTransform emitChain = new ClassTransform() {
            @Override
            public ClassNode apply(ClassNode node, TransformContext ctx) {
                log.add("t:" + node.name);
                if (node.name.equals("a/A")) {
                    ctx.extras.enqueue(node("b/Helper"));
                } else if (node.name.equals("b/Helper")) {
                    // emitted during the extras pass — invisible to this pass
                    ctx.extras.enqueue(node("c/Late"));
                }
                return node;
            }
        };
        Stage s = new Stage("s").transform(new ExtrasApplyTransform(emitChain));
        TransformPipeline p = new TransformPipeline().addStage(s);
        List<ClassNode> out = p.run(Collections.singletonList(node("a/A")), ctx());
        assertEquals(Arrays.asList("t:a/A", "t:b/Helper"), log);
        assertEquals(Arrays.asList("a/A", "b/Helper", "c/Late"), names(out));
    }

    @Test
    public void droppedPrimarySkipsExtrasPass() throws IOException {
        final List<String> log = new ArrayList<String>();
        Stage s = new Stage("s")
            .transform(new ClassTransform() {
                @Override
                public ClassNode apply(ClassNode node, TransformContext ctx) {
                    ctx.extras.enqueue(node("b/Helper")); // in worklist before the dropping transform
                    return node;
                }
            })
            .transform(new ExtrasApplyTransform(new ClassTransform() {
                @Override
                public ClassNode apply(ClassNode node, TransformContext ctx) {
                    log.add("t:" + node.name);
                    return null; // drops the primary
                }
            }));
        TransformPipeline p = new TransformPipeline().addStage(s);
        List<ClassNode> out = p.run(Collections.singletonList(node("a/A")), ctx());
        // stubWithExtras semantics: a dropped primary skips the extras pass —
        // b/Helper survives into output untransformed (never saw t)
        assertEquals(Collections.singletonList("t:a/A"), log);
        assertEquals(Collections.singletonList("b/Helper"), names(out));
    }

    @Test
    public void nullTransformOnExtraDropsIt() throws IOException {
        final Stage emit = new Stage("emit").transform(new ClassTransform() {
            @Override
            public ClassNode apply(ClassNode node, TransformContext ctx) {
                ctx.extras.enqueue(node("b/Helper"));
                return node;
            }
        });
        Stage s = new Stage("s").transform(new ExtrasApplyTransform(new ClassTransform() {
            @Override
            public ClassNode apply(ClassNode node, TransformContext ctx) {
                return node.name.equals("b/Helper") ? null : node;
            }
        }));
        TransformPipeline p = new TransformPipeline().addStage(emit).addStage(s);
        List<ClassNode> out = p.run(Collections.singletonList(node("a/A")), ctx());
        assertEquals(Collections.singletonList("a/A"), names(out));
    }

    @Test
    public void captureSnapshotAtStageEntry() throws IOException {
        Stage s1 = new Stage("s1").transform(new ClassTransform() {
            @Override
            public ClassNode apply(ClassNode node, TransformContext ctx) {
                node.name = node.name + "/mutated";
                return node;
            }
        });
        Stage s2 = new Stage("s2").capture("variant-11");
        TransformPipeline p = new TransformPipeline().addStage(s1).addStage(s2);
        List<ClassNode> out = p.run(Collections.singletonList(node("a/A")), ctx());
        assertEquals(Collections.singletonList("a/A/mutated"), names(out));
        // the variant captured s1's OUTPUT (stage entry of s2)
        assertEquals(Collections.singletonList("a/A/mutated"), names(p.variants().get("variant-11")));
    }

    @Test
    public void capturedVariantIsDeepCopy() throws IOException {
        Stage s1 = new Stage("s1").capture("v").transform(new ClassTransform() {
            @Override
            public ClassNode apply(ClassNode node, TransformContext ctx) {
                node.version = 99;
                return node;
            }
        });
        ClassNode in = node("a/A");
        in.version = 61;
        TransformPipeline p = new TransformPipeline().addStage(s1);
        p.run(Collections.singletonList(in), ctx());
        ClassNode captured = p.variants().get("v").get(0);
        assertNotSame(in, captured);
        assertEquals(61, captured.version); // unaffected by the later mutation
        assertEquals(99, in.version);
    }

    @Test
    public void copyClonesIndyBsmArgs() {
        ClassNode in = node("a/A");
        MethodNode m = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "m", "()V", null, null);
        m.instructions.add(new org.objectweb.asm.tree.InvokeDynamicInsnNode(
            "x", "()V", new Handle(Opcodes.H_INVOKESTATIC, "a/B", "bsm", "()V", false),
            new Object[]{new Handle(Opcodes.H_INVOKESTATIC, "a/C", "h", "()V", false)}));
        in.methods.add(m);
        ClassNode copy = TransformPipeline.copy(in);
        org.objectweb.asm.tree.InvokeDynamicInsnNode orig =
            (org.objectweb.asm.tree.InvokeDynamicInsnNode) in.methods.get(0).instructions.get(0);
        org.objectweb.asm.tree.InvokeDynamicInsnNode dup =
            (org.objectweb.asm.tree.InvokeDynamicInsnNode) copy.methods.get(0).instructions.get(0);
        // stubBSMArgs mutates bsmArgs in place — the clone must not share the array
        orig.bsmArgs[0] = new Handle(Opcodes.H_INVOKESTATIC, "a/D", "h2", "()V", false);
        assertEquals("a/C", ((Handle) dup.bsmArgs[0]).getOwner());
    }

    @Test
    public void preconditionRunsBeforeTransforms() throws IOException {
        final List<String> log = new ArrayList<String>();
        Stage s = new Stage("s")
            .precondition(new ClassPrecondition() {
                @Override
                public void check(ClassNode node) {
                    if (node.version != 61) {
                        throw new IllegalArgumentException("bad version");
                    }
                }
            })
            .transform(new Recorder("t", log));
        ClassNode bad = node("a/A");
        bad.version = 55;
        TransformPipeline p = new TransformPipeline().addStage(s);
        try {
            p.run(Collections.singletonList(bad), ctx());
            fail("expected precondition failure");
        } catch (IllegalArgumentException expected) {
        }
        assertTrue(log.isEmpty());
    }

    @Test
    public void barrierHooksRunOnceAroundClassLoop() throws IOException {
        final List<String> log = new ArrayList<String>();
        Stage s = new Stage("s")
            .onEnter(new StageHook() {
                @Override
                public void run(TransformContext ctx) {
                    log.add("enter");
                }
            })
            .transform(new Recorder("t", log))
            .onExit(new StageHook() {
                @Override
                public void run(TransformContext ctx) {
                    log.add("exit");
                }
            });
        TransformPipeline p = new TransformPipeline().addStage(s);
        p.run(Arrays.asList(node("a/A"), node("a/B")), ctx());
        assertEquals(Arrays.asList("enter", "t:a/A", "t:a/B", "exit"), log);
    }

    @Test
    public void outputIsNameOrdered() throws IOException {
        TransformPipeline p = new TransformPipeline().addStage(new Stage("s"));
        List<ClassNode> out = p.run(Arrays.asList(node("z/Z"), node("a/A"), node("m/M")), ctx());
        assertEquals(Arrays.asList("a/A", "m/M", "z/Z"), names(out));
    }

    @Test
    public void primaryEnqueueOverwritesEmittedSameName() throws IOException {
        Stage s1 = new Stage("s1").transform(new ClassTransform() {
            @Override
            public ClassNode apply(ClassNode node, TransformContext ctx) {
                ClassNode emitted = node("a/A");
                emitted.version = 42;
                ctx.extras.enqueue(emitted);
                return node; // primary enqueued after the emission — wins on name
            }
        });
        TransformPipeline p = new TransformPipeline().addStage(s1);
        List<ClassNode> out = p.run(Collections.singletonList(node("a/A")), ctx());
        assertEquals(1, out.size());
        assertEquals(0, out.get(0).version); // the returned primary, not the emitted v42
    }

    @Test
    public void emittedClassWithSameNameReplacesWhenPrimaryDrops() throws IOException {
        Stage s1 = new Stage("s1").transform(new ClassTransform() {
            @Override
            public ClassNode apply(ClassNode node, TransformContext ctx) {
                ClassNode emitted = node("a/A");
                emitted.version = 42;
                ctx.extras.enqueue(emitted);
                return null;
            }
        });
        TransformPipeline p = new TransformPipeline().addStage(s1);
        List<ClassNode> out = p.run(Collections.singletonList(node("a/A")), ctx());
        assertEquals(1, out.size());
        assertEquals(42, out.get(0).version);
    }

    /** A recorder that opts into fusion by straight composition. */
    private static class FusibleRecorder extends FusibleTransform {
        final String tag;
        final List<String> log;
        final boolean canFuse;

        FusibleRecorder(String tag, List<String> log) {
            this(tag, log, true);
        }

        FusibleRecorder(String tag, List<String> log, boolean canFuse) {
            this.tag = tag;
            this.log = log;
            this.canFuse = canFuse;
        }

        @Override
        public boolean canFuseWith(FusibleTransform next) {
            return canFuse;
        }

        @Override
        public FusibleTransform fuse(FusibleTransform next) {
            return new FusedTransform(Arrays.<FusibleTransform>asList(this, next));
        }

        @Override
        public ClassNode apply(ClassNode node, TransformContext ctx) {
            log.add(tag + ":" + node.name);
            return node;
        }
    }

    @Test
    public void fuseComposesAdjacentFusiblesOnly() {
        List<String> log = new ArrayList<String>();
        FusibleRecorder a = new FusibleRecorder("a", log);
        FusibleRecorder b = new FusibleRecorder("b", log);
        Recorder plain = new Recorder("p", log);
        FusibleRecorder c = new FusibleRecorder("c", log);
        List<ClassTransform> fused = TransformPipeline.fuse(Arrays.<ClassTransform>asList(a, b, plain, c));
        assertEquals(3, fused.size());
        assertTrue(fused.get(0) instanceof FusedTransform);
        assertSame(plain, fused.get(1));
        assertSame(c, fused.get(2));
        assertEquals(Arrays.<FusibleTransform>asList(a, b), ((FusedTransform) fused.get(0)).delegates());
    }

    @Test
    public void canFuseWithFalseKeepsPairSeparate() {
        List<String> log = new ArrayList<String>();
        FusibleRecorder a = new FusibleRecorder("a", log, false);
        FusibleRecorder b = new FusibleRecorder("b", log);
        List<ClassTransform> fused = TransformPipeline.fuse(Arrays.<ClassTransform>asList(a, b));
        assertEquals(2, fused.size());
        assertSame(a, fused.get(0));
        assertSame(b, fused.get(1));
    }

    @Test
    public void fusedVisitOrderMatchesSequential() throws IOException {
        // same stage list run fused and unfused — identical visit order and output
        List<String> fusedLog = new ArrayList<String>();
        Stage sf = new Stage("s").transform(new FusibleRecorder("a", fusedLog))
                .transform(new FusibleRecorder("b", fusedLog))
                .transform(new ExtrasApplyTransform(new FusibleRecorder("c", fusedLog)));
        TransformContext fctx = ctx();
        List<ClassNode> fusedOut = new TransformPipeline().addStage(sf)
                .run(Arrays.asList(node("a/A"), node("b/B")), fctx);

        List<String> seqLog = new ArrayList<String>();
        Stage ss = new Stage("s").transform(new FusibleRecorder("a", seqLog))
                .transform(new FusibleRecorder("b", seqLog))
                .transform(new ExtrasApplyTransform(new FusibleRecorder("c", seqLog)));
        TransformContext sctx = ctx();
        sctx.flags.debugNoFuse = true;
        List<ClassNode> seqOut = new TransformPipeline().addStage(ss)
                .run(Arrays.asList(node("a/A"), node("b/B")), sctx);

        assertEquals(seqLog, fusedLog);
        assertEquals(names(seqOut), names(fusedOut));
    }

    @Test
    public void debugNoFuseRunsUnfusedList() throws IOException {
        // with the flag set the stage's transforms execute as written —
        // observable because a mid-run non-fusible boundary splits the same
        // way in both modes; the flag only changes fused-list construction
        List<String> log = new ArrayList<String>();
        Stage s = new Stage("s").transform(new FusibleRecorder("a", log))
                .transform(new FusibleRecorder("b", log));
        TransformContext c = ctx();
        c.flags.debugNoFuse = true;
        new TransformPipeline().addStage(s).run(Collections.singletonList(node("a/A")), c);
        assertEquals(Arrays.asList("a:a/A", "b:a/A"), log);
    }

    @Test
    public void fusedTransformAppliesNullShortCircuit() throws IOException {
        // a delegate returning null must stop the fused run — same as the
        // engine loop's break
        List<String> log = new ArrayList<String>();
        FusedTransform fused = new FusedTransform(Arrays.<FusibleTransform>asList(
                new FusibleRecorder("a", log),
                new FusibleTransform() {
                    @Override
                    public boolean canFuseWith(FusibleTransform next) {
                        return true;
                    }

                    @Override
                    public FusibleTransform fuse(FusibleTransform next) {
                        return new FusedTransform(Arrays.asList(this, next));
                    }

                    @Override
                    public ClassNode apply(ClassNode node, TransformContext ctx) {
                        log.add("drop:" + node.name);
                        return null;
                    }
                },
                new FusibleRecorder("b", log)));
        TransformContext c = ctx();
        Stage s = new Stage("s").transform(fused);
        List<ClassNode> out = new TransformPipeline().addStage(s).run(Collections.singletonList(node("a/A")), c);
        assertTrue(out.isEmpty());
        assertEquals(Arrays.asList("a:a/A", "drop:a/A"), log); // b never ran
    }

}

