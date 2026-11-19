package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The mechanical half of a stub pass: rewrites every structural {@code Type}
 * slot in an instruction through a {@link TypeRemap} — member insn
 * owner/desc, indy desc and bootstrap-handle fields, {@code ldc} Type
 * constants, stack-map frame slots, {@code multianewarray}, and
 * {@code TypeInsnNode} element types (including the retro-supertype
 * {@code jvmdg$instanceof}/{@code jvmdg$checkcast} rewrites).
 * <p>
 * Member-insn dispatch ({@link MemberResolver} → {@link MemberStubTransform}),
 * bootstrap-arg rewriting and {@code ConstantDynamic} stay with the caller —
 * those resolve through member registries, not the type remap.
 */
public class StructuralRemapTransform {

    private final TypeRemap remap;

    public StructuralRemapTransform(TypeRemap remap) {
        this.remap = remap;
    }

    /**
     * Whether {@code name} is generated dispatch machinery — the
     * {@code xyz/wagyourtail/jvmdg/gen/} dispatchers and the
     * {@code $jvmdg$}/{@code $Ov$} generated siblings. Their
     * {@code instanceof}/{@code checkcast}/class-query sites are literal
     * fast-paths, never user sites to rewrite: routing them would produce
     * self-recursive dispatchers (the api jar's self-downgrade rewrites the
     * emitted {@code instanceof I} inside {@code gen/gained/<I>} into a call
     * to its own {@code jvmdg$isInstance}).
     */
    static boolean isGeneratedDispatch(String name) {
        return name.startsWith("xyz/wagyourtail/jvmdg/gen/")
            || name.contains("$jvmdg$")
            || name.contains("$Ov$");
    }

