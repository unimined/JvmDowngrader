package xyz.wagyourtail.jvmdg.version.map;

import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.ModifyUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Per-instruction emission half of member stubbing: given a resolved stub or
 * modify entry (from the {@link ClassMapping} resolution graph), rewrites the
 * call site — {@code INVOKESTATIC} replacement with optional input-version
 * ldc and checkcast, {@code NEW/DUP} surgery for stubbed constructors,
 * delegation-call fixups, and reflective {@code @Modify} invocation.
 * <p>
 * Contains no resolution state of its own — the mapping is passed per call —
 * which is what lets it become a pipeline stage in Phase D.
 */
public class MemberStubTransform {

    public void transformInsn(MemberResolver mr, MethodNode method, int index, ClassNode classNode, Set<ClassNode> extra, boolean runtimeAvailable, Set<String> warnings) {
        AbstractInsnNode insn = method.instructions.get(index);
        if (insn instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
            MemberNameAndDesc member = new MemberNameAndDesc(indy.bsm.getName(), Type.getMethodType(indy.bsm.getDesc()));

            Method m = mr.rawModify(member);
            if (m != null) {
                invokeModify(m, method, index, classNode, extra, mr, member, warnings);
            }
        } else {
            MemberNameAndDesc member;
            String owner;
            Type returnType;
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) insn;

                owner = min.owner;
                member = mr.unstubMember(min.name, Type.getMethodType(min.desc));
                returnType = Type.getReturnType(min.desc);
            } else if (insn instanceof FieldInsnNode) {
                FieldInsnNode fin = (FieldInsnNode) insn;

                owner = fin.owner;
                member = mr.unstubMember(fin.name, Type.getType(fin.desc));
                returnType = Type.getType(fin.desc);
            } else {
                return;
            }

