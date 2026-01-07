package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.j11.NestHost;
import xyz.wagyourtail.jvmdg.j11.NestMembers;
import xyz.wagyourtail.jvmdg.j11.stub.java_base.*;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpClient;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpHeaders;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpRequest;
import xyz.wagyourtail.jvmdg.j11.stub.java_net_http.J_N_H_HttpResponse;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;
import java.util.*;

public class Java11Downgrader extends VersionProvider {
    public Java11Downgrader() {
        super(Opcodes.V11, Opcodes.V10, 0);
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
        stub(J_L_I_ConstantBootstraps.class);
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
        stub(J_N_H_HttpClient.class);
        // HttpConnectTimeoutException
        stub(J_N_H_HttpHeaders.class);
        stub(J_N_H_HttpRequest.class);
        stub(J_N_H_HttpResponse.class);
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
    public ClassNode otherTransforms(ClassNode clazz, Set<ClassNode> extra, Function<String, ClassNode> getReadOnly) throws IOException {
        super.otherTransforms(clazz);
        if (clazz.name.equals("module-info")) {
            return clazz;
        }
        fixNests(clazz, getReadOnly);
        replaceCondy(clazz);
        fixPrivateMethodsInInterfaces(clazz);
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

                    // create new method to call private method
                    MethodVisitor mv = clazz.visitMethod((isStatic ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_PUBLIC | (downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "jvmdowngrader$nest$" + clazz.name.replace("/", "_") + "$" + name, desc, null, null);
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
                MethodVisitor mv = clazz.visitMethod((isStatic ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_PUBLIC | (downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "jvmdowngrader$nest$" + clazz.name.replace("/", "_") + "$get$" + field, "()" + desc, null, null);
                mv.visitCode();
                if (!isStatic) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                }
                mv.visitFieldInsn(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, clazz.name, field, desc);
                mv.visitInsn(fieldType.getOpcode(Opcodes.IRETURN));
                mv.visitEnd();

                // create new method to set field
                mv = clazz.visitMethod((isStatic ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_PUBLIC | (downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), "jvmdowngrader$nest$" + clazz.name.replace("/", "_") + "$set$" + field, "(" + desc + ")V", null, null);
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

        createAccessors(clazz, determinePrivateFieldsAndMethodsReadByNestMembers(clazz, nestMembers.values()));
        useAccessors(clazz, nestMembers);

        clazz.nestMembers = null;
    }

    public void fixNestsForChild(ClassNode clazz, Function<String, ClassNode> getReadOnly) {
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

        createAccessors(clazz, determinePrivateFieldsAndMethodsReadByNestMembers(clazz, nestMembers.values()));
        useAccessors(clazz, nestMembers);

        clazz.nestHostClass = null;
    }

    public void replaceCondy(ClassNode clazz) {
        for (MethodNode method : clazz.methods) {
            replaceCondy(method);
        }
    }

    public void replaceCondy(MethodNode method) {
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode insn = method.instructions.get(i);
            if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (ldc.cst instanceof ConstantDynamic) {
                    // "upgrade" to indy
                    ConstantDynamic condy = (ConstantDynamic) ldc.cst;
                    Object[] bsmArgs = new Object[condy.getBootstrapMethodArgumentCount() + 1];
                    bsmArgs[0] = condy.getBootstrapMethod();
                    for (int j = 0; j < condy.getBootstrapMethodArgumentCount(); j++) {
                        bsmArgs[j + 1] = condy.getBootstrapMethodArgument(j);
                    }
                    InvokeDynamicInsnNode indy = new InvokeDynamicInsnNode(
                        condy.getName(),
                        "()" + condy.getDescriptor(),
                        new Handle(
                            Opcodes.H_INVOKESTATIC,
                            Type.getInternalName(J_L_I_ConstantBootstraps.class),
                            "ldcCondyToIndy",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
                            false
                        ),
                        bsmArgs
                    );
                    method.instructions.set(insn, indy);
                    insn = indy;
                }
            }
            if (insn instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                StringBuilder condyArgs = new StringBuilder();
                for (int j = 0; j < indy.bsmArgs.length; j++) {
                    if (indy.bsmArgs[j] instanceof ConstantDynamic) {
                        condyArgs.append((char) j);
                    }
                }
                if (condyArgs.length() > 0) {
                    // replace with nest indy
                    List<Object> bsmArgs = new ArrayList<>();
                    bsmArgs.add(indy.bsm);
                    bsmArgs.add(condyArgs.toString());
                    for (Object bsmArg : indy.bsmArgs) {
                        if (bsmArg instanceof ConstantDynamic) {
                            ConstantDynamic condy = (ConstantDynamic) bsmArg;
                            insertCondyArgs(condy, bsmArgs);
                        } else {
                            bsmArgs.add(bsmArg);
                        }
                    }
                    indy = new InvokeDynamicInsnNode(
                        indy.name,
                        indy.desc,
                        new Handle(
                            Opcodes.H_INVOKESTATIC,
                            Type.getInternalName(J_L_I_ConstantBootstraps.class),
                            "nestedCondyInIndy",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
                            false
                        ),
                        bsmArgs.toArray()
                    );
                    method.instructions.set(insn, indy);
                }
            }
        }
    }

    public void insertCondyArgs(ConstantDynamic condy, List<Object> bsmArgs) {
        bsmArgs.add(condy.getBootstrapMethod());
        bsmArgs.add(condy.getName());
        bsmArgs.add(Type.getType(condy.getDescriptor()));
        bsmArgs.add(condy.getBootstrapMethodArgumentCount());
        StringBuilder condyArgs = new StringBuilder();
        for (int j = 0; j < condy.getBootstrapMethodArgumentCount(); j++) {
            if (condy.getBootstrapMethodArgument(j) instanceof ConstantDynamic) {
                condyArgs.append((char) j);
            }
        }
        bsmArgs.add(condyArgs.toString());
        for (int j = 0; j < condy.getBootstrapMethodArgumentCount(); j++) {
            Object o = condy.getBootstrapMethodArgument(j);
            if (o instanceof ConstantDynamic) {
                insertCondyArgs((ConstantDynamic) o, bsmArgs);
            } else {
                bsmArgs.add(o);
            }
        }
    }

    public void fixPrivateMethodsInInterfaces(ClassNode node) {
        if ((node.access & Opcodes.ACC_INTERFACE) == 0) return;

        List<String> privateMethods = new ArrayList<>();
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_PRIVATE) != 0) {
                privateMethods.add(method.name + method.desc);
            }
        }

        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    if (min.getOpcode() == Opcodes.INVOKEINTERFACE && min.owner.equals(node.name) && privateMethods.contains(min.name + min.desc)) {
                        min.setOpcode(Opcodes.INVOKESPECIAL);
                    }
                } else if (insn instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                    if (indy.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
                        if (indy.bsmArgs[1] instanceof Handle) {
                            Handle lambda = (Handle) indy.bsmArgs[1];
                            if (lambda.getOwner().equals(node.name) &&
                                lambda.getTag() == Opcodes.H_INVOKEINTERFACE &&
                                privateMethods.contains(lambda.getName() + lambda.getDesc())
                            ) {
                                indy.bsmArgs[1] = new Handle(Opcodes.H_INVOKESPECIAL, lambda.getOwner(), lambda.getName(), lambda.getDesc(), lambda.isInterface());
                            }
                        }
                    }
                }
            }
        }
    }

}
