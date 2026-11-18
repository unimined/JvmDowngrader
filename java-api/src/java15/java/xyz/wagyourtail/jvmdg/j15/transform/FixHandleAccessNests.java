package xyz.wagyourtail.jvmdg.j15.transform;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.BoundaryTransform;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Nestmate emulation for pre-11 targets: privatized nestmate members get
 * synthetic {@code jvmdowngrader$handleNest$*} accessor methods emitted on
 * the host, and {@code invokedynamic} bsms/args referencing them are
 * re-pointed — the 15 boundary additionally covers handle-based access
 * (private method/field handles inside bsm args).
 */
public class FixHandleAccessNests implements BoundaryTransform {

    @Override
    public ClassNode apply(VersionProvider provider, ClassNode clazz, Set<ClassNode> extra,
                           Function<String, ClassNode> getReadOnly, Set<String> warnings,
                           boolean enableRuntime,
                           IOFunction<Type, Set<MemberNameAndDesc>> memberResolver,
                           IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        fixHandleAccessNests(provider, clazz, getReadOnly);
        return clazz;
    }

    public void fixHandleAccessNests(VersionProvider provider, ClassNode clazz, Function<String, ClassNode> getReadOnly) {
        if (clazz.nestHostClass == null) {
            fixNestsForParent(provider, clazz, getReadOnly);
        } else {
            fixNestsForChild(provider, clazz, getReadOnly);
        }
    }

    public Map<String, Object> determinePrivateFieldsAndMethodsReadByNestMembers(ClassNode clazz, Collection<ClassNode> nestMembers) {
        Map<String, Object> fields = new HashMap<>();
        for (ClassNode nestMember : nestMembers) {
            fields.putAll(determinePrivateFieldsAndMethodsReadByNestMember(clazz, nestMember));
        }
        return fields;
    }

