package xyz.wagyourtail.jvmdg.version.map;

import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Coverage;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.ModifyUtils;
import xyz.wagyourtail.jvmdg.version.Stub;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ClassMapping {

    public final Type current;
    protected final Lazy<Map<MemberNameAndDesc, Pair<Boolean, Type>>> members;
    protected final Lazy<List<ClassMapping>> parents;
    protected final VersionProvider vp;
    private final Map<MemberNameAndDesc, Pair<Method, Stub>> methodStub = new HashMap<>();
    private final Map<MemberNameAndDesc, Pair<Pair<Method, Stub>, Pair<Method, Stub>>> fieldStub = new HashMap<>();
    private final Map<MemberNameAndDesc, Pair<Method, Modify>> memberModify = new HashMap<>();

    private final Coverage coverage;

    public ClassMapping(final Coverage coverage, final Lazy<List<ClassMapping>> parents, final Type current, final boolean isInterface, final IOFunction<Type, Set<MemberNameAndDesc>> members, VersionProvider vp) {
        this.parents = parents;
        this.current = current;
        this.coverage = coverage;
        this.members = new Lazy<Map<MemberNameAndDesc, Pair<Boolean, Type>>>() {

            @Override
            protected Map<MemberNameAndDesc, Pair<Boolean, Type>> init() {
                try {
                    Map<MemberNameAndDesc, Pair<Boolean, Type>> mems = new HashMap<>();
                    Set<MemberNameAndDesc> m = members.apply(current);
                    if (m != null) {
//                        mems.addAll(m);
                        for (MemberNameAndDesc member : m) {
                            mems.put(member, new Pair<>(isInterface, current));
                        }
                    }
                    // fix multi-inheritance
                    for (ClassMapping parent : parents.get()) {
                        for (Map.Entry<MemberNameAndDesc, Pair<Boolean, Type>> member : parent.members.get().entrySet()) {
                            if (!mems.containsKey(member.getKey()) || !member.getValue().getFirst()) {
                                mems.put(member.getKey(), member.getValue());
                            }
                        }
                    }
                    return mems;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        };
        this.vp = vp;
    }

    public boolean isAssignableFrom(Type target) {
        if (target.equals(current)) {
            return true;
        }
        for (ClassMapping parent : parents.get()) {
            if (parent.isAssignableFrom(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rebuilds a member with adapter (fake) types in its desc translated back
     * to their real (JDK) types. Api-internal call sites carry fake types
     * after api downgrading, while registered members and pre-transform call
     * sites carry real ones — matching requires the real form.
     */
    private MemberNameAndDesc unstubMember(String name, Type desc) {
        if (desc.getSort() == Type.METHOD) {
            Type[] args = desc.getArgumentTypes();
            Type ret = desc.getReturnType();
            boolean diff = false;
            for (int i = 0; i < args.length; i++) {
                Type u = vp.unstubClass(args[i]);
                if (u != args[i]) {
                    args[i] = u;
                    diff = true;
                }
            }
            Type uret = vp.unstubClass(ret);
            if (diff || uret != ret) {
                desc = Type.getMethodType(uret, args);
            }
        } else {
            desc = vp.unstubClass(desc);
        }
        return new MemberNameAndDesc(name, desc);
    }

    public void addStub(MemberNameAndDesc member, Method method, Stub stub) {
        int modifiers = method.getModifiers();
        if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            throw new RuntimeException("@Stub " + method + " must be public static");
        }
        if (member.isMethod()) {
            methodStub.put(member, new Pair<>(method, stub));
        } else {
            Pair<Pair<Method, Stub>, Pair<Method, Stub>> field = fieldStub.get(member);
            if (field == null) {
                field = new Pair<>(null, null);
            }
            if (method.getReturnType().equals(Void.TYPE)) {
                field = new Pair<>(field.getFirst(), new Pair<>(method, stub));
            } else {
                field = new Pair<>(new Pair<>(method, stub), field.getSecond());
            }
            fieldStub.put(member, field);
        }
    }

    public void addModify(MemberNameAndDesc member, Method method, Modify modify) {
        int modifiers = method.getModifiers();
        if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            throw new RuntimeException("@Modify " + method + " must be public static");
        }
        memberModify.put(member, new Pair<>(method, modify));
    }

    public void warnMember(MemberNameAndDesc member, Set<String> warnings, boolean invoke_static) {
        FullyQualifiedMemberNameAndDesc fqn = member.toFullyQualified(current);
        String mod = coverage.checkMember(fqn);
        if (mod != null) {
            coverage.warnMember(fqn, warnings);
        }
        if (!invoke_static && !member.getName().equals("<init>")) {
            for (ClassMapping parent : parents.get()) {
                parent.warnMember(member, warnings, false);
            }
        }
    }

    public void transform(MethodNode method, int index, ClassNode classNode, Set<ClassNode> extra, boolean runtimeAvailable, Set<String> warnings) {
        AbstractInsnNode insn = method.instructions.get(index);
        if (insn instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
            MemberNameAndDesc member = new MemberNameAndDesc(indy.bsm.getName(), Type.getMethodType(indy.bsm.getDesc()));

            Pair<Method, Modify> m = memberModify.get(member);
            if (m != null) {
                try {
                    Object[] modifyArgs = new Object[] { method, index, classNode, extra, vp.downgrader.flags.debugNoSynthetic };
                    int[] sigIdx = ModifyUtils.matchModifyParams(m.getFirst());
                    List<Object> takeArgs = new ArrayList<>();
                    for (int idx : sigIdx) {
                        takeArgs.add(modifyArgs[idx]);
                    }
                    m.getFirst().invoke(null, takeArgs.toArray());
                } catch (Throwable e) {
                    // a failed @Modify handler must not sink the whole class —
                    // warn and leave the insn unmodified
                    warnings.add("modify " + m.getFirst().getDeclaringClass().getSimpleName()
                        + "." + m.getFirst().getName() + " failed on " + member.getName()
                        + member.getDesc() + ": " + e);
                }
                return;
            }
        } else {
            MemberNameAndDesc member;
            String owner;
            Type returnType;
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) insn;

                owner = min.owner;
                member = unstubMember(min.name, Type.getMethodType(min.desc));
                returnType = Type.getReturnType(min.desc);
            } else if (insn instanceof FieldInsnNode) {
                FieldInsnNode fin = (FieldInsnNode) insn;

                owner = fin.owner;
                member = unstubMember(fin.name, Type.getType(fin.desc));
                returnType = Type.getType(fin.desc);
            } else {
                return;
            }

            Pair<Method, Stub> newMin = getStubFor(member, runtimeAvailable, insn.getOpcode(), warnings);
            if (newMin != null) {
                // handled specially, by inserting a call to the stub in the implementation if it's missing an implementation.
                // unless it's invokespecial, then this is a super call, and need to fix it to call the stub.
                if (newMin.getSecond().abstractDefault() && insn.getOpcode() != Opcodes.INVOKESPECIAL) {
                    return;
                }
                // TODO: REALLY shouldn't be doing this... should use @Modify now, so super ctor calls work.
                if (member.getName().equals("<init>")) {
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
                }
                InsnList insnList = new InsnList();
                if (newMin.getSecond().downgradeVersion()) {
                    insnList.add(new LdcInsnNode(vp.inputVersion));
                }
                Method m = newMin.getFirst();
                insnList.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Type.getType(m.getDeclaringClass()).getInternalName(),
                    m.getName(),
                    Type.getMethodDescriptor(m),
                    newMin.getFirst().getDeclaringClass().isInterface()
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
            Pair<Method, Modify> m = getModifyFor(member, insn.getOpcode(), warnings);
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
                try {
                    Object[] modifyArgs = new Object[] { method, invokeIndex, classNode, extra, vp.downgrader.flags.debugNoSynthetic };
                    int[] sigIdx = ModifyUtils.matchModifyParams(m.getFirst());
                    List<Object> takeArgs = new ArrayList<>();
                    for (int idx : sigIdx) {
                        takeArgs.add(modifyArgs[idx]);
                    }
                    m.getFirst().invoke(null, takeArgs.toArray());
                } catch (Throwable e) {
                    // a failed @Modify handler must not sink the whole class —
                    // warn and leave the insn unmodified
                    warnings.add("modify " + m.getFirst().getDeclaringClass().getSimpleName()
                        + "." + m.getFirst().getName() + " failed on " + member.getName()
                        + member.getDesc() + ": " + e);
                }
            }
        }
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

    public Pair<Method, Stub> getParentStubFor(MemberNameAndDesc member, boolean runtimeAvailable, int opcode, Set<String> warnings) {
        for (ClassMapping parent : parents.get()) {
            Pair<Method, Stub> node = parent.getStubFor(member, runtimeAvailable, opcode, warnings);
            if (node != null) {

                // check if explicitly excluded from stubbing
                // ie. method exists on the subtype in question
                for (String child : node.getSecond().excludeChild()) {
                    if (child.startsWith("L") && child.endsWith(";")) {
                        child = child.substring(1, child.length() - 1);
                    }
                    if (isAssignableFrom(Type.getObjectType(child))) {
                        return null;
                    }
                }

                return node;
            }
        }
        return null;
    }

    public Pair<Method, Stub> getStubFor(MemberNameAndDesc member, boolean runtimeAvailable, int opcode, Set<String> warnings) {
        opcode = ASMUtils.toHandleTag(opcode);
        try {
            Pair<Method, Stub> pair;
            if (member.isMethod()) {
                pair = methodStub.get(member);
                if (pair == null) {
                    if (opcode != Opcodes.H_INVOKESTATIC && !member.getName().equals("<init>")) {
                        Map<MemberNameAndDesc, Pair<Boolean, Type>> members = this.members.get();
                        if (members != null && members.containsKey(member)) {
                            return null;
                        }
                        return getParentStubFor(member, runtimeAvailable, opcode, warnings);
                    }
                    warnMember(member, warnings, opcode == Opcodes.H_INVOKESTATIC);
                    return null;
                }
            } else {
                Pair<Pair<Method, Stub>, Pair<Method, Stub>> stub = fieldStub.get(member);
                switch (opcode) {
                    case Opcodes.H_GETFIELD:
                        if (stub == null) {
                            Map<MemberNameAndDesc, Pair<Boolean, Type>> members = this.members.get();
                            if (members != null && members.containsKey(member)) {
                                return null;
                            }
                            return getParentStubFor(member, runtimeAvailable, opcode, warnings);
                        }
                    case Opcodes.H_GETSTATIC:
                        if (stub == null) {
                            return null;
                        }
                        pair = stub.getFirst();
                        break;
                    case Opcodes.H_PUTFIELD:
                        if (stub == null) {
                            Map<MemberNameAndDesc, Pair<Boolean, Type>> members = this.members.get();
                            if (members != null && members.containsKey(member)) {
                                return null;
                            }
                            return getParentStubFor(member, runtimeAvailable, opcode, warnings);
                        }
                    case Opcodes.H_PUTSTATIC:
                        if (stub == null) {
                            return null;
                        }
                        pair = stub.getSecond();
                        break;
                    default:
                        throw new IllegalStateException("Unexpected Handle: " + opcode);
                }
                if (pair == null) {
                    return null;
                }
            }
            Method m = pair.getFirst();
            if (!runtimeAvailable && pair.getSecond().requiresRuntime()) {
                warnings.add(m + " requires runtime transformation but runtime is not available...");
                return null;
            }
            if (opcode == Opcodes.H_INVOKESPECIAL && pair.getSecond().noSpecial()) {
                return null;
            }
            return pair;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Pair<Method, Modify> getParentModifyFor(MemberNameAndDesc member, int opcode, Set<String> warnings) {
        for (ClassMapping parent : parents.get()) {
            Pair<Method, Modify> node = parent.getModifyFor(member, opcode, warnings);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    public Pair<Method, Modify> getModifyFor(MemberNameAndDesc member, int opcode, Set<String> warnings) {
        opcode = ASMUtils.toHandleTag(opcode);
        boolean isStatic = false;
        switch (opcode) {
            case Opcodes.H_GETSTATIC:
            case Opcodes.H_PUTSTATIC:
            case Opcodes.H_INVOKESTATIC:
                isStatic = true;
        }
        try {
            Pair<Method, Modify> pair = memberModify.get(member);
            if (pair == null) {
                pair = memberModify.get(null);
            }
            if (pair == null) {
                if (!isStatic && (member == null || !member.getName().equals("<init>"))) {
                    Map<MemberNameAndDesc, Pair<Boolean, Type>> members = this.members.get();
                    if (members != null && members.containsKey(member)) {
                        return null;
                    }
                    return getParentModifyFor(member, opcode, warnings);
                }
                warnMember(member, warnings, opcode == Opcodes.H_INVOKESTATIC);
                return null;
            }
            Method m = pair.getFirst();
            int modifiers = m.getModifiers();
            if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                throw new RuntimeException("modify method must be public static");
            }
            return pair;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<MemberNameAndDesc, Pair<Method, Stub>> getMethodStubMap() {
        return methodStub;
    }

    public Map<MemberNameAndDesc, Pair<Pair<Method, Stub>, Pair<Method, Stub>>> getFieldStubMap() {
        return fieldStub;
    }

    public Map<MemberNameAndDesc, Pair<Method, Modify>> getMethodModifyMap() {
        return memberModify;
    }

    @VisibleForTesting
    public List<ClassMapping> getParents() {
        return parents.get();
    }

    public List<Pair<Method, Stub>> getStubTargets() {
        List<Pair<Method, Stub>> methods = new ArrayList<>();
        for (ClassMapping parent : parents.get()) {
            methods.addAll(parent.getStubTargets());
        }
        methods.addAll(methodStub.values());
        return methods;
    }

    public Map<MemberNameAndDesc, Pair<Method, Stub>> getAbstracts() {
        Map<MemberNameAndDesc, Pair<Method, Stub>> methods = new HashMap<>();
        for (ClassMapping parent : parents.get()) {
            methods.putAll(parent.getAbstracts());
        }
        for (Map.Entry<MemberNameAndDesc, Pair<Method, Stub>> entry : methodStub.entrySet()) {
            if (entry.getValue().getSecond().abstractDefault()) {
                methods.put(entry.getKey(), entry.getValue());
            }
        }
        // remove if can resolve a non-abstract already
        for (MemberNameAndDesc memberNameAndDesc : this.members.get().keySet()) {
            methods.remove(memberNameAndDesc);
        }
        return methods;
    }

    public Map<MemberNameAndDesc, Pair<Boolean, Type>> getMembers() {
        return members.get();
    }

}
