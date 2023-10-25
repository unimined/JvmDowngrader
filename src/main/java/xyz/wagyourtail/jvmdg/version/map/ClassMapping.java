package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.util.Lazy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ClassMapping {

    private final Map<MemberNameAndDesc, Method> methodStub = new HashMap<>();
    private final Map<MemberNameAndDesc, Method> methodModify = new HashMap<>();

    protected final Lazy<List<ClassMapping>> parents;
    protected final Type current;

    public ClassMapping(Lazy<List<ClassMapping>> parents, Type current) {
        this.parents = parents;
        this.current = current;
    }

    public void addStub(MemberNameAndDesc member, Method method) {
        methodStub.put(member, method);
    }

    public void addModify(MemberNameAndDesc member, Method method) {
        methodModify.put(member, method);
    }

    public void transform(MethodNode method, int index, ClassNode classNode, Set<ClassNode> extra) {
        AbstractInsnNode insn = method.instructions.get(index);
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) insn;
            MemberNameAndDesc member = new MemberNameAndDesc(min.name, Type.getMethodType(min.desc));
            MethodInsnNode newMin = getStubFor(member, min.getOpcode() == Opcodes.INVOKESTATIC);
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
                method.instructions.set(min, newMin);
                if (!returnType.equals(Type.getReturnType(newMin.desc))) {
                    // add cast
                    method.instructions.insert(newMin, new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
                }
                return;
            }
            Method m = methodModify.get(member);
            if (m != null) {
                try {
                    List<Object> modifyArgs = Arrays.asList(method, index, classNode, extra);
                    m.invoke(null, modifyArgs.subList(0, m.getParameterTypes().length).toArray());
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
            Method m = methodModify.get(member);
            if (m != null) {
                try {
                    List<Object> modifyArgs = Arrays.asList(method, index, classNode, extra);
                    m.invoke(null, modifyArgs.subList(0, m.getParameterTypes().length).toArray());
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public MethodInsnNode getStubFor(MemberNameAndDesc member, boolean invoke_static) {
        try {
            if (!invoke_static) {
                for (ClassMapping parent : parents.get()) {
                    MethodInsnNode node = parent.getStubFor(member, false);
                    if (node != null) {
                        return node;
                    }
                }
            }
            Method m = methodStub.get(member);
            if (m == null) {
                return null;
            }
            int modifiers = m.getModifiers();
            if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                throw new RuntimeException("stub method must be public static");
            }
            return new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Type.getType(m.getDeclaringClass()).getInternalName(),
                    m.getName(),
                    Type.getMethodDescriptor(m),
                    false
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
