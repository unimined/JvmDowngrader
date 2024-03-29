package xyz.wagyourtail.jvmdg.version;

import org.objectweb.asm.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.*;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.all.stub.J_L_Class;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

public abstract class VersionProvider {

    public final Map<Type, Type> classStubs = new HashMap<>();
    public final Map<Type, ClassMapping> stubMappings = new HashMap<>();
    public final int inputVersion;
    public final int outputVersion;

    private volatile boolean initialized = false;

    protected VersionProvider(int inputVersion, int outputVersion) {
        this.inputVersion = inputVersion;
        this.outputVersion = outputVersion;
    }

    public void afterInit() {
        if (Constants.DEBUG) {
            for (Map.Entry<Type, Type> stub : classStubs.entrySet()) {
                System.out.println(stub.getKey().getInternalName() + " -> " + stub.getValue().getInternalName());
            }
            for (Map.Entry<Type, ClassMapping> entry : stubMappings.entrySet()) {
                for (Map.Entry<MemberNameAndDesc, Pair<Method, Stub>> member : entry.getValue().getMethodStubMap().entrySet()) {
                    System.out.println(entry.getKey().getInternalName() + "." + member.getKey().getName() + member.getKey().getDesc() + " -> " + member.getValue().getFirst().getDeclaringClass().getCanonicalName().replace('.', '/') + ";" + member.getValue().getFirst().getName() + Type.getMethodDescriptor(member.getValue().getFirst()));
                }
            }
//            for (Map.Entry<Type, Pair<Type, Stub>> entry : classStubs.entrySet()) {
//                System.out.println(entry.getKey().getInternalName() + " -> " + entry.getValue().getFirst().getInternalName());
//            }
//            for (Map.Entry<String, Pair<Method, Stub>> entry : methodStubs.entrySet()) {
//                System.out.println(entry.getKey() + " -> " + entry.getValue().getFirst().getDeclaringClass().getCanonicalName().replace('.', '/') + ";" + entry.getValue().getFirst().getName() + Type.getMethodDescriptor(entry.getValue()
//                    .getFirst()));
//            }
        }
    }

    public void preInit() {
        // apply all version stubs
        stub(J_L_Class.class);

    }

    public abstract void init();

    public static void main(String[] args) {
        System.out.println(Type.getType(boolean.class).getDescriptor());
    }

    public synchronized ClassMapping getStubMapper(Type type) throws IOException {
        return getStubMapper(type, new IOFunction<Type, Set<MemberNameAndDesc>>() {

            @Override
            public Set<MemberNameAndDesc> apply(Type o) throws IOException {
                return ClassDowngrader.currentVersionDowngrader.getMembers(inputVersion, o);
            }

        });
    }

    public synchronized ClassMapping getStubMapper(Type type, final IOFunction<Type, Set<MemberNameAndDesc>> memberResolver) throws IOException {
        return getStubMapper(type, memberResolver, new IOFunction<Type, List<Type>>() {

            @Override
            public List<Type> apply(Type o) throws IOException {
                return ClassDowngrader.currentVersionDowngrader.getSupertypes(inputVersion, o);
            }

        });
    }

