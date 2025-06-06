package xyz.wagyourtail.jvmdg.providers;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.j7.stub.J_L_Throwable;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.ReflectionReferences;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.*;

public class Java7Downgrader extends VersionProvider {

    public Java7Downgrader() {
        super(Opcodes.V1_7, Opcodes.V1_6, 0);
    }

    @Override
    public void ensureInit(ClassDowngrader downgrader) {
        if (!isInitialized()) {
            downgrader.logger.warn("Java 7 -> 6 Stubs are VERY incomplete!");
        }
        super.ensureInit(downgrader);
    }

    @Override
    public void init() {
        stub(J_L_Throwable.class);
    }

    @Override
    public ClassNode otherTransforms(ClassNode clazz, Set<ClassNode> extra, Function<String, ClassNode> getReadOnly, Set<String> warnings, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        InsnList addToClinit = new InsnList();
        Type handleType = stubClass(Type.getObjectType("java/lang/invoke/MethodHandle"), warnings);
        Type callSiteType = stubClass(Type.getObjectType("java/lang/invoke/CallSite"), warnings);

        Type handlesType = stubClass(Type.getObjectType("java/lang/invoke/MethodHandles"), warnings);
        Type lookupType = stubClass(Type.getObjectType("java/lang/invoke/MethodHandles$Lookup"), warnings);
        Type methodType = stubClass(Type.getObjectType("java/lang/invoke/MethodType"), warnings);

        MethodNode clinit = null;
        List<FullyQualifiedMemberNameAndDesc> reflectionRefList = new ArrayList<>();
        for (MethodNode method : new ArrayList<>(clazz.methods)) {
            if (method.name.equals("<clinit>")) {
                clinit = method;
            }
            if (method.instructions != null) {
                for (int i = 0; i < method.instructions.size(); i++) {
                    AbstractInsnNode insn = method.instructions.get(i);
                    if (insn.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
                        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                        String name = indyToMethod(method, indy, clazz, addToClinit, callSiteType, handleType, lookupType, methodType, reflectionRefList);
                        InsnList insns = new InsnList();

                        method.instructions.set(indy, new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            clazz.name,
                            name,
                            indy.desc,
                            false
                        ));
                    }
                    if (insn.getType() == AbstractInsnNode.LDC_INSN) {
                        assert insn instanceof LdcInsnNode;
                        Object cst = ((LdcInsnNode) insn).cst;
                        if (cst instanceof Handle) {
                            String name = handleToLookupField((Handle) cst, clazz, addToClinit, handleType, lookupType, methodType, reflectionRefList);
                            method.instructions.set(insn, new FieldInsnNode(Opcodes.GETSTATIC, clazz.name, name, handleType.getDescriptor()));
                        } else if (cst instanceof Type && ((Type) cst).getSort() == Type.METHOD) {
                            String name = methodDescToLookupField((Type) cst, clazz, addToClinit, methodType);
                            method.instructions.set(insn, new FieldInsnNode(Opcodes.GETSTATIC, clazz.name, name, methodType.getDescriptor()));
                        }
                    }
                }
            }
        }
        if (addToClinit.size() > 0) {
            if (clinit == null) {
                clinit = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                clinit.visitCode();
                clinit.visitInsn(Opcodes.RETURN);
                clinit.visitMaxs(0, 0);
                clinit.visitEnd();
                clazz.methods.add(clinit);
            }
            if (clinit.visibleAnnotations == null) {
                clinit.visibleAnnotations = new ArrayList<>();
            }
            String reflectionRefs = Type.getType(ReflectionReferences.class).getDescriptor();
            String refType = Type.getType(Ref.class).getDescriptor();
            AnnotationNode node = null;
            for (AnnotationNode a : clinit.visibleAnnotations) {
                if (a.desc.equals(reflectionRefs)) {
                    node = a;
                    break;
                }
            }
            if (node == null) {
                node = new AnnotationNode(reflectionRefs);
                clinit.visibleAnnotations.add(node);
                node.values = new ArrayList<Object>(Arrays.asList("value", new ArrayList<AnnotationNode>()));
            }
            List<AnnotationNode> refs = ((List<AnnotationNode>) node.values.get(1));
            for (FullyQualifiedMemberNameAndDesc ref : reflectionRefList) {
                AnnotationNode refAnn = new AnnotationNode(refType);
                refAnn.visit("value", ref.getOwner().getInternalName());
                refAnn.visit("member", ref.getName());
                refAnn.visit("desc", ref.getDesc().toString());
                refAnn.visitEnd();
                refs.add(refAnn);
            }
            Handle lookup = stubHandle(clazz, clinit, extra, null, null, enableRuntime, memberResolver, superTypeResolver, warnings,
                new Handle(
                    Opcodes.H_INVOKESTATIC,
                    handlesType.getInternalName(),
                    "lookup",
                    "()" + lookupType.getDescriptor(),
                    false
                )
            );
            clazz.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "jvmdg$lookup", lookupType.getDescriptor(), null, null);
            InsnList lookupInsns = new InsnList();
            lookupInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, lookup.getOwner(), lookup.getName(), lookup.getDesc(), false));
            lookupInsns.add(new FieldInsnNode(Opcodes.PUTSTATIC, clazz.name, "jvmdg$lookup", lookupType.getDescriptor()));

            addToClinit.insert(lookupInsns);
            clinit.instructions.insert(addToClinit);
        }
        return super.otherTransforms(clazz);
    }

    public String indyToMethod(MethodNode method, InvokeDynamicInsnNode indy, ClassNode clazz, InsnList addToClinit, Type callsiteType, Type handleType, Type lookupType, Type methodType, List<FullyQualifiedMemberNameAndDesc> reflectionRefList) {
        InvokeDynamicType it = new InvokeDynamicType(indy);
        for (FieldNode field : clazz.fields) {
            if (field instanceof IndyField && ((IndyField) field).indy.equals(it)) {
                return field.name;
            }
        }
        int count = clazz.fields.size();
        IndyField indyField = new IndyField(it, callsiteType, count);
        clazz.fields.add(indyField);

        addToClinit.add(new FieldInsnNode(Opcodes.GETSTATIC, clazz.name, "jvmdg$lookup", lookupType.getDescriptor()));
        addToClinit.add(new LdcInsnNode(indy.name));
        addToClinit.add(methodDescToMethodType(Type.getMethodType(indy.desc), methodType));

        for (Object arg : indy.bsmArgs) {
            if (arg instanceof Type) {
                if (((Type) arg).getSort() == Type.METHOD) {
                    addToClinit.add(methodDescToMethodType((Type) arg, methodType));
                } else {
                    addToClinit.add(new LdcInsnNode(arg));
                }
            } else if (arg instanceof Handle) {
                addToClinit.add(handleToLookupStack((Handle) arg, clazz, handleType, lookupType, methodType, reflectionRefList));
            } else {
                addToClinit.add(new LdcInsnNode(arg));
            }
        }

        addToClinit.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            indy.bsm.getOwner(),
            indy.bsm.getName(),
            indy.bsm.getDesc(),
            indy.bsm.isInterface()
        ));

        addToClinit.add(new FieldInsnNode(
            Opcodes.PUTSTATIC,
            clazz.name,
            indyField.name,
            indyField.desc
        ));

        IndyMethod indyMethod = new IndyMethod(it, count);
        clazz.methods.add(indyMethod);
        indyMethod.visitCode();
        indyMethod.visitFieldInsn(Opcodes.GETSTATIC, clazz.name, indyField.name, callsiteType.getDescriptor());
        indyMethod.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            callsiteType.getInternalName(),
            "getTarget",
            Type.getMethodDescriptor(handleType),
            false
        );

        int i = 0;
        for (Type arg : Type.getArgumentTypes(indy.desc)) {
            indyMethod.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), i);
            i += arg.getSize();
        }

        // TODO: fix if stubbing MethodHandle properly
        indyMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, handleType.getInternalName(), "invokeExact", indy.desc, false);
        Type returnType = Type.getReturnType(indy.desc);
        indyMethod.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
        indyMethod.visitMaxs(0, 0);
        indyMethod.visitEnd();

        return indyMethod.name;
    }

    public String handleToLookupField(Handle handle, ClassNode clazz, InsnList addToClinit, Type handleType, Type lookupType, Type methodType, List<FullyQualifiedMemberNameAndDesc> reflectionRefList) {
        for (FieldNode field : clazz.fields) {
            if (field instanceof HandleField && handle.equals(((HandleField) field).handle)) {
                return field.name;
            }
        }
        HandleField field = new HandleField(handle, handleType, clazz.fields.size());
        clazz.fields.add(field);
        addToClinit.add(handleToLookupStack(handle, clazz, handleType, lookupType, methodType, reflectionRefList));
        addToClinit.add(new FieldInsnNode(Opcodes.PUTSTATIC, clazz.name, field.name, field.desc));
        return field.name;
    }

    public InsnList handleToLookupStack(Handle handle, ClassNode clazz, Type handleType, Type lookupType, Type methodType, List<FullyQualifiedMemberNameAndDesc> reflectionRefList) {
        reflectionRefList.add(FullyQualifiedMemberNameAndDesc.of(handle));
        InsnList insns = new InsnList();
        insns.add(new FieldInsnNode(Opcodes.GETSTATIC, clazz.name, "jvmdg$lookup", lookupType.getDescriptor()));
        insns.add(new LdcInsnNode(Type.getObjectType(handle.getOwner())));
        if (handle.getTag() != Opcodes.H_NEWINVOKESPECIAL) {
            insns.add(new LdcInsnNode(handle.getName()));
        }
        String funcName;
        switch (handle.getTag()) {
            case Opcodes.H_GETFIELD:
                funcName = "findGetter";
                break;
            case Opcodes.H_GETSTATIC:
                funcName = "findStaticGetter";
                break;
            case Opcodes.H_PUTFIELD:
                funcName = "findSetter";
                break;
            case Opcodes.H_PUTSTATIC:
                funcName = "findStaticSetter";
                break;
            case Opcodes.H_INVOKEVIRTUAL:
            case Opcodes.H_INVOKEINTERFACE:
                funcName = "findVirtual";
                break;
            case Opcodes.H_INVOKESTATIC:
                funcName = "findStatic";
                break;
            case Opcodes.H_INVOKESPECIAL:
                funcName = "findSpecial";
                break;
            case Opcodes.H_NEWINVOKESPECIAL:
                funcName = "findConstructor";
                break;
            default:
                throw new IllegalStateException();
        }
        String desc;
        switch (handle.getTag()) {
            case Opcodes.H_GETFIELD:
            case Opcodes.H_GETSTATIC:
            case Opcodes.H_PUTFIELD:
            case Opcodes.H_PUTSTATIC:
                desc = Type.getMethodDescriptor(handleType, Type.getObjectType("java/lang/Class"), Type.getObjectType("java/lang/String"), Type.getObjectType("java/lang/Class"));
                insns.add(new LdcInsnNode(Type.getType(handle.getDesc())));
                break;
            case Opcodes.H_INVOKEVIRTUAL:
            case Opcodes.H_INVOKESTATIC:
            case Opcodes.H_INVOKEINTERFACE:
                desc = Type.getMethodDescriptor(handleType, Type.getObjectType("java/lang/Class"), Type.getObjectType("java/lang/String"), methodType);
                insns.add(methodDescToMethodType(Type.getMethodType(handle.getDesc()), methodType));
                break;
            case Opcodes.H_INVOKESPECIAL:
                desc = Type.getMethodDescriptor(handleType, Type.getObjectType("java/lang/Class"), Type.getObjectType("java/lang/String"), methodType, Type.getObjectType("java/lang/Class"));
                insns.add(methodDescToMethodType(Type.getMethodType(handle.getDesc()), methodType));
                insns.add(new LdcInsnNode(Type.getObjectType(clazz.name)));
                break;
            case Opcodes.H_NEWINVOKESPECIAL:
                desc = Type.getMethodDescriptor(handleType, Type.getObjectType("java/lang/Class"), methodType);
                insns.add(methodDescToMethodType(Type.getMethodType(handle.getDesc()), methodType));
                break;
            default:
                throw new IllegalStateException();
        }
        insns.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            lookupType.getInternalName(),
            funcName,
            desc,
            false
        ));
        return insns;
    }

    public static String methodDescToLookupField(Type desc, ClassNode clazz, InsnList addToClinit, Type methodType) {
        int i = 0;
        for (FieldNode field : clazz.fields) {
            if (field instanceof MethodTypeField) {
                if (desc.equals(((MethodTypeField) field).type)) {
                    return field.name;
                }
                i++;
            }
        }
        MethodTypeField type = new MethodTypeField(desc, "jvmdg$methodtype$" + i, methodType);
        clazz.fields.add(type);
        addToClinit.add(methodDescToMethodType(desc, methodType));
        addToClinit.add(new FieldInsnNode(Opcodes.PUTSTATIC, clazz.name, type.name, type.desc));
        return type.name;
    }

    public static InsnList methodDescToMethodType(Type desc, Type methodType) {
        InsnList l = new InsnList();
        Type returnType = desc.getReturnType();
        Type[] args = desc.getArgumentTypes();
        if (returnType.getSort() < Type.ARRAY) {
            l.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                ASMUtils.getBoxFor(returnType).getInternalName(),
                "TYPE",
                "Ljava/lang/Class;"
            ));
        } else {
            l.add(new LdcInsnNode(returnType));
        }
        l.add(new LdcInsnNode(args.length));
        l.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Class"));
        for (int i = 0; i < args.length; i++) {
            l.add(new InsnNode(Opcodes.DUP));
            l.add(new LdcInsnNode(i));
            if (args[i].getSort() < Type.ARRAY) {
                l.add(new FieldInsnNode(
                    Opcodes.GETSTATIC,
                    ASMUtils.getBoxFor(args[i]).getInternalName(),
                    "TYPE",
                    "Ljava/lang/Class;"
                ));
            } else {
                l.add(new LdcInsnNode(args[i]));
            }
            l.add(new InsnNode(Opcodes.AASTORE));
        }
        l.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            methodType.getInternalName(),
            "methodType",
            "(Ljava/lang/Class;[Ljava/lang/Class;)" + methodType.getDescriptor(),
            false
        ));
        return l;
    }

    public static class IndyMethod extends MethodNode {
        private final InvokeDynamicType indy;


        public IndyMethod(InvokeDynamicType indy, int count) {
            super(Opcodes.ASM9, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "jvmdg$indy$" + indy.indy.name + "$" + count, indy.indy.desc, null, null);
            this.indy = indy;
        }

    }

    public static class MethodTypeField extends FieldNode {
        private final Type type;

        public MethodTypeField(Type type, String name, Type methodType) {
            super(Opcodes.ACC_PUBLIC, name, methodType.getDescriptor(), null, null);
            this.type = type;
        }
    }

    public static class HandleField extends FieldNode {
        private final Handle handle;

        public HandleField(Handle handle, Type handleType, int count) {
            super(Opcodes.ASM9, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "jvmdg$handle$" + handle.getName() + "$" + count, handleType.getDescriptor(), null, null);
            this.handle = handle;
        }

    }

    public static class IndyField extends FieldNode {
        private final InvokeDynamicType indy;

        public IndyField(InvokeDynamicType indy, Type callsiteType, int count) {
            super(Opcodes.ASM9, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "jvmdg$indy$" + indy.indy.name + "$" + count, callsiteType.getDescriptor(), null, null);
            this.indy = indy;
        }

    }

    public static class InvokeDynamicType {
        InvokeDynamicInsnNode indy;

        public InvokeDynamicType(InvokeDynamicInsnNode indy) {
            this.indy = indy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InvokeDynamicType)) return false;
            InvokeDynamicType that = (InvokeDynamicType) o;
            return indy.bsm.equals(that.indy.bsm) &&
                indy.name.equals(that.indy.name) &&
                indy.desc.equals(that.indy.desc) &&
                Arrays.equals(indy.bsmArgs, that.indy.bsmArgs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(indy.bsm, indy.name, indy.desc, Arrays.hashCode(indy.bsmArgs));
        }

    }

}
