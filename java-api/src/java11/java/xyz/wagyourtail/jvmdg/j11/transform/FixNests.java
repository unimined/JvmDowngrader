package xyz.wagyourtail.jvmdg.j11.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.j11.NestHost;
import xyz.wagyourtail.jvmdg.j11.NestMembers;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.BoundaryTransform;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.*;

/** nestmate emulation for pre-11 targets: nestmate private members get
 * synthetic {@code jvmdowngrader$nest$*} accessors on the owner, and direct
 * invokespecial/invokeinterface/getfield/putfield sites inside the nest are
 * re-pointed to them */
public class FixNests implements BoundaryTransform {

    @Override
    public ClassNode apply(VersionProvider provider, ClassNode clazz, Set<ClassNode> extra,
                           Function<String, ClassNode> getReadOnly, Set<String> warnings,
                           boolean enableRuntime,
                           IOFunction<Type, Set<MemberNameAndDesc>> memberResolver,
                           IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        if (clazz.name.equals("module-info")) {
            return clazz;
        }
        fixNests(provider, clazz, getReadOnly);
        return clazz;
    }

    @Override
    public int order() { return 0; }

    public void fixNests(VersionProvider provider, ClassNode clazz, Function<String, ClassNode> getReadOnly) {
        if (clazz.nestHostClass == null) {
            fixNestsForParent(provider, clazz, getReadOnly);
        } else {
            fixNestsForChild(provider, clazz, getReadOnly);
        }
    }

    public Map<String, Object> determinePrivateFieldsAndMethodsReadByNestMembers(ClassNode clazz, Collection<ClassNode> nestMembers) {
        // clazz's privates are scanned once for the whole nest rather than
        // rebuilt per member
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

        Map<String, Object> fields = new HashMap<>();
        for (ClassNode nestMember : nestMembers) {
            fields.putAll(determinePrivateFieldsAndMethodsReadByNestMember(clazz, nestMember, nestMemberPrivates));
        }
        return fields;
    }

    public Map<String, Object> determinePrivateFieldsAndMethodsReadByNestMember(ClassNode clazz, ClassNode nestMember) {
        return determinePrivateFieldsAndMethodsReadByNestMember(clazz, nestMember, privateMembers(clazz));
    }

    private static Map<String, Object> privateMembers(ClassNode clazz) {
        Map<String, Object> privates = new HashMap<>();
        for (FieldNode field : clazz.fields) {
            if ((field.access & Opcodes.ACC_PRIVATE) == 0) {
                continue;
            }
            privates.put(field.name, field);
        }
        for (MethodNode method : clazz.methods) {
            if ((method.access & Opcodes.ACC_PRIVATE) == 0) {
                continue;
            }
            privates.put(method.name + method.desc, method);
        }
        return privates;
    }