    public synchronized ClassMapping getStubMapper(final Type type, final IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, final IOFunction<Type, List<Type>> superTypeResolver) throws IOException {
        if (stubMappings.containsKey(type)) {
            return stubMappings.get(type);
        }
        if (type.getSort() == Type.ARRAY) {
            return new ClassMapping(new Lazy<List<ClassMapping>>() {
                @Override
                public List<ClassMapping> init() {
                     try {
                         return Collections.singletonList(getStubMapper(Type.getObjectType("java/lang/Object"), memberResolver, superTypeResolver));
                     } catch (IOException e) {
                         throw new RuntimeException(e);
                     }
                 }
            }, type, memberResolver, this);
        }
        if (type.getInternalName().equals("java/lang/Object")) {
            ClassMapping mapping = new ClassMapping(new Lazy <List<ClassMapping>>() {
                @Override
                public List<ClassMapping> init() {
                    return Collections.emptyList();
                }
            }, type, memberResolver, this);
            stubMappings.put(type, mapping);
            return mapping;
        }
        ClassMapping mapping = new ClassMapping(new Lazy<List<ClassMapping>>() {
            @Override
            public List<ClassMapping> init() {
                try {
                    List<Type> types = superTypeResolver.apply(type);
                    if (types == null) {
//                        System.err.println(VersionProvider.this.getClass().getName() + " Could not find class " + type.getInternalName());
                        types = Collections.emptyList();
                    }
                    List<ClassMapping> superTypes = new ArrayList<>();
                    for (Type superType : types) {
                        superTypes.add(getStubMapper(superType, memberResolver, superTypeResolver));
                    }
                    return superTypes;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, type, memberResolver, this);
        stubMappings.put(type, mapping);
        return mapping;
    }

    public void stub(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Stub.class)) {
            Stub stub = clazz.getAnnotation(Stub.class);
            if (stub.ref().value().isEmpty()) {
                throw new IllegalArgumentException("Class " + clazz.getName() + ", @Stub must have a ref");
            } else {
                Type type;
                if (stub.ref().value().startsWith("L") && stub.ref().value().endsWith(";")) {
                    type = Type.getType(stub.ref().value());
                } else {
                    type = Type.getObjectType(stub.ref().value());
                }
//                if (classStubs.containsKey(type)) {
//                    throw new IllegalArgumentException("Class " + clazz.getName() + ", @Stub ref " + type.getInternalName() + " already exists");
//                }
                classStubs.put(type, Type.getType(clazz));
            }
        }
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Stub.class)) {
                    Stub stub = method.getAnnotation(Stub.class);
                    FullyQualifiedMemberNameAndDesc target = resolveStubTarget(method, stub.ref());
                    Type owner = target.getOwner();
                    MemberNameAndDesc member = target.toMemberNameAndDesc();
                    getStubMapper(owner).addStub(member, method, stub);
                } else if (method.isAnnotationPresent(Modify.class)) {
                    Modify modify = method.getAnnotation(Modify.class);
                    FullyQualifiedMemberNameAndDesc target = resolveModifyTarget(method, modify.ref());
                    Type owner = target.getOwner();
                    MemberNameAndDesc member = target.toMemberNameAndDesc();
                    // ensure method parameters are valid
                    Class<?>[] params = method.getParameterTypes();
                    for (int i = 0; i < params.length; i++) {
                        if (i >= Modify.MODIFY_SIG.length) {
                            throw new IllegalArgumentException("Class " + clazz.getName() + ", @Modify method " + method.getName() + " has too many parameters");
                        }
                        if (params[i] != Modify.MODIFY_SIG[i]) {
                            throw new IllegalArgumentException("Class " + clazz.getName() + ", @Modify method " + method.getName() + " parameter " + i + " must be of type " + Modify.MODIFY_SIG[i].getName());
                        }
                    }
                    getStubMapper(owner).addModify(member, method, modify);
                }
            }
        } catch (Throwable e) {
//            throw new RuntimeException("failed to create stub " + clazz.getName(), e);
//            System.out.println("ERROR: failed to create stub for " + clazz.getName() + " (" + e.getMessage().split("\n")[0] + ")");
//            e.printStackTrace(System.err);
        }
        try {
            // inner classes
            for (Class<?> inner : clazz.getDeclaredClasses()) {
                stub(inner);
            }
        } catch (Throwable e) {
//            throw new RuntimeException("failed to create stub for inner classes of " + clazz.getName(), e);
//            System.out.println("ERROR: failed to create stub for inner classes of " + clazz.getName() + " (" + e.getMessage().split("\n")[0] + ")");
        }
    }

    public Type stubClass(Type desc) {
        switch (desc.getSort()) {
            case Type.METHOD:
                Type[] args = desc.getArgumentTypes();
                Type ret = desc.getReturnType();
                for (int i = 0; i < args.length; i++) {
                    args[i] = stubClass(args[i]);
                }
                ret = stubClass(ret);
                return Type.getMethodType(ret, args);
            case Type.ARRAY:
                Type type = desc.getElementType();
                if (classStubs.containsKey(type)) {
                    type = classStubs.get(type);
                }
                return Type.getType(desc.getDescriptor().substring(0, desc.getDimensions()) + type.getDescriptor());
            case Type.OBJECT:
                if (classStubs.containsKey(desc)) {
                    return classStubs.get(desc);
                }
                return desc;
            default:
                return desc;
        }
    }

    public ClassNode stubMethods(ClassNode owner, Set<ClassNode> extra, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Type>> superTypeResolver) throws IOException {
        for (MethodNode method : new ArrayList<>(owner.methods)) {
            MethodNode newMethod = stubMethods(method, owner, extra, enableRuntime, memberResolver, superTypeResolver);
            if (newMethod != method) {
                owner.methods.set(owner.methods.indexOf(method), newMethod);
            }
        }
        return owner;
    }

    public MethodNode stubMethods(MethodNode method, ClassNode owner, Set<ClassNode> extra, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Type>> superTypeResolver) throws IOException {
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode insn = method.instructions.get(i);
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) insn;
                min.owner = stubClass(Type.getObjectType(min.owner)).getInternalName();
                min.desc = stubClass(Type.getMethodType(min.desc)).getDescriptor();
                getStubMapper(Type.getObjectType(min.owner), memberResolver, superTypeResolver).transform(method, i, owner, extra, enableRuntime);
            } else if (insn instanceof TypeInsnNode) {
                TypeInsnNode tin = (TypeInsnNode) insn;
                tin.desc = stubClass(Type.getObjectType(tin.desc)).getInternalName();
            } else if (insn instanceof FieldInsnNode) {
                FieldInsnNode fin = (FieldInsnNode) insn;
                fin.owner = stubClass(Type.getObjectType(fin.owner)).getInternalName();
                fin.desc = stubClass(Type.getType(fin.desc)).getDescriptor();
                //TODO: field stubs (upgrade to method)
            } else if (insn instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                indy.desc = stubClass(Type.getMethodType(indy.desc)).getDescriptor();
                indy.bsm = new Handle(
                        indy.bsm.getTag(),
                        stubClass(Type.getObjectType(indy.bsm.getOwner())).getInternalName(),
                        indy.bsm.getName(),
                        stubClass(Type.getMethodType(indy.bsm.getDesc())).getDescriptor(),
                        indy.bsm.isInterface()
                );
                for (int j = 0; j < indy.bsmArgs.length; j++) {
                    Object arg = indy.bsmArgs[j];
                    if (arg instanceof Handle) {
                        Handle handle = (Handle) arg;
                        handle = new Handle(
                                handle.getTag(),
                                stubClass(Type.getObjectType(handle.getOwner())).getInternalName(),
                                handle.getName(),
                                stubClass(Type.getType(handle.getDesc())).getDescriptor(),
                                handle.isInterface()
                        );
                        indy.bsmArgs[j] = handle;
                        switch (handle.getTag()) {
                            case Opcodes.H_GETFIELD:
                            case Opcodes.H_GETSTATIC:
                            case Opcodes.H_PUTFIELD:
                            case Opcodes.H_PUTSTATIC:
                                //TODO
                                break;
                            case Opcodes.H_INVOKEVIRTUAL:
                            case Opcodes.H_INVOKESTATIC:
                            case Opcodes.H_INVOKESPECIAL:
                            case Opcodes.H_NEWINVOKESPECIAL:
                            case Opcodes.H_INVOKEINTERFACE:
                                Type[] captured = null;
                                if (indy.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
                                    captured = Type.getMethodType(indy.desc).getArgumentTypes();
                                }
                                Type hOwner = Type.getObjectType(handle.getOwner());
                                Type hDesc = Type.getMethodType(handle.getDesc());
                                MemberNameAndDesc member = new MemberNameAndDesc(handle.getName(), hDesc);
                                ClassMapping stubMapper = getStubMapper(hOwner, memberResolver, superTypeResolver);
                                Pair<Method, Stub> min = stubMapper.getStubFor(member, handle.getTag() == Opcodes.H_INVOKESTATIC, enableRuntime);
                                if (min != null) {
                                    if (min.getSecond().downgradeVersion()) {
                                        System.err.println("Invalid stub for indy handle: " + handle.getOwner() + "." + handle.getName() + handle.getDesc());
                                    } else if (!min.getSecond().abstractDefault()) {
                                        Type hStaticDesc;
                                        if (handle.getTag() == Opcodes.H_INVOKESTATIC) {
                                            hStaticDesc = hDesc;
                                        } else {
                                            Type[] params = new Type[hDesc.getArgumentCount() + 1];
                                            params[0] = hOwner;
                                            System.arraycopy(hDesc.getArgumentTypes(), 0, params, 1, hDesc.getArgumentCount());
                                            if (captured != null) {
                                                // replace actual with capture, fixes invokeInterface on LambdaMetafactory
                                                System.arraycopy(captured, 0, params, 0, captured.length);
                                            }
                                            hStaticDesc = Type.getMethodType(hDesc.getReturnType(), params);
                                        }

                                        Method m = min.getFirst();
                                        String newOwner = Type.getType(m.getDeclaringClass()).getInternalName();
                                        String name = m.getName();
                                        String desc = Type.getMethodDescriptor(m);
                                        if (!desc.equals(hStaticDesc.getDescriptor())) {
                                            // create wrapper as desc should exactly match.
                                            newOwner = owner.name;
                                            name = "jvmdowngrader$call$" + name;
                                            desc = hStaticDesc.getDescriptor();
                                            boolean found = false;
                                            for (MethodNode mn : owner.methods) {
                                                if (mn.name.equals(name)) {
                                                    found = true;
                                                    break;
                                                }
                                            }
                                            // else
                                            if (!found) {
                                                MethodVisitor mv = owner.visitMethod(Constants.synthetic(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC), name, hStaticDesc.getDescriptor(), null, null);
                                                mv.visitCode();
                                                Type returnType = hStaticDesc.getReturnType();
                                                Type[] arguments = hStaticDesc.getArgumentTypes();
                                                Type actualReturnType = Type.getType(m.getReturnType());
                                                int k = 0;
                                                for (Type argument : arguments) {
                                                    mv.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), k);
                                                    k += argument.getSize();
                                                }
                                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getType(m.getDeclaringClass()).getInternalName(), m.getName(), Type.getMethodDescriptor(m),false);
                                                if (!actualReturnType.equals(returnType)) {
                                                    mv.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
                                                }
                                                mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));

                                                mv.visitMaxs(0, 0);
                                                mv.visitEnd();
                                            }
                                        }
                                        indy.bsmArgs[j] = new Handle(
                                                Opcodes.H_INVOKESTATIC,
                                                newOwner,
                                                name,
                                                desc,
                                                false
                                        );
                                    }
                                }
                                break;
                        }

                    } else if (arg instanceof Type) {
                        Type type = (Type) arg;
                        indy.bsmArgs[j] = stubClass(type);
                    }
                }
                getStubMapper(Type.getObjectType(indy.bsm.getOwner()), memberResolver, superTypeResolver).transform(method, i, owner, extra, enableRuntime);
            } else if (insn instanceof MultiANewArrayInsnNode) {
                MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) insn;
                manain.desc = stubClass(Type.getType(manain.desc)).getDescriptor();
            } else if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (ldc.cst instanceof Type) {
                    ldc.cst = stubClass((Type) ldc.cst);
                }
            }
        }
        return method;
    }

    public static FullyQualifiedMemberNameAndDesc resolveStubTarget(Member member, Ref ref) {
        if (member instanceof Method) {
            Method method = (Method) member;
            Type owner;
            String name;
            List<Type> params = new ArrayList<>(Arrays.asList(Type.getArgumentTypes(method)));
            Type ret = Type.getReturnType(method);
            if (ref.value().isEmpty()) {
                owner = params.remove(0);
            } else {
                if (ref.value().startsWith("L") && ref.value().endsWith(";")) {
                    owner = Type.getType(ref.value());
                } else {
                    owner = Type.getObjectType(ref.value());
                }
            }
            if (ref.member().isEmpty()) {
                name = method.getName();
            } else {
                name = ref.member();
            }
            Type desc;
            if (ref.desc().isEmpty()) {
                if (name.equals("<init>")) {
                    ret = Type.VOID_TYPE;
                }
                desc = Type.getMethodType(ret, params.toArray(new Type[0]));
            } else {
                desc = Type.getMethodType(ref.desc());
            }
            // re-assemble desc
            return new FullyQualifiedMemberNameAndDesc(owner, name, desc);
        } else if (member instanceof Field) {
//            Field field = (Field) member;
            throw new UnsupportedOperationException("Not implemented yet");
        } else {
            throw new IllegalArgumentException("member must be a method or field");
        }
    }

    public static FullyQualifiedMemberNameAndDesc resolveModifyTarget(Member member, Ref ref) {
        if (member instanceof Method) {
            Method method = (Method) member;
            Type owner;
            String name;
            Type desc;
            if (ref.value().isEmpty()) {
                throw new IllegalArgumentException("ref must have a value");
            } else {
                if (ref.value().startsWith("L") && ref.value().endsWith(";")) {
                    owner = Type.getType(ref.value());
                } else {
                    owner = Type.getObjectType(ref.value());
                }
            }
            if (ref.member().isEmpty()) {
                throw new IllegalArgumentException("ref must have a member");
            } else {
                name = ref.member();
            }
            if (ref.desc().isEmpty()) {
                throw new IllegalArgumentException("ref must have a desc");
            } else {
                desc = Type.getMethodType(ref.desc());
            }
            return new FullyQualifiedMemberNameAndDesc(owner, name, desc);
        } else {
            throw new IllegalArgumentException("member must be a method");
        }
    }

    public void ensureInit() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    preInit();
                    init();
                    afterInit();
                    initialized = true;
                }
            }
        }
    }

    public ClassNode downgrade(ClassNode clazz, Set<ClassNode> extra, boolean enableRuntime, final Function<String, ClassNode> getReadOnly) throws InvocationTargetException, IllegalAccessException, IOException {
        if (clazz.version != inputVersion)
            throw new IllegalArgumentException("Class " + clazz.name + " is not version " + inputVersion);

        ensureInit();

        IOFunction<Type, Set<MemberNameAndDesc>> getMembers = new IOFunction<Type, Set<MemberNameAndDesc>>() {
            @Override
            public Set<MemberNameAndDesc> apply(Type o) throws IOException {
                Set<MemberNameAndDesc> members = ClassDowngrader.currentVersionDowngrader.getMembers(inputVersion, o);
                // if not found in the classloader, check getReadOnly for it. this should really only happen with the ZipDowngrader
                if (members == null) {
                    ClassNode ro = getReadOnly.apply(o.getInternalName());
                    if (ro != null) {
                        members = new HashSet<>();
                        for (MethodNode method : ro.methods) {
                            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) != 0) continue;
                            members.add(new MemberNameAndDesc(method.name, Type.getMethodType(method.desc)));
                        }
                    }
                }
                return members;
            }
        };

        IOFunction<Type, List<Type>> getSuperTypes = new IOFunction<Type, List<Type>>() {

            @Override
            public List<Type> apply(Type o) throws IOException {
                List<Type> types = ClassDowngrader.currentVersionDowngrader.getSupertypes(inputVersion, o);
                // if not found in the classloader, check getReadOnly for it. this should really only happen with the ZipDowngrader
                if (types == null) {
                    ClassNode ro = getReadOnly.apply(o.getInternalName());
                    if (ro != null) {
                        types = new ArrayList<>();
                        types.add(Type.getObjectType(ro.superName));
                        for (String anInterface : ro.interfaces) {
                            types.add(Type.getObjectType(anInterface));
                        }
                    }
                }
                return types;
            }
        };

        clazz = stubClasses(clazz, enableRuntime);
        clazz = stubMethods(clazz, extra, enableRuntime, getMembers, getSuperTypes);
        clazz = insertAbstractMethods(clazz, extra, getMembers, getSuperTypes);
        clazz = otherTransforms(clazz, extra, getReadOnly);
        clazz.version = inputVersion - 1;
        return clazz;
    }

    public ClassNode insertAbstractMethods(ClassNode clazz, Set<ClassNode> extra, IOFunction<Type, Set<MemberNameAndDesc>> getMembers, IOFunction<Type, List<Type>> getSuperTypes) throws IOException {
        Map<MemberNameAndDesc, Pair<Method, Stub>> members = getStubMapper(Type.getObjectType(clazz.name), getMembers, getSuperTypes).getAbstracts();
        for (Map.Entry<MemberNameAndDesc, Pair<Method, Stub>> member : members.entrySet()) {
            boolean contains = false;
            for (MethodNode method : clazz.methods) {
                if (method.name.equals(member.getKey().getName()) && method.desc.equals(member.getKey().getDesc().getDescriptor())) {
                    contains = true;
                    break;
                }
            }
            if (contains) continue;
            MethodVisitor mv = clazz.visitMethod(Opcodes.ACC_PUBLIC, member.getKey().getName(), member.getKey().getDesc().getDescriptor(), null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            Type[] params = member.getKey().getDesc().getArgumentTypes();
            int i = 1;
            for (Type param : params) {
                mv.visitVarInsn(param.getOpcode(Opcodes.ILOAD), i);
                i += param.getSize();
            }
            if (member.getValue().getSecond().downgradeVersion()) {
                mv.visitLdcInsn(this.inputVersion);
            }
            Class<?> declaring = member.getValue().getFirst().getDeclaringClass();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getType(declaring).getInternalName(), member.getValue().getFirst().getName(), Type.getMethodDescriptor(member.getValue().getFirst()), declaring.isInterface());
            Type returnType = member.getKey().getDesc().getReturnType();
            mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        return clazz;
    }

    public ClassNode otherTransforms(ClassNode clazz, Set<ClassNode> extra, Function<String, ClassNode> getReadOnly) {
        clazz = otherTransforms(clazz, extra);
        return clazz;
    }

    public ClassNode otherTransforms(ClassNode clazz, Set<ClassNode> extra) {
        clazz = otherTransforms(clazz);
        return clazz;
    }

    public ClassNode otherTransforms(ClassNode clazz) {
        return clazz;
    }

    public ClassNode stubClasses(ClassNode clazz, boolean enableRuntime) {
        // super
        Type type = Type.getObjectType(clazz.superName);
        if (classStubs.containsKey(type)) {
            clazz.superName = classStubs.get(type).getInternalName();
        }

        // interface
        if (clazz.interfaces != null) {
            for (int i = 0; i < clazz.interfaces.size(); i++) {
                type = Type.getObjectType(clazz.interfaces.get(i));
                if (classStubs.containsKey(type)) {
                    clazz.interfaces.set(i, classStubs.get(type).getInternalName());
                }
            }
        }

        // signature
        if (clazz.signature != null) {
            clazz.signature = transformSignature(clazz.signature);
        }

        // field descriptor
        if (clazz.fields != null) {
            for (FieldNode field : clazz.fields) {
                type = Type.getType(field.desc);
                field.desc = stubClass(type).getDescriptor();
                if (field.signature != null) {
                    field.signature = transformSignature(field.signature);
                }
            }
        }

        // method descriptor
        if (clazz.methods != null) {
            for (MethodNode method : clazz.methods) {
                type = Type.getMethodType(method.desc);
                method.desc = stubClass(type).getDescriptor();
                if (method.signature != null) {
                    method.signature = transformSignature(method.signature);
                }
                if (method.localVariables != null) {
                    for (LocalVariableNode local : method.localVariables) {
                        type = Type.getType(local.desc);
                        local.desc = stubClass(type).getDescriptor();
                        if (local.signature != null) {
                            local.signature = transformSignature(local.signature);
                        }
                    }
                }
                if (method.tryCatchBlocks != null) {
                    for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) {
                        if (tryCatch.type != null) {
                            type = Type.getObjectType(tryCatch.type);
                            tryCatch.type = stubClass(type).getInternalName();
                        }
                    }
                }
            }
        }
        return clazz;
    }

    public String transformSignature(String signature) {
        SignatureReader reader = new SignatureReader(signature);
        SignatureWriter writer = new SignatureWriter() {
            @Override
            public void visitClassType(String name) {
                super.visitClassType(stubClass(Type.getObjectType(name)).getInternalName());
            }

            @Override
            public void visitInnerClassType(String name) {
                // TODO: fix to support this
                super.visitInnerClassType(name);
            }
        };
        reader.accept(writer);
        return writer.toString();
    }

}
