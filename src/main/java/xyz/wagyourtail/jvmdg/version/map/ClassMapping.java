package xyz.wagyourtail.jvmdg.version.map;

import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Stub;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ClassMapping {

    private final Map<MemberNameAndDesc, Pair<Method, Stub>> methodStub = new HashMap<>();
    private final Map<MemberNameAndDesc, Pair<Method, Modify>> methodModify = new HashMap<>();
    protected final Lazy<Set<MemberNameAndDesc>> members;

    protected final Lazy<List<ClassMapping>> parents;
    protected final Type current;
    protected final VersionProvider vp;

    public ClassMapping(final Lazy<List<ClassMapping>> parents, final Type current, final IOFunction<Type, Set<MemberNameAndDesc>> members, VersionProvider vp) {
        this.parents = parents;
        this.current = current;
        this.members = new Lazy<Set<MemberNameAndDesc>>() {

            @Override
            protected Set<MemberNameAndDesc> init() {
                try {
                    Set<MemberNameAndDesc> mems = new HashSet<>();
                    Set<MemberNameAndDesc> m = members.apply(current);
                    if (m != null) {
                        mems.addAll(m);
                    }
                    // fix multi-inheritance
                    for (ClassMapping parent : parents.get()) {
                        mems.addAll(parent.members.get());
                    }
                    return mems;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };

        };
        this.vp = vp;
    }

    public void addStub(MemberNameAndDesc member, Method method, Stub stub) {
        methodStub.put(member, new Pair<>(method, stub));
    }

    public void addModify(MemberNameAndDesc member, Method method, Modify modify) {
        methodModify.put(member, new Pair<>(method, modify));
    }

    public void transform(MethodNode method, int index, ClassNode classNode, Set<ClassNode> extra, boolean runtimeAvailable) {
        AbstractInsnNode insn = method.instructions.get(index);
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) insn;
            MemberNameAndDesc member = new MemberNameAndDesc(min.name, Type.getMethodType(min.desc));
            Pair<Method, Stub> newMin = getStubFor(member, min.getOpcode() == Opcodes.INVOKESTATIC, runtimeAvailable);
            Type returnType = Type.getReturnType(min.desc);
            if (newMin != null) {
                // handled specially, by inserting a call to the stub in the implementation if it's missing an implementation.
                // unless it's invokespecial, then this is a super call, and need to fix it to call the stub.
                if (newMin.getSecond().abstractDefault() && min.getOpcode() != Opcodes.INVOKESPECIAL) {
                    return;
                }
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
                    false
                ));
                method.instructions.insertBefore(min, insnList);
                if (!returnType.equals(Type.getReturnType(m))) {
                    // add cast
                    method.instructions.insertBefore(min, new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
                }
                method.instructions.remove(min);
                return;
            }
            Pair<Method, Modify> m = methodModify.get(member);
            if (m != null) {
                try {
                    List<Object> modifyArgs = Arrays.asList(method, index, classNode, extra);
                    m.getFirst().invoke(null, modifyArgs.subList(0, m.getFirst().getParameterTypes().length).toArray());
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
            }
        }
    }

    public Pair<Method, Stub> getParentStubFor(MemberNameAndDesc member, boolean runtimeAvailable) {
        for (ClassMapping parent : parents.get()) {
            Pair<Method, Stub> node = parent.getStubFor(member, false, runtimeAvailable);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    public Pair<Method, Stub> getStubFor(MemberNameAndDesc member, boolean invoke_static, boolean runtimeAvailable) {
        try {
            Pair<Method, Stub> pair = methodStub.get(member);
            if (pair == null) {
                if (!invoke_static) {
                    Set<MemberNameAndDesc> members = this.members.get();
                    if (members != null && members.contains(member)) {
//                        if (parentStub != null) {
//                            System.err.println("WARNING: " + current.getDescriptor() + member + " is not missing but parent has stub...");
//                        }
                        return null;
                    }
                    return getParentStubFor(member, runtimeAvailable);
                }
                return null;
            }
            Method m = pair.getFirst();
            if (!runtimeAvailable && pair.getSecond().requiresRuntime()) {
                System.err.println("WARNING: " + m + " requires runtime transformation but runtime is not available...");
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
        for (MemberNameAndDesc memberNameAndDesc : this.members.get()) {
            methods.remove(memberNameAndDesc);
        }
        return methods;
    }


}