            if (member.getName().equals("<init>")) {
                // ctor callsites resolve owner-exact from the ctor map —
                // factories replace the NEW/DUP construction, rewrites
                // retarget the <init> and run pre/arg/post hooks
                CtorEntry ce = mr.getCtorFor(member, runtimeAvailable, warnings);
                if (ce instanceof CtorRewrite) {
                    rewriteCtor(method, insn, index, classNode, owner, member,
                        (CtorRewrite) ce, mr, warnings);
                    return;
                }
                if (ce instanceof CtorFactory) {
                    CtorFactory factory = (CtorFactory) ce;
                    returnType = Type.getObjectType(owner);
                    int newIndex = findPairedNew(method, index, owner);
                    if (newIndex < 0) {
                        // provable pairing failed: this()/super() delegation or a shape we can't
                        // verify. leave the original invokespecial untouched rather than risk
                        // corrupting the stack by removing the wrong NEW.
                        warnings.add("stubbed <init> on " + owner + " in " + classNode.name + "." +
                            method.name + method.desc + " could not be verified against a NEW/DUP " +
                            "pair, leaving original call (may fail at runtime if the constructor " +
                            "does not exist on the target)");
                        return;
                    }
                    // remove NEW and its immediately-following DUP (verified to be a pair)
                    method.instructions.remove(method.instructions.get(newIndex));
                    method.instructions.remove(method.instructions.get(newIndex));
                    InsnList insnList = new InsnList();
                    if (factory.downgradeVersion) {
                        insnList.add(new LdcInsnNode(mr.provider().inputVersion));
                    }
                    Method fm = factory.method;
                    insnList.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getType(fm.getDeclaringClass()).getInternalName(),
                        fm.getName(),
                        Type.getMethodDescriptor(fm),
                        fm.getDeclaringClass().isInterface()
                    ));
                    method.instructions.insertBefore(insn, insnList);
                    Type fret = Type.getReturnType(fm);
                    if (!returnType.equals(fret)) {
                        method.instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
                    }
                    method.instructions.remove(insn);
                    return;
                }
            } else {
                StubEntry newMin = mr.getStubFor(member, runtimeAvailable, insn.getOpcode(), warnings);
            if (newMin != null) {
                // handled specially, by inserting a call to the stub in the implementation if it's missing an implementation.
                // unless it's invokespecial, then this is a super call, and need to fix it to call the stub.
                if (newMin.abstractDefault && insn.getOpcode() != Opcodes.INVOKESPECIAL) {
                    return;
                }
                InsnList insnList = new InsnList();
                if (newMin.downgradeVersion) {
                    insnList.add(new LdcInsnNode(mr.provider().inputVersion));
                }
                Method m = newMin.method;
                insnList.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Type.getType(m.getDeclaringClass()).getInternalName(),
                    m.getName(),
                    Type.getMethodDescriptor(m),
                    m.getDeclaringClass().isInterface()
                ));
                if (!returnType.equals(Type.getReturnType(m))) {
                    if (Type.getReturnType(m) == Type.VOID_TYPE) {
                        // a void-returning stub cannot fabricate the original
                        // call's result — emitting the call would leave the
                        // stack short (and a CHECKCAST would pop nothing)
                        warnings.add("stub for " + owner + "." + member.getName() + member.getDesc() +
                            " returns void but the original member returns " + returnType +
                            " in " + classNode.name + "." + method.name + method.desc +
                            ", leaving original call (may fail at runtime if the member " +
                            "does not exist on the target)");
                        return;
                    }
                }
                method.instructions.insertBefore(insn, insnList);
                if (!returnType.equals(Type.getReturnType(m))) {
                    // add cast
                    method.instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
                }
                method.instructions.remove(insn);
                return;
            }
            }
            Method m = mr.getModifyFor(member, insn.getOpcode(), warnings);
            if (m != null) {
                // <init> @Modify handlers rewrite the construction site and assume
                // the javac NEW+DUP shape (two uninitialised refs on the stack,
                // the second just below the args). this()/super() delegation
                // calls have only one (the uninitialised `this` pushed as the
                // first instruction of the constructor): synthesise the missing
                // copy with a second ALOAD 0 right after it so the handler's
                // stack surgery still balances. For any other unpaired shape we
                // cannot prove the fix-up is safe, so leave the insn.
                int invokeIndex = index;
                if (member.getName().equals("<init>") && findPairedNew(method, index, owner) < 0) {
                    if (method.name.equals("<init>") && isDelegationCall(method, index, classNode, owner)) {
                        int receiver = firstRealInsnIndex(method);
                        method.instructions.insert(method.instructions.get(receiver),
                            new VarInsnNode(Opcodes.ALOAD, 0));
                        invokeIndex = index + 1;
                    } else {
                        warnings.add("stubbed <init> modify on " + owner + " in " + classNode.name + "." +
                            method.name + method.desc + " could not be verified against a NEW/DUP " +
                            "pair, leaving original call (may fail at runtime if the constructor " +
                            "does not exist on the target)");
                        return;
                    }
                }
                invokeModify(m, method, invokeIndex, classNode, extra, mr, member, warnings);
            }
        }
    }

    /** reflectively invokes a {@code @Modify} handler with its declared parameter subset; failures warn, never sink */
    private static void invokeModify(Method modify, MethodNode method, int index, ClassNode classNode,
                                     Set<ClassNode> extra, MemberResolver mr, MemberNameAndDesc member, Set<String> warnings) {
        try {
            Object[] modifyArgs = new Object[] { method, index, classNode, extra, mr.provider().downgrader.flags.debugNoSynthetic };
            int[] sigIdx = ModifyUtils.matchModifyParams(modify);
            List<Object> takeArgs = new ArrayList<>();
            for (int idx : sigIdx) {
                takeArgs.add(modifyArgs[idx]);
            }
            modify.invoke(null, takeArgs.toArray());
        } catch (Throwable e) {
            // a failed @Modify handler must not sink the whole class —
            // warn and leave the insn unmodified
            warnings.add("modify " + modify.getDeclaringClass().getSimpleName()
                + "." + modify.getName() + " failed on " + member.getName()
                + member.getDesc() + ": " + e);
        }
    }

    /**
     * Generalized ctor rewrite ({@code @InitRewrite}):
     * {@code invokespecial owner.<init>(origArgs)} becomes
     * {@code pre? → invokespecial owner.<init>(toArgs) → post?}, where each
     * {@code to} slot is a kept original arg or an {@code @InitArg}
     * conversion. The {@code NEW}/{@code DUP} pair — or the {@code ALOAD 0}
     * receiver on a {@code this()}/{@code super()} delegation — is left
     * untouched; all surgery happens at the callsite by popping the original
     * args into fresh locals in reverse order and reloading what the new
     * signature needs, so arg evaluation order (and cat-2 slot widths) is
     * preserved without mid-region rewriting.
     * <p>
     * After the retargeted call the object is initialized: on a {@code new}
     * site the surviving DUP'ed ref is {@code DUP}'ed for the post tail; on
     * delegation {@code this} is reloaded via {@code ALOAD 0} (legal once the
     * delegated {@code <init>} has run). A callsite shape the
     * pairing/delegation verifiers can't prove warns and leaves the original
     * {@code invokespecial} untouched.
     */
    public static void rewriteCtor(MethodNode method, AbstractInsnNode insn, int index,
                                    ClassNode classNode, String owner, MemberNameAndDesc member,
                                    CtorRewrite rw, MemberResolver mr, Set<String> warnings) {
        String where = owner + "." + member.getName() + member.getDesc() +
            " in " + classNode.name + "." + method.name + method.desc;
        Type[] origArgs = Type.getArgumentTypes(((MethodInsnNode) insn).desc);

        // prove the callsite shape: NEW+DUP construction, or this()/super() delegation
        boolean delegation = findPairedNew(method, index, owner) < 0;
        if (delegation && !(method.name.equals("<init>") && isDelegationCall(method, index, classNode, owner))) {
            warnings.add("stubbed <init> rewrite on " + where +
                " could not be verified against a NEW/DUP pair or delegation call, " +
                "leaving original call (may fail at runtime if the constructor " +
                "does not exist on the target)");
            return;
        }

        // fresh locals for all args, allocated past maxLocals (or past params
        // if the writer left maxLocals unset)
        int local = method.maxLocals;
        if (local == 0) {
            local = (method.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
            for (Type t : Type.getArgumentTypes(method.desc)) {
                local += t.getSize();
            }
        }
        int[] tmp = new int[origArgs.length];
        for (int i = 0; i < origArgs.length; i++) {
            tmp[i] = local;
            local += origArgs[i].getSize();
        }
        method.maxLocals = Math.max(method.maxLocals, local);

        InsnList ops = new InsnList();
        // pop args into locals, reverse order — each store consumes exactly
        // its own arg's slot width, so cat-2 types need no special-casing
        for (int i = origArgs.length - 1; i >= 0; i--) {
            ops.add(new VarInsnNode(origArgs[i].getOpcode(Opcodes.ISTORE), tmp[i]));
        }
        if (rw.pre != null) {
            for (int f : rw.preFrom) {
                ops.add(new VarInsnNode(origArgs[f].getOpcode(Opcodes.ILOAD), tmp[f]));
            }
            ops.add(invokestatic(rw.pre));
        }
        for (int i = 0; i < rw.toArgs.length; i++) {
            if (rw.keep[i] >= 0) {
                ops.add(new VarInsnNode(origArgs[rw.keep[i]].getOpcode(Opcodes.ILOAD), tmp[rw.keep[i]]));
            } else {
                for (int f : rw.argFrom[i]) {
                    ops.add(new VarInsnNode(origArgs[f].getOpcode(Opcodes.ILOAD), tmp[f]));
                }
                ops.add(invokestatic(rw.argMethods[i]));
            }
        }
        String toDesc = Type.getMethodDescriptor(Type.VOID_TYPE, rw.toArgs);
        ops.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, owner, "<init>", toDesc,
            ((MethodInsnNode) insn).itf));
        if (rw.post != null) {
            // receiver for the tail: the now-initialized object
            ops.add(delegation ? new VarInsnNode(Opcodes.ALOAD, 0) : new InsnNode(Opcodes.DUP));
            for (int f : rw.postFrom) {
                ops.add(new VarInsnNode(origArgs[f].getOpcode(Opcodes.ILOAD), tmp[f]));
            }
            if (rw.downgradeVersion) {
                ops.add(new LdcInsnNode(mr.provider().inputVersion));
            }
            ops.add(invokestatic(rw.post));
        }
        method.instructions.insertBefore(insn, ops);
        method.instructions.remove(insn);
    }

    private static MethodInsnNode invokestatic(Method m) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC,
            Type.getType(m.getDeclaringClass()).getInternalName(), m.getName(),
            Type.getMethodDescriptor(m), m.getDeclaringClass().isInterface());
    }

    // --- <init> NEW/DUP pairing verification ---------------------------------
    //
    // Rewriting a stubbed constructor call requires removing the matching
    // NEW+DUP pair so the static stub factory can take over allocation. The old
    // code scanned backwards and guessed ("skip" counting on same-owner <init>
    // calls), which both crashed on this()/super() delegation and could remove
    // an unrelated NEW in exotic instruction shapes. The helpers below instead
    // prove the pairing by bounded abstract interpretation of the stack region
    // between the candidate NEW and the invokespecial; only a proven pair is
    // rewritten, anything else leaves the original bytecode untouched.

    /** Marker for the uninitialized ref pushed by the candidate NEW (and its DUP). */
    private static final Object UNINIT = new Object();
    /** Marker for any other stack entry. */
    private static final Object STACK_ENTRY = new Object();

    /**
     * Finds the NEW instruction that provably pairs with the {@code <init>}
     * invokespecial at {@code initIndex}, or -1 when no pairing can be proven.
     * Candidates are tried nearest-first; nested same-owner constructions are
     * disambiguated because an inner {@code <init>} call consumes its own NEW's
     * ref, failing verification for the outer call.
     */
    @VisibleForTesting
    static int findPairedNew(MethodNode method, int initIndex, String owner) {
        for (int j = initIndex - 1; j >= 0; j--) {
            AbstractInsnNode insn = method.instructions.get(j);
            if (insn.getOpcode() == Opcodes.NEW && insn instanceof TypeInsnNode &&
                owner.equals(((TypeInsnNode) insn).desc) &&
                verifyNewInitPairing(method, j, initIndex)) {
                return j;
            }
        }
        return -1;
    }

    /**
     * Proves that the uninitialized ref produced by the NEW at {@code newIndex}
     * (which must be immediately DUP'ed — that is the pair the rewrite removes)
     * is the receiver consumed by the invokespecial at {@code initIndex}, and
     * that it is not consumed by anything else in between. All control-flow
     * paths inside the region must keep matching stacks at merge points (as the
     * bytecode verifier requires) and must not leave the region before the call.
     */
    @VisibleForTesting
    static boolean verifyNewInitPairing(MethodNode method, int newIndex, int initIndex) {
        AbstractInsnNode afterNew = method.instructions.get(newIndex + 1);
        if (newIndex + 1 >= initIndex || afterNew.getOpcode() != Opcodes.DUP) {
            return false;
        }
        List<Object> atCall = simulateStackRegion(method, newIndex + 1, initIndex);
        if (atCall == null) {
            return false;
        }
        int argCount = Type.getArgumentTypes(((MethodInsnNode) method.instructions.get(initIndex)).desc).length;
        if (atCall.size() < argCount + 1) {
            return false;
        }
        for (int i = atCall.size() - argCount; i < atCall.size(); i++) {
            if (atCall.get(i) == UNINIT) {
                return false; // uninit ref used as a call argument — cannot prove
            }
        }
        return atCall.get(atCall.size() - argCount - 1) == UNINIT;
    }

    /**
     * Proves that the {@code <init>} invokespecial at {@code initIndex} is a
     * this()/super() constructor delegation whose {@code this} was pushed by
     * {@code firstIndex} (the method's first real instruction, an ALOAD 0),
     * i.e. at call entry the operand stack is exactly {@code [this, args...]}
     * with the tracked ref at the bottom. When that holds, a second ALOAD 0
     * inserted just after {@code firstIndex} reproduces the stack shape the
     * javac NEW+DUP pair would have given a {@code new} call site.
     */
    @VisibleForTesting
    static boolean verifyThisDelegation(MethodNode method, int firstIndex, int initIndex) {
        List<Object> atCall = simulateStackRegion(method, firstIndex + 1, initIndex);
        if (atCall == null) {
            return false;
        }
        int argCount = Type.getArgumentTypes(((MethodInsnNode) method.instructions.get(initIndex)).desc).length;
        return atCall.size() == argCount + 1 && atCall.get(0) == UNINIT;
    }

    /**
     * True when the invokespecial at {@code index} is provably a this()/super()
     * constructor-delegation call: it is inside a constructor, targets this
     * class or its direct superclass, and the method's first real instruction
     * pushes {@code this}.
     */
    private static boolean isDelegationCall(MethodNode method, int index, ClassNode classNode, String owner) {
        if (!owner.equals(classNode.name) && !owner.equals(classNode.superName)) {
            return false;
        }
        int first = firstRealInsnIndex(method);
        return first >= 0 && first < index
            && method.instructions.get(first).getOpcode() == Opcodes.ALOAD
            && ((VarInsnNode) method.instructions.get(first)).var == 0
            && verifyThisDelegation(method, first, index);
    }

    /** Index of the first non-metadata instruction of {@code method}. */
    private static int firstRealInsnIndex(MethodNode method) {
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode insn = method.instructions.get(i);
            if (!(insn instanceof LabelNode || insn instanceof LineNumberNode || insn instanceof FrameNode)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Abstract interpretation of the instruction region between
     * {@code startIndex} and {@code initIndex} (exclusive of both), seeded
     * with a one-element stack {@code [UNINIT]}. Returns the abstract stack
     * on entry to instruction {@code initIndex}, or null when control flow
     * exits or re-enters the region, merges divergent stacks, or meets an
     * instruction shape {@link #transferInsn} cannot model.
     */
    private static List<Object> simulateStackRegion(MethodNode method, int startIndex, int initIndex) {
        Map<LabelNode, Integer> labels = new HashMap<LabelNode, Integer>();
        for (int i = startIndex; i <= initIndex; i++) {
            AbstractInsnNode insn = method.instructions.get(i);
            if (insn instanceof LabelNode) {
                labels.put((LabelNode) insn, i);
            }
        }
        // stacks[i] = abstract operand stack on entry to instruction i.
        @SuppressWarnings("unchecked")
        List<Object>[] stacks = new List[initIndex + 1];
        List<Object> start = new ArrayList<Object>();
        start.add(UNINIT);
        stacks[startIndex] = start;
        Deque<Integer> work = new ArrayDeque<Integer>();
        work.add(startIndex);
        while (!work.isEmpty()) {
            int i = work.poll();
            if (i >= initIndex) {
                continue; // the <init> call itself is terminal for the region
            }
            List<Object> in = stacks[i];
            if (in == null) {
                continue;
            }
            List<Pair<Integer, List<Object>>> out = transferInsn(method.instructions.get(i), i, in, labels);
            if (out == null) {
                return null; // instruction shape we cannot model
            }
            for (Pair<Integer, List<Object>> succ : out) {
                int t = succ.getFirst();
                if (t <= startIndex || t > initIndex) {
                    // control flow exits the region (or loops back) while our
                    // tracked ref is still live — not provably consumed by the call
                    return null;
                }
                List<Object> prev = stacks[t];
                if (prev == null) {
                    stacks[t] = succ.getSecond();
                    work.add(t);
                } else if (!prev.equals(succ.getSecond())) {
                    return null; // divergent stacks at a merge point
                }
            }
        }
        return stacks[initIndex];
    }

    /**
     * One step of the stack simulation. Returns the (index, stack) successors
     * of {@code insn}, or null when the instruction's effect on our tracked ref
     * cannot be proven safe.
     */
    private static List<Pair<Integer, List<Object>>> transferInsn(AbstractInsnNode insn, int index,
                                                                  List<Object> in, Map<LabelNode, Integer> labels) {
        List<Pair<Integer, List<Object>>> out = new ArrayList<Pair<Integer, List<Object>>>(2);
        if (insn instanceof LabelNode || insn instanceof LineNumberNode || insn instanceof FrameNode) {
            out.add(new Pair<Integer, List<Object>>(index + 1, in));
            return out;
        }
        int op = insn.getOpcode();
        if (insn instanceof JumpInsnNode) {
            Integer target = labels.get(((JumpInsnNode) insn).label);
            if (target == null) {
                return null;
            }
            if (op == Opcodes.GOTO) {
                out.add(new Pair<Integer, List<Object>>(target, in));
                return out;
            }
            int pops = (op == Opcodes.IF_ICMPEQ || op == Opcodes.IF_ICMPNE || op == Opcodes.IF_ICMPLT ||
                op == Opcodes.IF_ICMPGE || op == Opcodes.IF_ICMPGT || op == Opcodes.IF_ICMPLE ||
                op == Opcodes.IF_ACMPEQ || op == Opcodes.IF_ACMPNE) ? 2 : 1;
            List<Object> next = popChecked(in, pops);
            if (next == null) {
                return null;
            }
            out.add(new Pair<Integer, List<Object>>(index + 1, next));
            out.add(new Pair<Integer, List<Object>>(target, next));
            return out;
        }
        if (insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode) {
            List<Object> next = popChecked(in, 1);
            if (next == null) {
                return null;
            }
            List<LabelNode> targets = insn instanceof TableSwitchInsnNode ?
                ((TableSwitchInsnNode) insn).labels : ((LookupSwitchInsnNode) insn).labels;
            LabelNode dflt = insn instanceof TableSwitchInsnNode ?
                ((TableSwitchInsnNode) insn).dflt : ((LookupSwitchInsnNode) insn).dflt;
            for (LabelNode l : targets) {
                Integer t = labels.get(l);
                if (t == null) {
                    return null;
                }
                out.add(new Pair<Integer, List<Object>>(t, next));
            }
            Integer t = labels.get(dflt);
            if (t == null) {
                return null;
            }
            out.add(new Pair<Integer, List<Object>>(t, next));
            return out;
        }
        if (insn instanceof VarInsnNode) {
            if (op >= Opcodes.ILOAD && op <= Opcodes.ALOAD) {
                out.add(new Pair<Integer, List<Object>>(index + 1, push(in, STACK_ENTRY)));
                return out;
            }
            List<Object> next = popChecked(in, 1);
            return next == null ? null : single(index + 1, next);
        }
        if (insn instanceof IincInsnNode) {
            return single(index + 1, in);
        }
        if (insn instanceof LdcInsnNode) {
            return single(index + 1, push(in, STACK_ENTRY));
        }
        if (insn instanceof IntInsnNode) {
            if (op == Opcodes.NEWARRAY) {
                List<Object> next = popChecked(in, 1);
                return next == null ? null : single(index + 1, push(next, STACK_ENTRY));
            }
            return single(index + 1, push(in, STACK_ENTRY)); // BIPUSH / SIPUSH
        }
        if (insn instanceof TypeInsnNode) {
            if (op == Opcodes.NEW) {
                return single(index + 1, push(in, STACK_ENTRY));
            }
            // CHECKCAST / ANEWARRAY / INSTANCEOF: pop 1, push 1
            List<Object> next = popChecked(in, 1);
            return next == null ? null : single(index + 1, push(next, STACK_ENTRY));
        }
        if (insn instanceof FieldInsnNode) {
            int pops;
            switch (op) {
                case Opcodes.GETSTATIC:
                    return single(index + 1, push(in, STACK_ENTRY));
                case Opcodes.GETFIELD:
                    pops = 1;
                    break;
                case Opcodes.PUTSTATIC:
                    pops = 1;
                    break;
                case Opcodes.PUTFIELD:
                    pops = 2;
                    break;
                default:
                    return null;
            }
            List<Object> next = popChecked(in, pops);
            if (next == null) {
                return null;
            }
            return single(index + 1, op == Opcodes.GETFIELD ? push(next, STACK_ENTRY) : next);
        }
        if (insn instanceof MethodInsnNode) {
            int pops = Type.getArgumentTypes(((MethodInsnNode) insn).desc).length +
                (op == Opcodes.INVOKESTATIC ? 0 : 1);
            List<Object> next = popChecked(in, pops);
            if (next == null) {
                return null;
            }
            return single(index + 1,
                ((MethodInsnNode) insn).desc.endsWith("V") ? next : push(next, STACK_ENTRY));
        }
        if (insn instanceof InvokeDynamicInsnNode) {
            List<Object> next = popChecked(in, Type.getArgumentTypes(((InvokeDynamicInsnNode) insn).desc).length);
            if (next == null) {
                return null;
            }
            return single(index + 1,
                ((InvokeDynamicInsnNode) insn).desc.endsWith("V") ? next : push(next, STACK_ENTRY));
        }
        if (insn instanceof MultiANewArrayInsnNode) {
            List<Object> next = popChecked(in, ((MultiANewArrayInsnNode) insn).dims);
            return next == null ? null : single(index + 1, push(next, STACK_ENTRY));
        }
        if (insn instanceof InsnNode) {
            switch (op) {
                case Opcodes.NOP:
                    return single(index + 1, in);
                case Opcodes.ACONST_NULL:
                case Opcodes.ICONST_M1:
                case Opcodes.ICONST_0:
                case Opcodes.ICONST_1:
                case Opcodes.ICONST_2:
                case Opcodes.ICONST_3:
                case Opcodes.ICONST_4:
                case Opcodes.ICONST_5:
                case Opcodes.LCONST_0:
                case Opcodes.LCONST_1:
                case Opcodes.FCONST_0:
                case Opcodes.FCONST_1:
                case Opcodes.FCONST_2:
                case Opcodes.DCONST_0:
                case Opcodes.DCONST_1:
                    return single(index + 1, push(in, STACK_ENTRY));
                case Opcodes.POP: {
                    List<Object> next = popChecked(in, 1);
                    return next == null ? null : single(index + 1, next);
                }
                case Opcodes.POP2: {
                    List<Object> next = popChecked(in, 2);
                    return next == null ? null : single(index + 1, next);
                }
                case Opcodes.DUP: {
                    if (in.isEmpty()) {
                        return null;
                    }
                    return single(index + 1, push(in, in.get(in.size() - 1)));
                }
                case Opcodes.DUP2: {
                    if (in.size() < 2) {
                        return null;
                    }
                    List<Object> next = push(push(in, in.get(in.size() - 2)), in.get(in.size() - 1));
                    return single(index + 1, next);
                }
                case Opcodes.SWAP: {
                    if (in.size() < 2 || in.get(in.size() - 1) == UNINIT || in.get(in.size() - 2) == UNINIT) {
                        return null; // moving the uninit ref is unprovable
                    }
                    List<Object> next = new ArrayList<Object>(in.subList(0, in.size() - 2));
                    next.add(in.get(in.size() - 1));
                    next.add(in.get(in.size() - 2));
                    return single(index + 1, next);
                }
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                case Opcodes.FRETURN:
                case Opcodes.DRETURN:
                case Opcodes.ARETURN:
                case Opcodes.RETURN:
                case Opcodes.ATHROW:
                    return out; // path exits the method; no successors
                default:
                    break;
            }
            // remaining InsnNode ops have uniform popN/pushM shapes
            int pops;
            int pushes;
            if ((op >= Opcodes.IALOAD && op <= Opcodes.SALOAD) || op == Opcodes.ARRAYLENGTH ||
                (op >= Opcodes.I2L && op <= Opcodes.I2S) || op == Opcodes.LCMP ||
                op == Opcodes.FCMPL || op == Opcodes.FCMPG || op == Opcodes.DCMPL || op == Opcodes.DCMPG) {
                // array load / arraylength / primitive conversion: pop up to 2, push 1
                // (LCMP & co pop 2 push 1 like binops)
                pops = (op >= Opcodes.I2L && op <= Opcodes.I2S) || op == Opcodes.ARRAYLENGTH ? 1 : 2;
                pushes = 1;
            } else if (op >= Opcodes.IASTORE && op <= Opcodes.SASTORE) {
                pops = 3;
                pushes = 0;
            } else if (op >= Opcodes.IADD && op <= Opcodes.LXOR) {
                pops = 2;
                pushes = 1; // binops, shifts, comparisons
            } else if (op >= Opcodes.INEG && op <= Opcodes.DNEG) {
                pops = 1;
                pushes = 1;
            } else {
                return null; // MONITORENTER/EXIT, dup_x*/dup2_x*, anything else
            }
            List<Object> next = popChecked(in, pops);
            if (next == null) {
                return null;
            }
            if (pushes == 1) {
                next = push(next, STACK_ENTRY);
            }
            return single(index + 1, next);
        }
        return null;
    }

    private static List<Pair<Integer, List<Object>>> single(int index, List<Object> stack) {
        List<Pair<Integer, List<Object>>> out = new ArrayList<Pair<Integer, List<Object>>>(1);
        out.add(new Pair<Integer, List<Object>>(index, stack));
        return out;
    }

    /** Returns a copy of {@code stack} with {@code value} pushed. */
    private static List<Object> push(List<Object> stack, Object value) {
        List<Object> next = new ArrayList<Object>(stack.size() + 1);
        next.addAll(stack);
        next.add(value);
        return next;
    }

    /**
     * Returns a copy of {@code stack} with {@code count} entries popped, or
     * null if the stack underflows or any popped entry is the tracked
     * uninitialized ref (an instruction consuming it other than the verified
     * call makes the pairing unprovable).
     */
    private static List<Object> popChecked(List<Object> stack, int count) {
        if (stack.size() < count) {
            return null;
        }
        List<Object> next = new ArrayList<Object>(stack.size() - count);
        next.addAll(stack.subList(0, stack.size() - count));
        for (int i = stack.size() - count; i < stack.size(); i++) {
            if (stack.get(i) == UNINIT) {
                return null;
            }
        }
        return next;
    }
}
