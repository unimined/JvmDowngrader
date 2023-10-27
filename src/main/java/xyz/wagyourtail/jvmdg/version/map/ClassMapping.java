package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Stub;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ClassMapping {

    private final Map<MemberNameAndDesc, Pair<Method, Stub>> methodStub = new HashMap<>();
    private final Map<MemberNameAndDesc, Pair<Method, Modify>> methodModify = new HashMap<>();

    protected final Lazy<List<ClassMapping>> parents;
    protected final Type current;
    protected final VersionProvider vp;

    public ClassMapping(Lazy<List<ClassMapping>> parents, Type current, VersionProvider vp) {
        this.parents = parents;
        this.current = current;
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
            InsnList newMin = getStubFor(member, min.getOpcode() == Opcodes.INVOKESTATIC, runtimeAvailable);
            Type returnType = Type.getReturnType(min.desc);
            if (newMin != null) {
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
                MethodInsnNode target = (MethodInsnNode) newMin.getLast();
                method.instructions.insertBefore(min, newMin);
                method.instructions.remove(min);
                if (!returnType.equals(Type.getReturnType(target.desc))) {
                    // add cast
                    method.instructions.insert(target, new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
                }
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
//            MethodInsnNode newMin = getStubFor(member);
//            if (newMin != null) {
//                method.instructions.set(indy, newMin);
//                return;
//            }
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

    public InsnList getStubFor(MemberNameAndDesc member, boolean invoke_static, boolean runtimeAvailable) {
        try {
            if (!invoke_static) {
                for (ClassMapping parent : parents.get()) {
                    InsnList node = parent.getStubFor(member, false, runtimeAvailable);
                    if (node != null) {
                        return node;
                    }
                }
            }
            Pair<Method, Stub> pair = methodStub.get(member);
            if (pair == null) {
                return null;
            }
            Method m = pair.getFirst();
            if (!runtimeAvailable && pair.getSecond().requiresRuntime()) {
                System.err.println("WARNING: " + m + " requires runtime transformation but runtime is not available...");
            }
            if (pair.getSecond().abstractDefault()) {
                // these are special and should be treated differently, as we will implement them on the newer classes in a seperate transform
                return null;
            }
            int modifiers = m.getModifiers();
            if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                throw new RuntimeException("stub method must be public static");
            }
            InsnList insnList = new InsnList();
            if (pair.getSecond().downgradeVersion()) {
                insnList.add(new LdcInsnNode(vp.inputVersion));
            }
            insnList.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Type.getType(m.getDeclaringClass()).getInternalName(),
                    m.getName(),
                    Type.getMethodDescriptor(m),
                    false
            ));
            return insnList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<MemberNameAndDesc, Pair<Method, Stub>> getMethodStub() {
        return methodStub;
    }

    public Map<MemberNameAndDesc, Pair<Method, Modify>> getMethodModify() {
        return methodModify;
    }

    public List<Method> getStubTargets() {
        List<Method> methods = new ArrayList<>();
        for (ClassMapping parent : parents.get()) {
            methods.addAll(parent.getStubTargets());
        }
        for (Pair<Method, Stub> pair : methodStub.values()) {
            methods.add(pair.getFirst());
        }
        return methods;
    }

    public Map<MemberNameAndDesc, Method> getAbstracts() {
        Map<MemberNameAndDesc, Method> methods = new HashMap<>();
        for (ClassMapping parent : parents.get()) {
            methods.putAll(parent.getAbstracts());
        }
        for (Map.Entry<MemberNameAndDesc, Pair<Method, Stub>> entry : methodStub.entrySet()) {
            if (entry.getValue().getSecond().abstractDefault()) {
                methods.put(entry.getKey(), entry.getValue().getFirst());
            }
        }
        return methods;
    }


}