    private Map<String, Object> determinePrivateFieldsAndMethodsReadByNestMember(ClassNode clazz, ClassNode nestMember, Map<String, Object> nestMemberPrivates) {
        Map<String, Object> fields = new HashMap<>();
        if (nestMember.methods == null) {
            return fields;
        }

        for (MethodNode method : nestMember.methods) {
            if (method.instructions == null) {
                continue;
            }
            for (Object insn : method.instructions) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    if (fieldInsn.owner.equals(clazz.name)) {
                        if (nestMemberPrivates.containsKey(fieldInsn.name)) {
                            fields.put(fieldInsn.name, nestMemberPrivates.get(fieldInsn.name));
                        }
                    }
                } else if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    if (methodInsn.owner.equals(clazz.name)) {
                        if (nestMemberPrivates.containsKey(methodInsn.name + methodInsn.desc)) {
                            fields.put(methodInsn.name + methodInsn.desc, nestMemberPrivates.get(methodInsn.name + methodInsn.desc));
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

        // index each member's private fields/methods once — the insn loop
        // resolves accesses in O(1) instead of scanning target.members per
        // site. keys: field name / method name+desc (desc's "(" keeps the
        // namespaces disjoint)
        Map<String, Map<String, Object>> privatesByOwner = new HashMap<>();
        for (ClassNode member : nestMembers.values()) {
            privatesByOwner.put(member.name, privateMembers(member));
        }

        for (MethodNode method : clazz.methods) {
            if (method.instructions == null) {
                continue;
            }
            for (int i = 0; i < method.instructions.size(); i++) {
                AbstractInsnNode insn = method.instructions.get(i);
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    Map<String, Object> privates = privatesByOwner.get(fieldInsn.owner);
                    if (privates != null && privates.get(fieldInsn.name) instanceof FieldNode) {
                        switch (insn.getOpcode()) {
                            case Opcodes.GETSTATIC:
                                method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, fieldInsn.owner, "jvmdowngrader$nest$" + fieldInsn.owner.replace("/", "_") + "$get$" + fieldInsn.name, "()" + fieldInsn.desc, false));
                                break;
                            case Opcodes.PUTSTATIC:
                                method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, fieldInsn.owner, "jvmdowngrader$nest$" + fieldInsn.owner.replace("/", "_") + "$set$" + fieldInsn.name, "(" + fieldInsn.desc + ")V", false));
                                break;
                            case Opcodes.GETFIELD:
                                method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, fieldInsn.owner, "jvmdowngrader$nest$" + fieldInsn.owner.replace("/", "_") + "$get$" + fieldInsn.name, "()" + fieldInsn.desc, false));
                                break;
                            case Opcodes.PUTFIELD:
                                method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, fieldInsn.owner, "jvmdowngrader$nest$" + fieldInsn.owner.replace("/", "_") + "$set$" + fieldInsn.name, "(" + fieldInsn.desc + ")V", false));
                                break;
                            default:
                                throw new RuntimeException("Unexpected opcode: " + insn.getOpcode());
                        }
                    }
                } else if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    Map<String, Object> privates = privatesByOwner.get(methodInsn.owner);
                    if (privates != null && !methodInsn.name.equals("<init>")
                        && privates.get(methodInsn.name + methodInsn.desc) instanceof MethodNode) {
                        switch (insn.getOpcode()) {
                            case Opcodes.INVOKESTATIC:
                                method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, methodInsn.owner, "jvmdowngrader$nest$" + methodInsn.owner.replace("/", "_") + "$" + methodInsn.name, methodInsn.desc, false));
                                break;
                            case Opcodes.INVOKEVIRTUAL:
                                method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, methodInsn.owner, "jvmdowngrader$nest$" + methodInsn.owner.replace("/", "_") + "$" + methodInsn.name, methodInsn.desc, false));
                                break;
                            case Opcodes.INVOKESPECIAL:
                                method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESPECIAL, methodInsn.owner, "jvmdowngrader$nest$" + methodInsn.owner.replace("/", "_") + "$" + methodInsn.name, methodInsn.desc, false));
                                break;
                            default:
                                throw new RuntimeException("Unexpected opcode: " + insn.getOpcode());
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
                    MethodVisitor mv = clazz.visitMethod((isStatic ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_PUBLIC | (provider.downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "jvmdowngrader$nest$" + clazz.name.replace("/", "_") + "$" + name, desc, null, null);
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
                MethodVisitor mv = clazz.visitMethod((isStatic ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_PUBLIC | (provider.downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "jvmdowngrader$nest$" + clazz.name.replace("/", "_") + "$get$" + field, "()" + desc, null, null);
                mv.visitCode();
                if (!isStatic) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                }
                mv.visitFieldInsn(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, clazz.name, field, desc);
                mv.visitInsn(fieldType.getOpcode(Opcodes.IRETURN));
                mv.visitEnd();

                // create new method to set field
                mv = clazz.visitMethod((isStatic ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_PUBLIC | (provider.downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "jvmdowngrader$nest$" + clazz.name.replace("/", "_") + "$set$" + field, "(" + desc + ")V", null, null);
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

        // create nest members synthetic class

        AnnotationVisitor av = clazz.visitAnnotation(Type.getType(NestMembers.class).getDescriptor(), true);
        AnnotationVisitor values = av.visitArray("value");
        for (String member : clazz.nestMembers) {
            values.visit(null, Type.getObjectType(member));
        }
        values.visitEnd();
        av.visitEnd();

        Map<String, ClassNode> nestMembers = new HashMap<>();
        for (String member : clazz.nestMembers) {
            ClassNode node = getReadOnly.apply(member);
            if (node != null) {
                nestMembers.put(node.name, node);
            }
        }

        createAccessors(provider, clazz, determinePrivateFieldsAndMethodsReadByNestMembers(clazz, nestMembers.values()));
        useAccessors(clazz, nestMembers);

        clazz.nestMembers = null;
    }

    public void fixNestsForChild(VersionProvider provider, ClassNode clazz, Function<String, ClassNode> getReadOnly) {
        if (clazz.nestHostClass == null) {
            return;
        }

        AnnotationVisitor av = clazz.visitAnnotation(Type.getType(NestHost.class).getDescriptor(), true);
        av.visit("value", Type.getObjectType(clazz.nestHostClass));
        av.visitEnd();

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

        clazz.nestHostClass = null;
    }


}
