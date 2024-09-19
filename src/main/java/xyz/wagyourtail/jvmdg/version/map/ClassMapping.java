package xyz.wagyourtail.jvmdg.version.map;

import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Coverage;
import xyz.wagyourtail.jvmdg.version.Modify;
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
    private final Map<MemberNameAndDesc, Pair<Method, Modify>> methodModify = new HashMap<>();

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

    public void addStub(MemberNameAndDesc member, Method method, Stub stub) {
        int modifiers = method.getModifiers();
        if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            throw new RuntimeException("@Stub " + method + " must be public static");
        }
        methodStub.put(member, new Pair<>(method, stub));
    }

    public void addModify(MemberNameAndDesc member, Method method, Modify modify) {
        int modifiers = method.getModifiers();
        if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            throw new RuntimeException("@Modify " + method + " must be public static");
        }
        methodModify.put(member, new Pair<>(method, modify));
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
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) insn;
            MemberNameAndDesc member = new MemberNameAndDesc(min.name, Type.getMethodType(min.desc));
            boolean isStatic = insn.getOpcode() == Opcodes.INVOKESTATIC;
            boolean isSpecial = insn.getOpcode() == Opcodes.INVOKESPECIAL;

            Pair<Method, Stub> newMin = getStubFor(member, isStatic, runtimeAvailable, isSpecial, warnings);
            Type returnType = Type.getReturnType(min.desc);
            if (newMin != null) {
                // handled specially, by inserting a call to the stub in the implementation if it's missing an implementation.
                // unless it's invokespecial, then this is a super call, and need to fix it to call the stub.
                if (newMin.getSecond().abstractDefault() && !isSpecial) {
                    return;
                }
                // TODO: REALLY shouldn't be doing this... should use @Modify now, so super ctor calls work.
                if (member.getName().equals("<init>")) {
                    returnType = Type.getObjectType(min.owner);
                    int skip = 0;
                    int j;
                    for (j = index - 1; j >= 0; j--) {
                        AbstractInsnNode prev = method.instructions.get(j);
                        if (prev.getOpcode() == Opcodes.NEW && prev instanceof TypeInsnNode) {
                            if (((TypeInsnNode) prev).desc.equals(min.owner) && skip-- == 0) {
                                method.instructions.remove(prev);
                                // check and remove dup
                                if (method.instructions.get(j).getOpcode() == Opcodes.DUP) {
                                    method.instructions.remove(method.instructions.get(j));
                                } else {
                                    throw new IllegalStateException("expected DUP after NEW");
                                }
                                break;
                            }
                        } else if (prev.getOpcode() == Opcodes.INVOKESPECIAL && prev instanceof MethodInsnNode) {
                            if (((MethodInsnNode) prev).owner.equals(min.owner) && ((MethodInsnNode) prev).name.equals("<init>")) {
                                skip++;
                            }
                        }
                    }
                    if (j < 0) {
                        throw new IllegalStateException("Could not find NEW for <init> call...");
                    }
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
                method.instructions.insertBefore(min, insnList);
                if (!returnType.equals(Type.getReturnType(m))) {
                    // add cast
                    method.instructions.insertBefore(min, new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
                }
                method.instructions.remove(min);
                return;
            }
            Pair<Method, Modify> m = getModifyFor(member, isStatic, warnings);
            if (m != null) {
                try {
                    List<Object> modifyArgs = Arrays.asList(method, index, classNode, extra);
                    m.getFirst().invoke(null, modifyArgs.subList(0, m.getFirst().getParameterTypes().length).toArray());
                    return;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (insn instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
            MemberNameAndDesc member = new MemberNameAndDesc(indy.bsm.getName(), Type.getMethodType(indy.bsm.getDesc()));

            Pair<Method, Modify> m = methodModify.get(member);
            if (m != null) {
                try {
                    List<Object> modifyArgs = Arrays.asList(method, index, classNode, extra);
                    m.getFirst().invoke(null, modifyArgs.subList(0, m.getFirst().getParameterTypes().length).toArray());
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }
    }

    public Pair<Method, Stub> getParentStubFor(MemberNameAndDesc member, boolean runtimeAvailable, boolean special, Set<String> warnings) {
        for (ClassMapping parent : parents.get()) {
            Pair<Method, Stub> node = parent.getStubFor(member, false, runtimeAvailable, special, warnings);
            if (node != null) {

                // check if explicitly excluded from stubbing
                // ie. method exists on the subtype in question
                for (String child : node.getSecond().excludeChild()) {
                    if (child.startsWith("L") && child.endsWith(";")) {
                        child = child.substring(1, child.length() - 1);
                    }
                    if (child.equals(current.getInternalName())) {
                        return null;
                    }
                }

                return node;
            }
        }
        return null;
    }

    public Pair<Method, Stub> getStubFor(MemberNameAndDesc member, boolean invoke_static, boolean runtimeAvailable, boolean special, Set<String> warnings) {
        try {
            Pair<Method, Stub> pair = methodStub.get(member);
            if (pair == null) {
                if (!invoke_static && !member.getName().equals("<init>")) {
                    Map<MemberNameAndDesc, Pair<Boolean, Type>> members = this.members.get();
                    if (members != null && members.containsKey(member)) {
//                        if (parentStub != null) {
//                            System.err.println("WARNING: " + current.getDescriptor() + member + " is not missing but parent has stub...");
//                        }
                        return null;
                    }
                    return getParentStubFor(member, runtimeAvailable, special, warnings);
                }
                warnMember(member, warnings, invoke_static);
                return null;
            }
            Method m = pair.getFirst();
            if (!runtimeAvailable && pair.getSecond().requiresRuntime()) {
                warnings.add(m + " requires runtime transformation but runtime is not available...");
            }
            if (special && pair.getSecond().noSpecial()) {
                return null;
            }
            int modifiers = m.getModifiers();
            if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                throw new RuntimeException("stub method must be public static");
            }
            return pair;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Pair<Method, Modify> getParentModifyFor(MemberNameAndDesc member, Set<String> warnings) {
        for (ClassMapping parent : parents.get()) {
            Pair<Method, Modify> node = parent.getModifyFor(member, false, warnings);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    public Pair<Method, Modify> getModifyFor(MemberNameAndDesc member, boolean invoke_static, Set<String> warnings) {
        try {
            Pair<Method, Modify> pair = methodModify.get(member);
            if (pair == null) {
                if (!invoke_static && !member.getName().equals("<init>")) {
                    Map<MemberNameAndDesc, Pair<Boolean, Type>> members = this.members.get();
                    if (members != null && members.containsKey(member)) {
                        return null;
                    }
                    return getParentModifyFor(member, warnings);
                }
                warnMember(member, warnings, invoke_static);
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

    public Map<MemberNameAndDesc, Pair<Method, Modify>> getMethodModifyMap() {
        return methodModify;
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
