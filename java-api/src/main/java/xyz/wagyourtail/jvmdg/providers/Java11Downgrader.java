package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.j11.stub.java_base.*;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Java11Downgrader extends VersionProvider {
    public Java11Downgrader() {
        super(Opcodes.V11, Opcodes.V10);
    }

    public void init() {
        // -- java.base --
        // ChaCha20Cipher
        stub(J_I_ByteArrayOutputStream.class);
        stub(J_I_FileReader.class);
        stub(J_I_FileWriter.class);
        stub(J_I_InputStream.class);
        stub(J_I_OutputStream.class);
        stub(J_I_Reader.class);
        stub(J_I_Writer.class);
        // AbstractStringBuilder
        stub(J_L_Character.class);
        stub(J_L_CharSequence.class);
        stub(J_L_Class.class);
        stub(J_L_String.class);
        stub(J_L_StringBuffer.class);
        stub(J_L_StringBuilder.class);
        // ConstantBootstraps
        // Reference
        stub(J_N_C_SelectionKey.class);
        stub(J_N_C_Selector.class);
        stub(J_N_F_Files.class);
        stub(J_N_F_Path.class);
        // RSAKey
        // XECKey
        // XECPrivateKey
        // XECPublicKey
        // NamedParameterSpec
        // PSSParameterSpec
        // RSAKeyGenParameterSpec
        // RSAMultiPrimePrivateCrtKeySpec
        // RSAPrivateCrtKeySpec
        // RSAPrivateKeySpec
        // RSAPublicKeySpec
        // XECPrivateKeySpec
        // XECPublicKeySpec
        stub(J_U_Collection.class);
        stub(J_U_Optional.class);
        stub(J_U_OptionalDouble.class);
        stub(J_U_OptionalInt.class);
        stub(J_U_OptionalLong.class);
        stub(J_U_C_TimeUnit.class);
        stub(J_U_F_Predicate.class);
        stub(J_U_R_Pattern.class);
        stub(J_U_Z_Deflater.class);
        stub(J_U_Z_Inflater.class);
        // ZipInputStream -- handled by InputStream
        // ChaCha20ParameterSpec
        // Container
        // Metrics
        // MGF1
        // RSAPSSSignature
        // RSAUtil
        // SignatureUtil

        // -- java.compiler --
        // SourceVersion

        // -- java.desktop --
        // DialogOwner
        // ListSelectionModel
        // SwingUtilities2

        // -- java.net.http --
        //TODO:
//             stub(J_N_H_HttpClient.class);
        // HttpConnectTimeoutException
//             stub(J_N_H_HttpRequest.class);
//             stub(J_N_H_HttpRequest.Builder.class);
//             stub(J_N_H_HttpRequest.BodyPublisher.class);
        // HttpResponse
        // HttpTimeoutException
        // WebSocket
        // WebSocketHandshakeException

        // -- java.xml.crypto --
        // DigestMethod
        // SignatureMethod

        // -- jdk.jshell --
        // EvalException
        // RemoteCodes

        // -- jdk.net --
        // ExtendedSocketOptions
        // Channels

        // -- jdk.unsupported.desktop --
        // DispatcherWrapper
        // DragSourceContextWrapper
        // DropTargetContextWrapper
        // LightweightContentWrapper
        // SwingInterOpUtils
        // InteropProviderImpl
    }

    @Override
    public ClassNode otherTransforms(ClassNode clazz, Set<ClassNode> extra, Function<String, ClassNode> getReadOnly) {
        super.otherTransforms(clazz);
        fixNests(clazz, getReadOnly);
        return clazz;
    }

    public void fixNests(ClassNode clazz, Function<String, ClassNode> getReadOnly) {
        if (clazz.nestHostClass == null) {
            fixNestsForParent(clazz, getReadOnly);
        } else {
            fixNestsForChild(clazz, getReadOnly);
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
                } else if (insn instanceof InvokeDynamicInsnNode) {
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
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    if (nestMembers.containsKey(fieldInsn.owner)) {
                        ClassNode target = nestMembers.get(fieldInsn.owner);
                        for (FieldNode field : target.fields) {
                            if (field.name.equals(fieldInsn.name)) {
                                if ((field.access & Opcodes.ACC_PRIVATE) == 0) {
                                    break;
                                }
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
                                break;
                            }
                        }
                    }
                } else if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    if (nestMembers.containsKey(methodInsn.owner) && !methodInsn.name.equals("<init>")) {
                        ClassNode target = nestMembers.get(methodInsn.owner);
                        for (MethodNode methodNode : target.methods) {
                            if (methodNode.name.equals(methodInsn.name) && methodNode.desc.equals(methodInsn.desc)) {
                                if ((methodNode.access & Opcodes.ACC_PRIVATE) == 0) {
                                    break;
                                }
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
                                break;
                            }
                        }
                    }
                } else if (insn instanceof InvokeDynamicInsnNode) {
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
                                        methodInsn.bsm = new Handle(Opcodes.H_INVOKESTATIC, methodInsn.bsm.getOwner(), "jvmdowngrader$nest$" + methodInsn.bsm.getOwner().replace("/", "_") + "$" + methodInsn.bsm.getName(), methodInsn.bsm.getDesc(), false);
                                        break;
                                    case Opcodes.H_INVOKEVIRTUAL:
                                        methodInsn.bsm = new Handle(Opcodes.H_INVOKEVIRTUAL, methodInsn.bsm.getOwner(), "jvmdowngrader$nest$" + methodInsn.bsm.getOwner().replace("/", "_") + "$" + methodInsn.bsm.getName(), methodInsn.bsm.getDesc(), false);
                                        break;
                                    case Opcodes.H_INVOKESPECIAL:
                                        methodInsn.bsm = new Handle(Opcodes.H_INVOKESPECIAL, methodInsn.bsm.getOwner(), "jvmdowngrader$nest$" + methodInsn.bsm.getOwner().replace("/", "_") + "$" + methodInsn.bsm.getName(), methodInsn.bsm.getDesc(), false);
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
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKESTATIC, handle.getOwner(), "jvmdowngrader$nest$" + handle.getOwner().replace("/", "_") + "$" + handle.getName(), handle.getDesc(), false);
                                                    break;
                                                case Opcodes.H_INVOKEVIRTUAL:
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKEVIRTUAL, handle.getOwner(), "jvmdowngrader$nest$" + handle.getOwner().replace("/", "_") + "$" + handle.getName(), handle.getDesc(), false);
                                                    break;
                                                case Opcodes.H_INVOKESPECIAL:
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKESPECIAL, handle.getOwner(), "jvmdowngrader$nest$" + handle.getOwner().replace("/", "_") + "$" + handle.getName(), handle.getDesc(), false);
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
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKEVIRTUAL, handle.getOwner(), "jvmdowngrader$nest$" + handle.getOwner().replace("/", "_") + "$get$" + handle.getName(), handle.getDesc(), false);
                                                    break;
                                                case Opcodes.H_PUTFIELD:
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKEVIRTUAL, handle.getOwner(), "jvmdowngrader$nest$" + handle.getOwner().replace("/", "_") + "$set$" + handle.getName(), handle.getDesc(), false);
                                                    break;
                                                case Opcodes.H_GETSTATIC:
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKESTATIC, handle.getOwner(), "jvmdowngrader$nest$" + handle.getOwner().replace("/", "_") + "$get$" + handle.getName(), handle.getDesc(), false);
                                                    break;
                                                case Opcodes.H_PUTSTATIC:
                                                    methodInsn.bsmArgs[j] = new Handle(Opcodes.H_INVOKESTATIC, handle.getOwner(), "jvmdowngrader$nest$" + handle.getOwner().replace("/", "_") + "$set$" + handle.getName(), handle.getDesc(), false);
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

    private void createAccessors(ClassNode clazz, Map<String, Object> fields) {
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

                    // create new package-private method to call private method
                    MethodVisitor mv = clazz.visitMethod(isStatic ? Opcodes.ACC_STATIC : 0, "jvmdowngrader$nest$" + clazz.name.replace("/", "_") + "$" + name, desc, null, null);
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

                // create new package-private method to get field
                MethodVisitor mv = clazz.visitMethod(isStatic ? Opcodes.ACC_STATIC : 0, "jvmdowngrader$nest$" + clazz.name.replace("/", "_") + "$get$" + field, "()" + desc, null, null);
                mv.visitCode();
                if (!isStatic) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                }
                mv.visitFieldInsn(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, clazz.name, field, desc);
                mv.visitInsn(fieldType.getOpcode(Opcodes.IRETURN));
                mv.visitEnd();

                // create new package-private method to set field
                mv = clazz.visitMethod(isStatic ? Opcodes.ACC_STATIC : 0, "jvmdowngrader$nest$" + clazz.name.replace("/", "_") + "$set$" + field, "(" + desc + ")V", null, null);
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

    public void fixNestsForParent(ClassNode clazz, Function<String, ClassNode> getReadOnly) {
        if (clazz.nestMembers == null) {
            return;
        }

        // create nest members synthetic class
        StringBuilder sb = new StringBuilder();
        for (String member : clazz.nestMembers) {
            sb.append(member).append(":");
        }
        sb.deleteCharAt(sb.length() - 1);

        clazz.visitField(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
            "jvmdowngrader$nestMembers",
            "Ljava/lang/String;",
            null,
            sb.toString()
        ).visitEnd();

        Map<String, ClassNode> nestMembers = new HashMap<>();
        for (String member : clazz.nestMembers) {
            ClassNode node = getReadOnly.apply(member);
            if (node != null) {
                nestMembers.put(node.name, node);
            }
        }

        createAccessors(clazz, determinePrivateFieldsAndMethodsReadByNestMembers(clazz, nestMembers.values()));
        useAccessors(clazz, nestMembers);

    }

    public void fixNestsForChild(ClassNode clazz, Function<String, ClassNode> getReadOnly) {
        if (clazz.nestHostClass == null) {
            return;
        }

        // create nest members synthetic class
        clazz.visitField(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
            "jvmdowngrader$nestHost",
            "Ljava/lang/String;",
            null,
            clazz.nestHostClass
        ).visitEnd();

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

        createAccessors(clazz, determinePrivateFieldsAndMethodsReadByNestMembers(clazz, nestMembers.values()));
        useAccessors(clazz, nestMembers);
    }
}