    public Map<String, Object> determinePrivateFieldsAndMethodsReadByNestMember(ClassNode clazz, ClassNode nestMember) {
        Map<String, Object> fields = new HashMap<>();
        if (nestMember.methods == null) {
            return fields;
        }

        Map<String, Object> nestMemberPrivates = new HashMap<>();
        for (FieldNode field : clazz.fields) {
            if ((field.access & Opcodes.ACC_PRIVATE) == 0) {
                continue;
            }
            nestMemberPrivates.put(field.name, field);
        }
        for (MethodNode method : clazz.methods) {
            if ((method.access & Opcodes.ACC_PRIVATE) == 0) {
                continue;
            }
            nestMemberPrivates.put(method.name + method.desc, method);
        }

        for (MethodNode method : nestMember.methods) {
            if (method.instructions == null) {
                continue;
            }
            for (Object insn : method.instructions) {
                if (insn instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode methodInsn = (InvokeDynamicInsnNode) insn;
                    if (methodInsn.bsm.getOwner().equals(clazz.name)) {
                        if (nestMemberPrivates.containsKey(methodInsn.name + methodInsn.desc)) {
                            fields.put(methodInsn.name + methodInsn.desc, nestMemberPrivates.get(methodInsn.name + methodInsn.desc));
                        }
                    } else {
                        for (Object arg : methodInsn.bsmArgs) {
                            if (arg instanceof Handle) {
                                Handle handle = (Handle) arg;
                                if (handle.getOwner().equals(clazz.name)) {
                                    if (nestMemberPrivates.containsKey(handle.getName() + handle.getDesc())) {
                                        fields.put(handle.getName() + handle.getDesc(), nestMemberPrivates.get(handle.getName() + handle.getDesc()));
                                    } else if (nestMemberPrivates.containsKey(handle.getName())) {
                                        fields.put(handle.getName(), nestMemberPrivates.get(handle.getName()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return fields;
    }

    public void useAccessors(ClassNode clazz, Map<String, ClassNode> nestMembers) {

        if (clazz.methods == null) {
            return;
        }

        for (MethodNode method : clazz.methods) {
            if (method.instructions == null) {
                continue;
            }
            for (int i = 0; i < method.instructions.size(); i++) {
                AbstractInsnNode insn = method.instructions.get(i);
                if (insn instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode methodInsn = (InvokeDynamicInsnNode) insn;
                    if (nestMembers.containsKey(methodInsn.bsm.getOwner())) {
                        ClassNode target = nestMembers.get(methodInsn.bsm.getOwner());
                        for (MethodNode methodNode : target.methods) {
                            if (methodNode.name.equals(methodInsn.name) && methodNode.desc.equals(methodInsn.desc)) {
                                if ((methodNode.access & Opcodes.ACC_PRIVATE) == 0) {
                                    break;
                                }
                                switch (methodInsn.bsm.getTag()) {
                                    case Opcodes.H_INVOKESTATIC:
                                        methodInsn.bsm = new Handle(Opcodes.H_INVOKESTATIC, methodInsn.bsm.getOwner(), "jvmdowngrader$handleNest$" + methodInsn.bsm.getOwner().replace("/", "_") + "$" + methodInsn.bsm.getName(), methodInsn.bsm.getDesc(), false);
                                        break;
                                    case Opcodes.H_INVOKEVIRTUAL:
                                        methodInsn.bsm = new Handle(Opcodes.H_INVOKEVIRTUAL, methodInsn.bsm.getOwner(), "jvmdowngrader$handleNest$" + methodInsn.bsm.getOwner().replace("/", "_") + "$" + methodInsn.bsm.getName(), methodInsn.bsm.getDesc(), false);
                                        break;
                                    case Opcodes.H_INVOKESPECIAL:
                                        methodInsn.bsm = new Handle(Opcodes.H_INVOKESPECIAL, methodInsn.bsm.getOwner(), "jvmdowngrader$handleNest$" + methodInsn.bsm.getOwner().replace("/", "_") + "$" + methodInsn.bsm.getName(), methodInsn.bsm.getDesc(), false);
                                        break;
                                    default:
                                        throw new RuntimeException("Unexpected opcode: " + insn.getOpcode());
                                }
                                break;
                            }
                        }
                    }
                    for (int j = 0; j < methodInsn.bsmArgs.length; j++) {
                        Object arg = methodInsn.bsmArgs[j];
                        if (arg instanceof Handle) {
                            Handle handle = (Handle) arg;
                            if (nestMembers.containsKey(handle.getOwner())) {
                                ClassNode target = nestMembers.get(handle.getOwner());
                                if (handle.getOwner().equals(target.name)) {
                                    if (handle.getName().equals("<init>")) {
                                        continue;
                                    }
                                    for (MethodNode methodNode : target.methods) {
                                        if (methodNode.name.equals(handle.getName()) && methodNode.desc.equals(handle.getDesc())) {
                                            if ((methodNode.access & Opcodes.ACC_PRIVATE) == 0) {
                                                break;
                                            }
                                            switch (handle.getTag()) {
                                                case Opcodes.H_INVOKESTATIC:
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKESTATIC, handle.getOwner(), "jvmdowngrader$handleNest$" + handle.getOwner().replace("/", "_") + "$" + handle.getName(), handle.getDesc(), false);
                                                    break;
                                                case Opcodes.H_INVOKEVIRTUAL:
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKEVIRTUAL, handle.getOwner(), "jvmdowngrader$handleNest$" + handle.getOwner().replace("/", "_") + "$" + handle.getName(), handle.getDesc(), false);
                                                    break;
                                                case Opcodes.H_INVOKESPECIAL:
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKESPECIAL, handle.getOwner(), "jvmdowngrader$handleNest$" + handle.getOwner().replace("/", "_") + "$" + handle.getName(), handle.getDesc(), false);
                                                    break;
                                                default:
                                                    throw new RuntimeException("Unexpected opcode: " + insn.getOpcode());
                                            }
                                            break;
                                        }
                                    }
                                    for (FieldNode fieldNode : target.fields) {
                                        if (fieldNode.name.equals(handle.getName()) && fieldNode.desc.equals(handle.getDesc())) {
                                            if ((fieldNode.access & Opcodes.ACC_PRIVATE) == 0) {
                                                break;
                                            }
                                            switch (handle.getTag()) {
                                                case Opcodes.H_GETFIELD:
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKEVIRTUAL, handle.getOwner(), "jvmdowngrader$handleNest$" + handle.getOwner().replace("/", "_") + "$get$" + handle.getName(), handle.getDesc(), false);
                                                    break;
                                                case Opcodes.H_PUTFIELD:
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKEVIRTUAL, handle.getOwner(), "jvmdowngrader$handleNest$" + handle.getOwner().replace("/", "_") + "$set$" + handle.getName(), handle.getDesc(), false);
                                                    break;
                                                case Opcodes.H_GETSTATIC:
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKESTATIC, handle.getOwner(), "jvmdowngrader$handleNest$" + handle.getOwner().replace("/", "_") + "$get$" + handle.getName(), handle.getDesc(), false);
                                                    break;
                                                case Opcodes.H_PUTSTATIC:
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKESTATIC, handle.getOwner(), "jvmdowngrader$handleNest$" + handle.getOwner().replace("/", "_") + "$set$" + handle.getName(), handle.getDesc(), false);
                                                    break;
                                                default:
                                                    throw new RuntimeException("Unexpected opcode: " + insn.getOpcode());
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void createAccessors(VersionProvider provider, ClassNode clazz, Map<String, Object> fields) {
        for (String field : fields.keySet()) {
            if (field.contains("(")) {
                String name = field.substring(0, field.indexOf("("));
                String desc = field.substring(field.indexOf("("));

                Type methodType = Type.getMethodType(desc);
                Type[] args = methodType.getArgumentTypes();
                Type returnType = methodType.getReturnType();

                boolean isStatic;
                // determine if static
                if (fields.get(field) instanceof MethodNode) {
                    MethodNode methodNode = (MethodNode) fields.get(field);
                    isStatic = (methodNode.access & Opcodes.ACC_STATIC) != 0;
                } else {
                    throw new RuntimeException("not method?");
                }

                if (name.equals("<init>")) {

                    // just make it package-private
                    if (fields.get(field) instanceof MethodNode) {
                        MethodNode methodNode = (MethodNode) fields.get(field);
                        methodNode.access &= ~Opcodes.ACC_PRIVATE;
                    } else {
                        throw new RuntimeException("not method?");
                    }

                } else {

                    // create new method to call private method
                    MethodVisitor mv = clazz.visitMethod((isStatic ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_PUBLIC | (provider.downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "jvmdowngrader$handleNest$" + clazz.name.replace("/", "_") + "$" + name, desc, null, null);
                    mv.visitCode();
                    if (!isStatic) {
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                    }
                    int index = isStatic ? 0 : 1;
                    for (Type arg : args) {
                        mv.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), index);
                        index += arg.getSize();
                    }
                    mv.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL, clazz.name, name, desc, false);
                    if (returnType == Type.VOID_TYPE) {
                        mv.visitInsn(Opcodes.RETURN);
                    } else {
                        mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
                    }
                    mv.visitEnd();

                }

            } else {
                String desc = ((FieldNode) fields.get(field)).desc;

                Type fieldType = Type.getType(desc);

                boolean isStatic;
                // determine if static
                if (fields.get(field) instanceof FieldNode) {
                    FieldNode fieldNode = (FieldNode) fields.get(field);
                    isStatic = (fieldNode.access & Opcodes.ACC_STATIC) != 0;
                } else {
                    throw new RuntimeException("not field?");
                }

                // create new method to get field
                MethodVisitor mv = clazz.visitMethod((isStatic ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_PUBLIC | (provider.downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "jvmdowngrader$handleNest$" + clazz.name.replace("/", "_") + "$get$" + field, "()" + desc, null, null);
                mv.visitCode();
                if (!isStatic) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                }
                mv.visitFieldInsn(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, clazz.name, field, desc);
                mv.visitInsn(fieldType.getOpcode(Opcodes.IRETURN));
                mv.visitEnd();

                // create new method to set field
                mv = clazz.visitMethod((isStatic ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_PUBLIC | (provider.downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "jvmdowngrader$handleNest$" + clazz.name.replace("/", "_") + "$set$" + field, "(" + desc + ")V", null, null);
                mv.visitCode();
                if (!isStatic) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                }
                mv.visitVarInsn(fieldType.getOpcode(Opcodes.ILOAD), isStatic ? 0 : 1);
                mv.visitFieldInsn(isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD, clazz.name, field, desc);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitEnd();

            }
        }
    }

    public void fixNestsForParent(VersionProvider provider, ClassNode clazz, Function<String, ClassNode> getReadOnly) {
        if (clazz.nestMembers == null) {
            return;
        }

        Map<String, ClassNode> nestMembers = new HashMap<>();
        for (String member : clazz.nestMembers) {
            ClassNode node = getReadOnly.apply(member);
            if (node != null) {
                nestMembers.put(node.name, node);
            }
        }

        createAccessors(provider, clazz, determinePrivateFieldsAndMethodsReadByNestMembers(clazz, nestMembers.values()));
        useAccessors(clazz, nestMembers);
    }

    public void fixNestsForChild(VersionProvider provider, ClassNode clazz, Function<String, ClassNode> getReadOnly) {
        if (clazz.nestHostClass == null) {
            return;
        }

        Map<String, ClassNode> nestMembers = new HashMap<>();
        ClassNode nestHost = getReadOnly.apply(clazz.nestHostClass);
        if (nestHost == null) {
            throw new RuntimeException("nest host not found?");
        }
        for (String member : nestHost.nestMembers) {
            ClassNode node = getReadOnly.apply(member);
            if (node != null) {
                nestMembers.put(node.name, node);
            }
        }
        nestMembers.put(nestHost.name, nestHost);
        // remove self
        nestMembers.remove(clazz.name);

        createAccessors(provider, clazz, determinePrivateFieldsAndMethodsReadByNestMembers(clazz, nestMembers.values()));
        useAccessors(clazz, nestMembers);
    }

}