    /**
     * Remaps the structural Type slots of {@code insn} in place.
     * @param ownerName the containing class's internal name — generated
     *        dispatch machinery skips the retro/gained site rewrites
     *        (mechanical remaps still apply)
     * @param ceiling the containing class's authored class-file version —
     *        the membership ceiling gained-supertype sites emit
     * @return a replacement insn list for the {@code TypeInsnNode}
     *         retro-supertype rewrite ({@code instanceof}/{@code checkcast} →
     *         ldc + invokestatic), or null when the insn continues as-is.
     */
    public InsnList remapInsn(AbstractInsnNode insn, Set<String> warnings, int ceiling, String ownerName) {
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) insn;
            min.owner = remap.remap(Type.getObjectType(min.owner), warnings).getInternalName();
            min.desc = remap.remap(Type.getMethodType(min.desc), warnings).getDescriptor();
        } else if (insn instanceof FieldInsnNode) {
            FieldInsnNode fin = (FieldInsnNode) insn;
            fin.owner = remap.remap(Type.getObjectType(fin.owner), warnings).getInternalName();
            fin.desc = remap.remap(Type.getType(fin.desc), warnings).getDescriptor();
        } else if (insn instanceof TypeInsnNode) {
            TypeInsnNode tin = (TypeInsnNode) insn;
            if (isGeneratedDispatch(ownerName)) {
                // mechanical remap still applies — only the retro/gained
                // dispatcher rewrites below are suppressed for generated
                // dispatch machinery
                tin.desc = remap.remap(Type.getObjectType(tin.desc), warnings).getInternalName();
                return null;
            }
            return remapTypeInsn(tin, warnings, ceiling);
        } else if (insn instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
            indy.desc = remap.remap(Type.getMethodType(indy.desc), warnings).getDescriptor();
            indy.bsm = new Handle(
                indy.bsm.getTag(),
                remap.remap(Type.getObjectType(indy.bsm.getOwner()), warnings).getInternalName(),
                indy.bsm.getName(),
                remap.remap(Type.getMethodType(indy.bsm.getDesc()), warnings).getDescriptor(),
                indy.bsm.isInterface()
            );
        } else if (insn instanceof MultiANewArrayInsnNode) {
            MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) insn;
            manain.desc = remap.remap(Type.getType(manain.desc), warnings).getDescriptor();
        } else if (insn instanceof LdcInsnNode) {
            LdcInsnNode ldc = (LdcInsnNode) insn;
            if (ldc.cst instanceof Type) {
                ldc.cst = remap.remap((Type) ldc.cst, warnings);
            }
        } else if (insn instanceof FrameNode) {
            FrameNode fn = (FrameNode) insn;
            if (fn.local != null) {
                for (int j = 0; j < fn.local.size(); j++) {
                    Object o = fn.local.get(j);
                    if (o instanceof String) {
                        fn.local.set(j, remap.remap(Type.getObjectType((String) o), warnings).getInternalName());
                    }
                }
            }
            if (fn.stack != null) {
                for (int j = 0; j < fn.stack.size(); j++) {
                    Object o = fn.stack.get(j);
                    if (o instanceof String) {
                        fn.stack.set(j, remap.remap(Type.getObjectType((String) o), warnings).getInternalName());
                    }
                }
            }
        }
        return null;
    }

    /**
     * TypeInsnNode handling: array/object element remap, and for stubbed
     * object types the retro-supertype rewrite — a {@code jvmdg$instanceof}/
     * {@code jvmdg$checkcast} hook always wins, else a retro-capable adapter
     * emits {@code RetroTypes.isInstance}/{@code cast} / the declared
     * {@code wrap} factory — returning the replacement insns, or null.
     * Retro emission gates on {@code mode} alone, never {@code implementedBy}:
     * membership lives in the {@code META-INF/jvmdg/retrotypes} runtime table,
     * so the scan and idx load paths must emit identically.
     */
    private InsnList remapTypeInsn(TypeInsnNode insn, Set<String> warnings, int ceiling) {
        Type desc = Type.getObjectType(insn.desc);
        switch (desc.getSort()) {
            case Type.ARRAY:
                Type type = desc.getElementType();
                if (insn.getOpcode() == Opcodes.INSTANCEOF && type.getSort() == Type.OBJECT) {
                    String disp = remap.gainedFor(type);
                    if (disp != null) {
                        // gained-element arrays: I[k] covariance reduces to
                        // the base element's membership — the dispatcher
                        // strips dims itself. checkcast stays native:
                        // member arrays can't be fabricated (covariance is
                        // a VM property), so its CCE is the honest answer
                        InsnList out = new InsnList();
                        out.add(intInsn(ceiling));
                        out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                            "jvmdg$isArrayInstance", "(Ljava/lang/Object;I)Z", false));
                        return out;
                    }
                }
                ClassStubEntry elementStub = remap.stubFor(type);
                if (elementStub != null && elementStub.mode == Adapter.Mode.SUPERTYPE &&
                    (insn.getOpcode() == Opcodes.INSTANCEOF || insn.getOpcode() == Opcodes.CHECKCAST)) {
                    // retro-supertype array form: element-type membership
                    // check — own-interface arrays stay on the element remap
                    // (JDK impls are not adapter instances)
                    remap.warnUnstubbed(elementStub.target, warnings);
                    InsnList out = new InsnList();
                    out.add(dispCall(elementStub.stubType, insn.getOpcode() == Opcodes.INSTANCEOF
                        ? "isArrayInstance" : "castArray",
                        insn.getOpcode() == Opcodes.INSTANCEOF));
                    if (insn.getOpcode() == Opcodes.CHECKCAST) {
                        out.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getType(
                            desc.getDescriptor().substring(0, desc.getDimensions()) +
                                elementStub.target.getDescriptor()).getInternalName()));
                    }
                    return out;
                }
                if (elementStub != null) {
                    type = elementStub.target;
                }
                if (remap.gainedFor(type) == null) {
                    // gained interfaces are real at the output — not a coverage gap
                    remap.warnUnstubbed(type, warnings);
                }
                insn.desc = Type.getType(desc.getDescriptor().substring(0, desc.getDimensions()) + type.getDescriptor()).getInternalName();
                return null;
            case Type.OBJECT:
                ClassStubEntry stub = remap.stubFor(desc);
                if (stub != null) {
                    switch (insn.getOpcode()) {
                        case Opcodes.INSTANCEOF:
                            if (stub.instanceofHook != null) {
                                // baked jvmdg$ hook from the stub class — explicit override
                                return single(hookInvoke(stub.instanceofHook, stub.itf));
                            }
                            {
                                // every class stub is retro-capable: SUPERTYPE
                                // membership is the whole point, OWN_INTERFACE
                                // rows cover the JDK implementors the stub
                                // interface itself can't see
                                return single(dispCall(stub.stubType, "isInstance", true));
                            }
                        case Opcodes.CHECKCAST:
                            if (stub.checkcastHook != null) {
                                return single(hookInvoke(stub.checkcastHook, stub.itf));
                            }
                            if (stub.mode == Adapter.Mode.SUPERTYPE) {
                                InsnList out = new InsnList();
                                out.add(dispCall(stub.stubType, "cast", false));
                                // membership is enforced; the remapped
                                // checkcast keeps the verifier's stack type
                                out.add(new TypeInsnNode(Opcodes.CHECKCAST, stub.target.getInternalName()));
                                return out;
                            }
                            if (stub.mode == Adapter.Mode.OWN_INTERFACE && stub.wrap != null) {
                                return single(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                    stub.wrap.getOwner().getInternalName(), stub.wrap.getName(),
                                    stub.wrap.getDesc().getDescriptor(),
                                    stub.wrap.getOwner().equals(stub.stubType)
                                        ? stub.itf : isInterface(stub.wrap.getOwner())));
                            }
                            if (stub.mode == Adapter.Mode.OWN_INTERFACE) {
                                // generated wrap dispatcher: adapter-typed
                                // objects pass through, declared impls adapt
                                // members, RetroTypes.wrapImpl is the table
                                // fallback — the trailing checkcast keeps
                                // the verifier's stack type. Emitted for
                                // every OWN_INTERFACE adapter so scan/idx
                                // emit identically
                                InsnList out = new InsnList();
                                out.add(dispCall(stub.stubType, "wrap", false));
                                out.add(new TypeInsnNode(Opcodes.CHECKCAST, stub.target.getInternalName()));
                                return out;
                            }
                            break;
                        default:
                    }
                    insn.desc = stub.target.getInternalName();
                    return null;
                }
                String disp = remap.gainedFor(desc);
                if (disp != null) {
                    // gained-supertype interface: real at the target so the
                    // type slot stays — only the query ops route, with the
                    // class's authored version as the membership ceiling
                    switch (insn.getOpcode()) {
                        case Opcodes.INSTANCEOF: {
                            InsnList out = new InsnList();
                            out.add(intInsn(ceiling));
                            out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                                "jvmdg$isInstance", "(Ljava/lang/Object;I)Z", false));
                            return out;
                        }
                        case Opcodes.CHECKCAST: {
                            // the dispatcher's wrap produces a real I-typed
                            // result — the verifier's stack type a raw
                            // member could never satisfy
                            InsnList out = new InsnList();
                            out.add(intInsn(ceiling));
                            out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                                "jvmdg$cast", "(Ljava/lang/Object;I)" + desc.getDescriptor(), false));
                            return out;
                        }
                        default:
                    }
                }
                remap.warnUnstubbed(desc, warnings);
                break;
        }
        return null;
    }

    /**
     * Rewrites {@code X.class.isInstance(o)}/{@code cast}/{@code isAssignableFrom}
     * calls whose receiver is an {@code ldc}'d retro-capable type. The
     * {@code ldc} is remapped to the target class like any other use, so
     * the call would lose the retro identity — instead the receiver/arg
     * pair is rearranged ({@code swap;pop;ldc name;invokestatic}) to
     * {@link xyz.wagyourtail.jvmdg.stub.runtime.RetroTypes}.
     * <p>
     * Producer analysis is a {@link SourceValue} pass over the
     * <em>pre-remap</em> bytecode — run before the per-insn loop mutates
     * the method. Only uniform producer sets rewrite (every producer an
     * {@code ldc} of the same retro type); mixed or opaque receivers keep
     * the remapped-native behavior. Returns call insn → replacement.
     */
    /** insn-scan bit for {@link MethodAnalysis#hasInsns}: a routed {@code X.class.*} call exists */
    private static final int HAS_CLASS_QUERY = 1;
    /** insn-scan bit for {@link MethodAnalysis#hasInsns}: an {@code AASTORE} exists */
    private static final int HAS_AASTORE = 2;

    /**
     * Per-method analysis artifact — the lazily-computed shared input to
     * every structural consumer for the method's processing window. The
     * insn scan and the fused {@link Analyzer} pass each run at most once
     * per method, on first demand; consumers that need neither pay nothing.
     * Frames describe the instruction sequence <em>at analysis time</em> —
     * consumers must take what they need before the per-insn rewrite loop
     * mutates the method (or key their lookups by insn object, which stays
     * valid across replacement).
     */
    public static final class MethodAnalysis {
        private final String owner;
        private final MethodNode method;
        private int hasInsns = -1;
        private Frame<FusedValue>[] frames;
        private boolean analyzed;

        MethodAnalysis(String owner, MethodNode method) {
            this.owner = owner;
            this.method = method;
        }

        /** bitmask of {@link #HAS_CLASS_QUERY}/{@link #HAS_AASTORE} — one linear scan */
        int hasInsns() {
            if (hasInsns < 0) {
                int mask = 0;
                for (int i = 0; i < method.instructions.size(); i++) {
                    AbstractInsnNode insn = method.instructions.get(i);
                    if (isClassQuery(insn)) {
                        mask |= HAS_CLASS_QUERY;
                    } else if (insn.getOpcode() == Opcodes.AASTORE) {
                        mask |= HAS_AASTORE;
                    }
                }
                hasInsns = mask;
            }
            return hasInsns;
        }

        /**
         * The fused producer+type frames, or {@code null} when the method is
         * unverifiable by {@link Analyzer} — callers leave such insns native
         * (the real verifier is the backstop). Computed once, lazily.
         */
        public Frame<FusedValue>[] frames() {
            if (!analyzed) {
                analyzed = true;
                try {
                    frames = trustingAnalyze(owner, method);
                    if (frames == null) {
                        // no declared frames / JSR-RET — the fixpoint
                        // analyzer handles what trusting can't
                        frames = new Analyzer<>(new FusedInterpreter()).analyze(owner, method);
                    }
                } catch (AnalyzerException | RuntimeException e) {
                    frames = null;
                }
            }
            return frames;
        }
    }

    /** a {@link MethodAnalysis} holder for {@code method} — share it with every consumer of the method */
    public MethodAnalysis analysis(String owner, MethodNode method) {
        return new MethodAnalysis(owner, method);
    }

    public Map<AbstractInsnNode, InsnList> remapClassCalls(String owner, MethodNode method, int ceiling) {
        return remapClassCalls(new MethodAnalysis(owner, method), ceiling);
    }

    /**
     * Routes this method's gained-supertype class queries and
     * gained-element {@code AASTORE}s through their dispatchers.
     * @param ceiling the containing class's authored class-file version —
     *        the membership ceiling every emitted site carries
     */
    public Map<AbstractInsnNode, InsnList> remapClassCalls(MethodAnalysis analysis, int ceiling) {
        Map<AbstractInsnNode, InsnList> out = new HashMap<>();
        if (isGeneratedDispatch(analysis.owner)) {
            // generated machinery's class queries are literal fast-paths —
            // never rewrite them into the dispatchers they implement
            return out;
        }
        int mask = analysis.hasInsns();
        if (mask == 0) {
            return out;
        }
        Frame<FusedValue>[] frames = analysis.frames();
        if (frames == null) {
            return out;
        }
        MethodNode method = analysis.method;
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode insn = method.instructions.get(i);
            if ((mask & HAS_CLASS_QUERY) != 0 && isClassQuery(insn)) {
                Frame<FusedValue> frame = frames[i];
                if (frame == null || frame.getStackSize() < 2) {
                    continue; // unreachable / malformed
                }
                FusedValue receiverV = frame.getStack(frame.getStackSize() - 2);
                if (receiverV == null) {
                    continue; // null-valued slot (dead-code shape) — stay native
                }
                Type receiver = uniformRoutableType(receiverV.producers);
                if (receiver != null) {
                    InsnList postfix = classQueryPostfix((MethodInsnNode) insn, receiver, ceiling);
                    if (postfix != null) {
                        out.put(insn, postfix);
                    }
                }
            } else if ((mask & HAS_AASTORE) != 0 && insn.getOpcode() == Opcodes.AASTORE) {
                Frame<FusedValue> frame = frames[i];
                if (frame == null || frame.getStackSize() < 3) {
                    continue; // unreachable / malformed
                }
                if (insn.getPrevious() instanceof MethodInsnNode
                    && "jvmdg$aastore".equals(((MethodInsnNode) insn.getPrevious()).name)
                    && ((MethodInsnNode) insn.getPrevious()).owner.startsWith("xyz/wagyourtail/jvmdg/gen/gained/")) {
                    // the rewrite keeps a native AASTORE tail, so later stages
                    // would otherwise append their own wrap call — only the
                    // first stage's (authored-ceiling) call may stand
                    continue;
                }
                FusedValue arrayV = frame.getStack(frame.getStackSize() - 3);
                Type array = arrayV == null ? null : arrayV.type;
                if (array == null || array.getSort() != Type.ARRAY
                    || array.getDimensions() != 1) {
                    // scalar-component arrays only — a multi-dim store's
                    // value is itself an array, which can never be an I
                    // member; the real element check handles it natively
                    continue;
                }
                Type element = array.getElementType();
                if (element.getSort() != Type.OBJECT) {
                    continue;
                }
                String disp = remap.gainedFor(element);
                if (disp == null) {
                    continue;
                }
                // [.., I-array, idx, val] → val through jvmdg$aastore
                // (member → $Wrap so the runtime element check passes;
                // the helper throws ArrayStoreException on true
                // mismatches — the same failure the store would have
                // produced), then the native AASTORE. The ceiling is the
                // authored version: the store's legality was proven against
                // the bindings the author could see, so the wrap check must
                // evaluate those, not this stage's output ceiling
                InsnList rewrite = new InsnList();
                rewrite.add(intInsn(ceiling));
                rewrite.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                    "jvmdg$aastore", "(Ljava/lang/Object;I)" + element.getDescriptor(), false));
                rewrite.add(new InsnNode(Opcodes.AASTORE));
                out.put(insn, rewrite);
            }
        }
        return out;
    }

    /**
     * Fused stack-slot value: the producing instruction set (the
     * {@code SourceValue} role — what fed this slot through copies) plus the
     * concrete type (the {@code BasicValue} role, except stock
     * BasicInterpreter collapses refs to {@code java/lang/Object} while this
     * keeps them). One {@link Analyzer} pass serves producer-uniformity
     * consumers (class queries) and type consumers (aastore) alike.
     * <p>
     * Merges union producers and widen types to {@code java/lang/Object} on
     * mismatch — a statically-ambiguous slot stays native, which is the
     * honest unrewritten behavior for every consumer.
     */
    public static final class FusedValue implements Value {
        /** instructions that directly produced this value; copies preserve the producer chain; {@code null} = unknown/incomplete (a merge-reachable frame slot whose other predecessors the linear pass never saw) */
        public final Set<AbstractInsnNode> producers;
        /** the concrete type, or {@code java/lang/Object} for opaque/widened refs; null only for void producers */
        public final Type type;
        private final int size;

        private FusedValue(Set<AbstractInsnNode> producers, Type type, int size) {
            this.producers = producers;
            this.type = type;
            this.size = size;
        }

        private static FusedValue of(AbstractInsnNode producer, Type type) {
            Set<AbstractInsnNode> producers = new HashSet<>(1);
            if (producer != null) {
                producers.add(producer);
            }
            return new FusedValue(producers, type, type.getSize());
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FusedValue)) {
                return false;
            }
            FusedValue other = (FusedValue) o;
            return size == other.size && type.equals(other.type) && java.util.Objects.equals(producers, other.producers);
        }

        @Override
        public int hashCode() {
            return type.hashCode() * 31 + (producers == null ? 0 : producers.hashCode());
        }
    }

    /**
     * Single-pass analysis that trusts the declared {@link FrameNode}s
     * instead of running {@link Analyzer}'s merge-point fixpoint: state
     * propagates linearly between frame boundaries (via {@link Frame#execute}
     * for full stack discipline) and is restored from the declared frame
     * wherever a jump or handler edge can reach. O(n) — the insn scan, one
     * label/target walk, then the frame walk.
     * <p>
     * Producer soundness: at a merge-reachable frame the linear pass has
     * only seen one predecessor, so declared slots get {@code producers=null}
     * (unknown sources) — consumers requiring uniform producers stay native
     * there, which is conservative-correct. Types always come from the
     * declared frame (verifier-trusted) at merge points; between them the
     * propagated types are exact for the single path. Frames at
     * non-merge indices are informational and keep the propagated state.
     * <p>
     * Returns {@code null} when the method can't be trust-analyzed — no
     * declared frames (pre-50 input or frame-skipping parse) or JSR/RET —
     * in which case callers run the full {@link Analyzer} instead.
     */
    private static Frame<FusedValue>[] trustingAnalyze(String owner, MethodNode method) throws AnalyzerException {
        InsnList insns = method.instructions;
        int n = insns.size();
        // pass 1: label → index, frame presence, bail on jsr/ret
        Map<LabelNode, Integer> labelIdx = new IdentityHashMap<>();
        boolean hasFrames = false;
        for (int i = 0; i < n; i++) {
            AbstractInsnNode insn = insns.get(i);
            if (insn instanceof LabelNode) {
                labelIdx.put((LabelNode) insn, i);
            } else if (insn instanceof FrameNode) {
                hasFrames = true;
            } else if (insn.getOpcode() == Opcodes.JSR || insn.getOpcode() == Opcodes.RET) {
                return null;
            }
        }
        if (!hasFrames) {
            return null;
        }
        // jump + handler targets — frame positions reachable from an edge
        // the linear walk can't see
        Set<Integer> targets = new HashSet<>();
        for (int i = 0; i < n; i++) {
            AbstractInsnNode insn = insns.get(i);
            if (insn instanceof JumpInsnNode) {
                Integer t = labelIdx.get(((JumpInsnNode) insn).label);
                if (t != null) targets.add(t);
            } else if (insn instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode ts = (TableSwitchInsnNode) insn;
                Integer d = labelIdx.get(ts.dflt);
                if (d != null) targets.add(d);
                for (LabelNode l : ts.labels) {
                    Integer t = labelIdx.get(l);
                    if (t != null) targets.add(t);
                }
            } else if (insn instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode ls = (LookupSwitchInsnNode) insn;
                Integer d = labelIdx.get(ls.dflt);
                if (d != null) targets.add(d);
                for (LabelNode l : ls.labels) {
                    Integer t = labelIdx.get(l);
                    if (t != null) targets.add(t);
                }
            }
        }
        if (method.tryCatchBlocks != null) {
            for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
                Integer t = labelIdx.get(tcb.handler);
                if (t != null) targets.add(t);
            }
        }

        Frame<FusedValue>[] frames = new Frame[n];
        FusedInterpreter interp = new FusedInterpreter();
        Frame<FusedValue> cur = initialFrame(owner, method);
        // declared locals as of the last frame node — compressed delta
        // frames resolve against it; declaredItems counts verification
        // items (long/double = 1 item over 2 slots) for F_CHOP
        FusedValue[] declared = cur == null ? new FusedValue[method.maxLocals] : copyLocals(cur);
        int declaredItems = countItems(declared);
        boolean live = true;
        boolean pendingTarget = false;
        for (int i = 0; i < n; i++) {
            AbstractInsnNode insn = insns.get(i);
            if (insn instanceof FrameNode) {
                FrameNode f = (FrameNode) insn;
                declaredItems = applyDeclared(f, declared, declaredItems);
                if (pendingTarget || !live) {
                    // merge-reachable: trust the declared types and drop
                    // producer knowledge — the linear pass only saw one
                    // predecessor
                    cur = rebuild(method.maxLocals, method.maxStack, declared, f);
                }
                live = true;
                pendingTarget = false;
                continue;
            }
            if (insn instanceof LabelNode && targets.contains(i)) {
                pendingTarget = true;
                if (cur != null) {
                    cur = live ? stripProducers(cur) : opaqueFrame(cur);
                    // live: producers partial across the merge — drop them,
                    // keep propagated types. dead-start with no declared
                    // frame: nothing is known — go fully opaque.
                }
            }
            frames[i] = live ? cur : null;
            if (!live || insn.getOpcode() < 0) {
                continue;
            }
            Frame<FusedValue> next = new Frame<>(cur);
            next.execute(insn, interp);
            cur = next;
            int op = insn.getOpcode();
            if (op == Opcodes.GOTO || op == Opcodes.ATHROW
                || (op >= Opcodes.IRETURN && op <= Opcodes.RETURN)) {
                live = false;
            }
        }
        return frames;
    }

    /** entry state: {@code this} + declared parameter types, empty producers */
    private static Frame<FusedValue> initialFrame(String owner, MethodNode method) {
        Frame<FusedValue> f = new Frame<>(method.maxLocals, method.maxStack);
        int slot = 0;
        if ((method.access & Opcodes.ACC_STATIC) == 0) {
            f.setLocal(slot++, FusedValue.of(null, Type.getObjectType(owner)));
        }
        for (Type arg : Type.getArgumentTypes(method.desc)) {
            f.setLocal(slot, FusedValue.of(null, arg));
            slot += arg.getSize();
        }
        return f;
    }

    private static FusedValue[] copyLocals(Frame<FusedValue> frame) {
        FusedValue[] out = new FusedValue[frame.getLocals()];
        for (int i = 0; i < out.length; i++) {
            out[i] = frame.getLocal(i);
        }
        return out;
    }

    /**
     * Applies a FrameNode's declared locals to {@code declared}, resolving
     * compressed delta forms, and returns the new verification-item count.
     * Item and slot indices differ for long/double (one item, two slots).
     */
    private static int applyDeclared(FrameNode f, FusedValue[] declared, int items) {
        switch (f.type) {
            case Opcodes.F_FULL:
            case Opcodes.F_NEW:
                Arrays.fill(declared, null);
                expandDeclared(f.local, declared, 0);
                return f.local.size();
            case Opcodes.F_APPEND:
                expandDeclared(f.local, declared, slotAt(declared, items));
                return items + f.local.size();
            case Opcodes.F_CHOP:
                int chop = ((Integer) f.local.get(0));
                int keep = slotAt(declared, items - chop);
                for (int i = Math.max(keep, 0); i < declared.length; i++) {
                    declared[i] = null;
                }
                return items - chop;
            default:
                return items; // F_SAME/F_SAME1 — locals unchanged
        }
    }

    /** number of verification items in {@code declared} (null slots count as TOP items) */
    private static int countItems(FusedValue[] declared) {
        int items = 0;
        for (int i = 0; i < declared.length; ) {
            FusedValue v = declared[i];
            i += v == null ? 1 : v.getSize();
            items++;
        }
        return items;
    }

    /** slot index just past the {@code items}-th verification item */
    private static int slotAt(FusedValue[] declared, int items) {
        int slot = 0;
        while (items > 0 && slot < declared.length) {
            FusedValue v = declared[slot];
            slot += v == null ? 1 : v.getSize();
            items--;
        }
        return slot;
    }

    private static void expandDeclared(List<Object> local, FusedValue[] declared, int start) {
        int slot = start;
        for (Object o : local) {
            FusedValue v = declaredValue(o);
            if (slot < declared.length) {
                declared[slot] = v;
            }
            slot += v == null ? 1 : v.getSize();
        }
    }

    /** a declared-frame slot value — producers are always unknown (null) */
    private static FusedValue declaredValue(Object o) {
        if (o instanceof String) {
            return new FusedValue(null, Type.getObjectType((String) o), 1);
        }
        if (o instanceof Integer) {
            // frame tags are the boxed Opcodes singletons — ClassReader
            // stores the very objects, so identity compare is reliable
            if (o == Opcodes.INTEGER) return new FusedValue(null, Type.INT_TYPE, 1);
            if (o == Opcodes.FLOAT) return new FusedValue(null, Type.FLOAT_TYPE, 1);
            if (o == Opcodes.LONG) return new FusedValue(null, Type.LONG_TYPE, 2);
            if (o == Opcodes.DOUBLE) return new FusedValue(null, Type.DOUBLE_TYPE, 2);
            return new FusedValue(null, FusedInterpreter.OBJECT_T, 1); // NULL/UNINITIALIZED_*
        }
        // LabelNode — uninitialized-NEW marker; opaque
        return new FusedValue(null, FusedInterpreter.OBJECT_T, 1);
    }

    /** rebuilds state from the declared frame — types trusted, producers dropped */
    private static Frame<FusedValue> rebuild(int maxLocals, int maxStack, FusedValue[] declared, FrameNode f) {
        Frame<FusedValue> out = new Frame<>(maxLocals, maxStack);
        for (int i = 0; i < declared.length; i++) {
            out.setLocal(i, declared[i]);
        }
        List<Object> stack = f.stack;
        if (stack != null) {
            for (Object o : stack) {
                out.push(declaredValue(o));
            }
        }
        return out;
    }

    /** keeps slot shapes+types but drops producer knowledge — a live-path merge */
    private static Frame<FusedValue> stripProducers(Frame<FusedValue> prev) {
        Frame<FusedValue> out = new Frame<>(prev);
        for (int i = 0; i < out.getLocals(); i++) {
            FusedValue v = out.getLocal(i);
            if (v != null && v.producers != null) {
                out.setLocal(i, new FusedValue(null, v.type, v.getSize()));
            }
        }
        for (int i = 0; i < out.getStackSize(); i++) {
            FusedValue v = out.getStack(i);
            if (v != null && v.producers != null) {
                out.setStack(i, new FusedValue(null, v.type, v.getSize()));
            }
        }
        return out;
    }

    /** all-Object state for un-declared reachability — every consumer stays native */
    private static Frame<FusedValue> opaqueFrame(Frame<FusedValue> prev) {
        Frame<FusedValue> out = new Frame<>(prev);
        for (int i = 0; i < out.getLocals(); i++) {
            out.setLocal(i, new FusedValue(null, FusedInterpreter.OBJECT_T, 1));
        }
        for (int i = out.getStackSize() - 1; i >= 0; i--) {
            out.setStack(i, new FusedValue(null, FusedInterpreter.OBJECT_T, 1));
        }
        return out;
    }

    /**
     * The {@link Interpreter} behind {@link MethodAnalysis}: operations
     * record the producing insn and the operation's result type; copies
     * propagate both (so an {@code ldc}-stored-then-loaded receiver still
     * resolves to its {@code ldc} producer); merges degrade conservatively.
     */
    private static final class FusedInterpreter extends Interpreter<FusedValue> {
        static final Type OBJECT_T = Type.getObjectType("java/lang/Object");

        FusedInterpreter() {
            super(Opcodes.ASM9);
        }

        /** refs stay concrete, primitives keep their size, unknown/null → Object */
        private static Type refOrObject(Type type) {
            return type == null ? OBJECT_T : type;
        }

        @Override
        public FusedValue newValue(Type type) {
            if (type == Type.VOID_TYPE) {
                return null;
            }
            // a null type is a TOP slot — Analyzer pushes it for catch-all
            // handler frames, so it must be a real value (unknown producers),
            // not a null that consumers would dereference
            return type == null ? new FusedValue(null, OBJECT_T, 1)
                : FusedValue.of(null, refOrObject(type));
        }

        @Override
        public FusedValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
            return newValue(type);
        }

        @Override
        public FusedValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
            switch (insn.getOpcode()) {
                case Opcodes.ACONST_NULL:
                    return FusedValue.of(insn, OBJECT_T);
                case Opcodes.LDC: {
                    Object cst = ((LdcInsnNode) insn).cst;
                    return FusedValue.of(insn, Type.getObjectType(
                        cst instanceof Type ? "java/lang/Class"
                            : cst instanceof Handle ? "java/lang/invoke/MethodHandle"
                            : cst instanceof ConstantDynamic ? "java/lang/Object"
                            : cst.getClass().getName().replace('.', '/')));
                }
                case Opcodes.GETSTATIC:
                    return FusedValue.of(insn, refOrObject(Type.getType(((FieldInsnNode) insn).desc)));
                case Opcodes.NEW:
                    return FusedValue.of(insn, Type.getObjectType(((TypeInsnNode) insn).desc));
                case Opcodes.ICONST_M1:
                case Opcodes.ICONST_0:
                case Opcodes.ICONST_1:
                case Opcodes.ICONST_2:
                case Opcodes.ICONST_3:
                case Opcodes.ICONST_4:
                case Opcodes.ICONST_5:
                case Opcodes.BIPUSH:
                case Opcodes.SIPUSH:
                    return FusedValue.of(insn, Type.INT_TYPE);
                case Opcodes.LCONST_0:
                case Opcodes.LCONST_1:
                    return FusedValue.of(insn, Type.LONG_TYPE);
                case Opcodes.FCONST_0:
                case Opcodes.FCONST_1:
                case Opcodes.FCONST_2:
                    return FusedValue.of(insn, Type.FLOAT_TYPE);
                case Opcodes.DCONST_0:
                case Opcodes.DCONST_1:
                    return FusedValue.of(insn, Type.DOUBLE_TYPE);
                default:
                    // other producers (e.g. ATHROW path never reaches here)
                    return FusedValue.of(insn, OBJECT_T);
            }
        }

        @Override
        public FusedValue copyOperation(AbstractInsnNode insn, FusedValue value) {
            // loads/dups propagate the producing chain — an ldc'd value stays
            // visible as such through local slots
            return value;
        }

        @Override
        public FusedValue unaryOperation(AbstractInsnNode insn, FusedValue value) throws AnalyzerException {
            switch (insn.getOpcode()) {
                case Opcodes.CHECKCAST:
                    return FusedValue.of(insn, Type.getObjectType(((TypeInsnNode) insn).desc));
                case Opcodes.GETFIELD:
                    return FusedValue.of(insn, refOrObject(Type.getType(((FieldInsnNode) insn).desc)));
                case Opcodes.ARRAYLENGTH:
                    return FusedValue.of(insn, Type.INT_TYPE);
                case Opcodes.INSTANCEOF:
                    return FusedValue.of(insn, Type.INT_TYPE);
                case Opcodes.ANEWARRAY:
                    return FusedValue.of(insn, Type.getType("[L" + ((TypeInsnNode) insn).desc + ";"));
                case Opcodes.I2L:
                    return FusedValue.of(insn, Type.LONG_TYPE);
                case Opcodes.I2F:
                    return FusedValue.of(insn, Type.FLOAT_TYPE);
                case Opcodes.I2D:
                case Opcodes.L2D:
                case Opcodes.F2D:
                    return FusedValue.of(insn, Type.DOUBLE_TYPE);
                case Opcodes.L2I:
                case Opcodes.F2I:
                case Opcodes.D2I:
                case Opcodes.I2B:
                case Opcodes.I2C:
                case Opcodes.I2S:
                    return FusedValue.of(insn, Type.INT_TYPE);
                case Opcodes.L2F:
                case Opcodes.D2F:
                    return FusedValue.of(insn, Type.FLOAT_TYPE);
                case Opcodes.F2L:
                case Opcodes.D2L:
                    return FusedValue.of(insn, Type.LONG_TYPE);
                case Opcodes.INEG:
                    return FusedValue.of(insn, Type.INT_TYPE);
                case Opcodes.LNEG:
                    return FusedValue.of(insn, Type.LONG_TYPE);
                case Opcodes.FNEG:
                    return FusedValue.of(insn, Type.FLOAT_TYPE);
                case Opcodes.DNEG:
                    return FusedValue.of(insn, Type.DOUBLE_TYPE);
                default:
                    // numeric ops keep the operand's size; ref results are Object
                    return FusedValue.of(insn, value != null ? value.type : OBJECT_T);
            }
        }

        @Override
        public FusedValue binaryOperation(AbstractInsnNode insn, FusedValue value1, FusedValue value2) throws AnalyzerException {
            switch (insn.getOpcode()) {
                case Opcodes.AALOAD:
                    return FusedValue.of(insn,
                        value1 != null && value1.type.getSort() == Type.ARRAY
                            ? refOrObject(value1.type.getElementType()) : OBJECT_T);
                case Opcodes.BALOAD:
                    return FusedValue.of(insn, Type.INT_TYPE);
                case Opcodes.CALOAD:
                    return FusedValue.of(insn, Type.INT_TYPE);
                case Opcodes.SALOAD:
                    return FusedValue.of(insn, Type.INT_TYPE);
                case Opcodes.IALOAD:
                    return FusedValue.of(insn, Type.INT_TYPE);
                case Opcodes.LALOAD:
                    return FusedValue.of(insn, Type.LONG_TYPE);
                case Opcodes.FALOAD:
                    return FusedValue.of(insn, Type.FLOAT_TYPE);
                case Opcodes.DALOAD:
                    return FusedValue.of(insn, Type.DOUBLE_TYPE);
                case Opcodes.PUTFIELD:
                    return null;
                case Opcodes.LADD:
                case Opcodes.LSUB:
                case Opcodes.LMUL:
                case Opcodes.LDIV:
                case Opcodes.LREM:
                case Opcodes.LAND:
                case Opcodes.LOR:
                case Opcodes.LXOR:
                case Opcodes.LSHL:
                case Opcodes.LSHR:
                case Opcodes.LUSHR:
                    return FusedValue.of(insn, Type.LONG_TYPE);
                case Opcodes.FADD:
                case Opcodes.FSUB:
                case Opcodes.FMUL:
                case Opcodes.FDIV:
                case Opcodes.FREM:
                    return FusedValue.of(insn, Type.FLOAT_TYPE);
                case Opcodes.DADD:
                case Opcodes.DSUB:
                case Opcodes.DMUL:
                case Opcodes.DDIV:
                case Opcodes.DREM:
                    return FusedValue.of(insn, Type.DOUBLE_TYPE);
                default:
                    // I-family and remaining ops: int result; same-type operands keep theirs
                    return FusedValue.of(insn,
                        value1 != null && value1.type.equals(value2 != null ? value2.type : null)
                            ? value1.type : Type.INT_TYPE);
            }
        }

        @Override
        public FusedValue ternaryOperation(AbstractInsnNode insn, FusedValue value1, FusedValue value2, FusedValue value3) throws AnalyzerException {
            return null; // *ASTORE produce nothing
        }

        @Override
        public FusedValue naryOperation(AbstractInsnNode insn, List<? extends FusedValue> values) throws AnalyzerException {
            if (insn instanceof MethodInsnNode || insn instanceof InvokeDynamicInsnNode) {
                String desc = insn instanceof MethodInsnNode
                    ? ((MethodInsnNode) insn).desc : ((InvokeDynamicInsnNode) insn).desc;
                Type ret = Type.getReturnType(desc);
                return ret == Type.VOID_TYPE ? null : FusedValue.of(insn, ret);
            }
            if (insn.getOpcode() == Opcodes.MULTIANEWARRAY) {
                return FusedValue.of(insn, Type.getType(((MultiANewArrayInsnNode) insn).desc));
            }
            return FusedValue.of(insn, OBJECT_T);
        }

        @Override
        public void returnOperation(AbstractInsnNode insn, FusedValue value, FusedValue expected) {
        }

        @Override
        public FusedValue merge(FusedValue value1, FusedValue value2) {
            // Analyzer passes null for TOP/unset slots in declared frames
            if (value1 == null || value2 == null) {
                return value1 != null ? value1
                    : value2 != null ? value2 : FusedValue.of(null, OBJECT_T);
            }
            if (value1.equals(value2)) {
                return value1;
            }
            // incomplete dominates: an unknown-source side can produce anything
            Set<AbstractInsnNode> producers =
                value1.producers == null || value2.producers == null ? null : union(value1.producers, value2.producers);
            Type type = value1.type.equals(value2.type) ? value1.type : OBJECT_T;
            return new FusedValue(producers, type, Math.max(value1.size, value2.size));
        }

        private static Set<AbstractInsnNode> union(Set<AbstractInsnNode> a, Set<AbstractInsnNode> b) {
            Set<AbstractInsnNode> out = new HashSet<>(a);
            out.addAll(b);
            return out;
        }
    }

    /** {@code INVOKEVIRTUAL java/lang/Class.isInstance|cast|isAssignableFrom} — the query shapes {@link #remapClassCalls} routes. */
    private static boolean isClassQuery(AbstractInsnNode insn) {
        if (!(insn instanceof MethodInsnNode) || insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
            return false;
        }
        MethodInsnNode min = (MethodInsnNode) insn;
        if (!min.owner.equals("java/lang/Class")) {
            return false;
        }
        return (min.name.equals("isInstance") && min.desc.equals("(Ljava/lang/Object;)Z")) ||
            (min.name.equals("cast") && min.desc.equals("(Ljava/lang/Object;)Ljava/lang/Object;")) ||
            (min.name.equals("isAssignableFrom") && min.desc.equals("(Ljava/lang/Class;)Z"));
    }

    /**
     * The single routable {@link Type} every producer {@code ldc}s, or
     * null — producers must be uniformly {@code ldc <type>.class} of the
     * same routable type: a retro (stubbed) receiver or a gained-supertype
     * interface (an array constant counts only when its element is
     * routable; scalar-vs-array merges mismatch and are skipped).
     */
    private Type uniformRoutableType(Set<AbstractInsnNode> producers) {
        if (producers == null || producers.isEmpty()) {
            return null;
        }
        String name = null;
        for (AbstractInsnNode producer : producers) {
            if (!(producer instanceof LdcInsnNode) || !(((LdcInsnNode) producer).cst instanceof Type)) {
                return null;
            }
            Type t = (Type) ((LdcInsnNode) producer).cst;
            Type element = t.getSort() == Type.ARRAY ? t.getElementType() : t;
            boolean routable = element.getSort() == Type.OBJECT &&
                (remap.stubFor(element) != null || remap.gainedFor(element) != null);
            if (!routable || (name != null && !name.equals(t.getInternalName()))) {
                return null;
            }
            name = t.getInternalName();
        }
        return Type.getObjectType(name);
    }

    /**
     * {@code [.., X.class, arg] → [.., arg(, dims)] → X$jvmdg$retro.*} —
     * stack effect identical to the original call so the next hand-authored
     * frame stays valid. Array receivers route to the element-membership
     * forms and carry the receiver's dimension count; every retro query
     * lands on the generated per-adapter dispatcher (guaranteed by
     * {@link #uniformRetroType}, which only accepts stubbed receivers).
     */
    private InsnList classQueryPostfix(MethodInsnNode call, Type receiver, int ceiling) {
        boolean array = receiver.getSort() == Type.ARRAY;
        Type element = array ? receiver.getElementType() : receiver;
        ClassStubEntry stub = remap.stubFor(element);
        if (stub == null) {
            return gainedQueryPostfix(call, receiver, element, ceiling);
        }
        InsnList out = new InsnList();
        out.add(new InsnNode(Opcodes.SWAP));
        out.add(new InsnNode(Opcodes.POP));
        switch (call.name) {
            case "isInstance":
                out.add(dispCall(stub.stubType, array ? "isArrayInstance" : "isInstance", true));
                break;
            case "cast":
                // mirror checkcast's dispatch: OWN_INTERFACE receivers must
                // adapt members to the adapter type — cast() validates but
                // returns the raw object — so the same hook/wrap-factory/
                // generated-wrap order applies
                if (!array && stub.mode == Adapter.Mode.OWN_INTERFACE) {
                    if (stub.checkcastHook != null) {
                        out.add(hookInvoke(stub.checkcastHook, stub.itf));
                    } else if (stub.wrap != null) {
                        out.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            stub.wrap.getOwner().getInternalName(), stub.wrap.getName(),
                            stub.wrap.getDesc().getDescriptor(),
                            stub.wrap.getOwner().equals(stub.stubType)
                                ? stub.itf : isInterface(stub.wrap.getOwner())));
                    } else {
                        out.add(dispCall(stub.stubType, "wrap", false));
                    }
                } else {
                    out.add(dispCall(stub.stubType, array ? "castArray" : "cast", false));
                }
                break;
            default: // isAssignableFrom
                out.add(intInsn(array ? receiver.getDimensions() : 0));
                out.add(dispCall(stub.stubType, "isAssignableFrom", "(Ljava/lang/Class;I)Z"));
        }
        return out;
    }

    /**
     * {@code X.class.*} on a gained-supertype receiver — the same
     * swap/pop receiver rearrangement, routed to the interface's
     * dispatcher with the authored ceiling. {@code cast} on an array
     * receiver stays native (member arrays can't be fabricated); its
     * {@code null} return leaves the call alone.
     */
    private InsnList gainedQueryPostfix(MethodInsnNode call, Type receiver, Type element, int ceiling) {
        String disp = remap.gainedFor(element);
        if (disp == null) {
            return null;
        }
        boolean array = receiver.getSort() == Type.ARRAY;
        InsnList out = new InsnList();
        out.add(new InsnNode(Opcodes.SWAP));
        out.add(new InsnNode(Opcodes.POP));
        switch (call.name) {
            case "isInstance":
                out.add(intInsn(ceiling));
                out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                    array ? "jvmdg$isArrayInstance" : "jvmdg$isInstance",
                    "(Ljava/lang/Object;I)Z", false));
                break;
            case "cast":
                if (array) {
                    return null;
                }
                out.add(intInsn(ceiling));
                out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp, "jvmdg$cast",
                    "(Ljava/lang/Object;I)" + receiver.getDescriptor(), false));
                break;
            default: // isAssignableFrom
                if (array) {
                    out.add(intInsn(receiver.getDimensions()));
                    out.add(intInsn(ceiling));
                    out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                        "jvmdg$isArrayAssignableFrom", "(Ljava/lang/Class;II)Z", false));
                } else {
                    out.add(intInsn(ceiling));
                    out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, disp,
                        "jvmdg$isAssignableFrom", "(Ljava/lang/Class;I)Z", false));
                }
        }
        return out;
    }

    private static AbstractInsnNode intInsn(int v) {
        if (v >= 0 && v <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + v);
        }
        return new IntInsnNode(v <= Byte.MAX_VALUE ? Opcodes.BIPUSH : Opcodes.SIPUSH, v);
    }

    private static InsnList single(AbstractInsnNode insn) {
        InsnList out = new InsnList();
        out.add(insn);
        return out;
    }

    private static MethodInsnNode hookInvoke(FullyQualifiedMemberNameAndDesc member, boolean itf) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, member.getOwner().getInternalName(),
            member.getName(), member.getDesc().getDescriptor(), itf);
    }

    /**
     * Off-adapter {@code wrap} owner fallback — the adapter's own bit comes
     * from {@link ClassStubEntry#itf}; a foreign owner's invokestatic bit
     * resolves reflectively when it can, treating unloadable classes
     * (including pre-downgrade class-file versions a runtime loader would
     * hand us) as non-interface.
     */
    private static boolean isInterface(Type owner) {
        try {
            return Class.forName(owner.getClassName(), false, StructuralRemapTransform.class.getClassLoader()).isInterface();
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /**
     * {@code invokestatic <adapter>$jvmdg$retro.<name>} — the per-adapter
     * dispatcher {@code genStubIndex} generates into the api jar for every
     * class stub (OWN_INTERFACE adapters get {@code wrap}; SUPERTYPE get
     * {@code cast}; both get the membership/array/query forms). The owner
     * is an ordinary api-class reference: the shade closure indexes the
     * edge and prefix-remapping rewrites it like any other api call.
     */
    private static MethodInsnNode dispCall(Type adapter, String name, boolean bool) {
        return dispCall(adapter, name,
            "(Ljava/lang/Object;)" + (bool ? "Z" : "Ljava/lang/Object;"));
    }

    private static MethodInsnNode dispCall(Type adapter, String name, String desc) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC,
            adapter.getInternalName() + "$jvmdg$retro", name, desc, false);
    }
}
