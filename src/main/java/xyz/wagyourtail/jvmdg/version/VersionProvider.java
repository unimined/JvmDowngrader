package xyz.wagyourtail.jvmdg.version;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.all.RemovedInterfaces;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.IOFunction;
import xyz.wagyourtail.jvmdg.util.Lazy;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.all.stub.J_L_Class;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;

public abstract class VersionProvider {
    public final Map<Type, Pair<Type, Pair<Class<?>, Adapter>>> classStubs = new LinkedHashMap<>();
    public final Map<Type, ClassMapping> stubMappings = new LinkedHashMap<>();
    public final int inputVersion;
    public final int outputVersion;

    public final Coverage coverage;

    public final int priotity;

    /**
     * lateinit
     * bound during ensureInit
     */
    protected ClassDowngrader downgrader;
    protected Logger logger;

    private volatile boolean initialized = false;

    /**
     * will be made package-private in a future release
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "1.2.0")
    protected VersionProvider(int inputVersion, int outputVersion) {
        this.inputVersion = inputVersion;
        this.outputVersion = outputVersion;
        this.coverage = new Coverage(inputVersion, this);
        this.priotity = 0;
    }

    protected VersionProvider(int inputVersion, int outputVersion, int priotity) {
        this.inputVersion = inputVersion;
        this.outputVersion = outputVersion;
        this.coverage = new Coverage(inputVersion, this);
        this.priotity = priotity;
    }

    public FullyQualifiedMemberNameAndDesc resolveStubTarget(Member member, Ref ref) {
        if (member instanceof Method) {
            Method method = (Method) member;
            Type owner;
            String name;
            List<Type> params = new ArrayList<>(Arrays.asList(Type.getArgumentTypes(method)));

            Annotation[][] annotations = method.getParameterAnnotations();
            for (int i = 0; i < params.size(); i++) {
                Annotation[] param = annotations[i];
                for (Annotation a : param) {
                    if (a instanceof Coerce) {
                        Coerce c = (Coerce) a;
                        params.set(i, Type.getType(c.value()));
                    }
                }
            }

            Class<?>[] paramClasses = method.getParameterTypes();
            for (int i = 0; i < params.size(); i++) {
                Adapter a = paramClasses[i].getAnnotation(Adapter.class);
                if (a != null) {
                    String aRef = a.value();
                    Type t;
                    if (aRef.startsWith("L") && aRef.endsWith(";")) {
                        t = Type.getType(aRef);
                    } else {
                        t = Type.getObjectType(aRef);
                    }

                    for (VersionProvider versionProvider : downgrader.versionProviders(this.outputVersion)) {
                        if (versionProvider.classStubs.containsKey(t)) {
                            params.set(i, t);
                            break;
                        }
                    }
                }
            }

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
                desc = Type.getMethodType(ret, params.toArray(new Type[0]));
            } else {
                desc = Type.getMethodType(ref.desc());
            }
            if (name.equals("<init>")) {
                // due to not being able to pass UNINITIALIZED_THIS
                throw new IllegalArgumentException(owner.getDescriptor() + "<init>;" + desc + " shouldn't be @Stub, should be @Modify");
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

    public void afterInit() {
        if (logger.is(Logger.Level.DEBUG)) {
            StringBuilder sb = new StringBuilder("Stubs: \n");
            for (Map.Entry<Type, Pair<Type, Pair<Class<?>, Adapter>>> stub : classStubs.entrySet()) {
                sb.append(stub.getKey().getInternalName()).append(" -> ").append(stub.getValue().getFirst()).append("\n");
            }
            for (Map.Entry<Type, ClassMapping> entry : stubMappings.entrySet()) {
                for (Map.Entry<MemberNameAndDesc, Pair<Method, Stub>> member : entry.getValue().getMethodStubMap().entrySet()) {
                    sb.append(entry.getKey().getInternalName())
                        .append(".").append(member.getKey().getName())
                        .append(member.getKey().getDesc()).append(" -> ")
                        .append(member.getValue().getFirst().getDeclaringClass().getCanonicalName().replace('.', '/'))
                        .append(";")
                        .append(member.getValue().getFirst().getName())
                        .append(Type.getMethodDescriptor(member.getValue().getFirst()))
                        .append("\n");
                }
            }
            logger.debug(sb.toString());
        }
    }

    public void preInit() {
        // apply all version stubs
        stub(J_L_Class.class);

    }

    public abstract void init();

    public synchronized ClassMapping getStubMapper(Type type, Set<String> warnings) throws IOException {
        return getStubMapper(type, downgrader.isInterface(outputVersion, type, warnings) == Boolean.TRUE, warnings);
    }

    public synchronized ClassMapping getStubMapper(Type type, boolean isInterface, final Set<String> warnings) throws IOException {
        return getStubMapper(type, isInterface, new IOFunction<Type, Set<MemberNameAndDesc>>() {

            @Override
            public Set<MemberNameAndDesc> apply(Type o) throws IOException {
                return downgrader.getMembers(inputVersion, o, warnings);
            }

        }, warnings);
    }

    public synchronized ClassMapping getStubMapper(Type type, final boolean isInterface, final IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, final Set<String> warnings) throws IOException {
        return getStubMapper(type, isInterface, memberResolver, new IOFunction<Type, List<Pair<Type, Boolean>>>() {

            @Override
            public List<Pair<Type, Boolean>> apply(Type o) throws IOException {
                return downgrader.getSupertypes(inputVersion, o, warnings);
            }

        });
    }

    public synchronized ClassMapping getStubMapper(final Type type, final boolean isInterface, final IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, final IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        if (stubMappings.containsKey(type)) {
            return stubMappings.get(type);
        }
        if (type.getSort() == Type.ARRAY) {
            return new ClassMapping(coverage, new Lazy<List<ClassMapping>>() {
                @Override
                public List<ClassMapping> init() {
                    try {
                        return Collections.singletonList(getStubMapper(Type.getObjectType("java/lang/Object"), false, memberResolver, superTypeResolver));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, type, isInterface, memberResolver, this);
        }
        if (type.getInternalName().equals("java/lang/Object")) {
            ClassMapping mapping = new ClassMapping(coverage, new Lazy<List<ClassMapping>>() {
                @Override
                public List<ClassMapping> init() {
                    return Collections.emptyList();
                }
            }, type, false, memberResolver, this);
            stubMappings.put(type, mapping);
            return mapping;
        }
        ClassMapping mapping = new ClassMapping(coverage, new Lazy<List<ClassMapping>>() {
            @Override
            public List<ClassMapping> init() {
                try {
                    List<Pair<Type, Boolean>> types = superTypeResolver.apply(type);
                    if (types == null) {
                        logger.error("Could not find class " + type.getInternalName());
                        types = Collections.emptyList();
                    }
                    List<ClassMapping> superTypes = new ArrayList<>();
                    for (Pair<Type, Boolean> superType : types) {
                        superTypes.add(getStubMapper(superType.getFirst(), superType.getSecond(), memberResolver, superTypeResolver));
                    }
                    return superTypes;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, type, isInterface, memberResolver, this);
        stubMappings.put(type, mapping);
        return mapping;
    }

    public void stub(Class<?> clazz) {
        Set<String> warnings = new LinkedHashSet<>();
        try {
            if (clazz.isAnnotationPresent(Adapter.class)) {
                Adapter stub = clazz.getAnnotation(Adapter.class);
                if (stub.value().isEmpty()) {
                    throw new IllegalArgumentException("Class " + clazz.getName() + ", @Adapter must have a ref");
                } else {
                    Type value;
                    if (stub.value().startsWith("L") && stub.value().endsWith(";")) {
                        value = Type.getType(stub.value());
                    } else {
                        value = Type.getObjectType(stub.value());
                    }
//                if (classStubs.containsKey(type)) {
//                    throw new IllegalArgumentException("Class " + clazz.getName() + ", @Adapter ref " + type.getInternalName() + " already exists");
//                }
                    Type target;
                    if (stub.target().isEmpty()) {
                        target = Type.getType(clazz);
                    } else {
                        if (stub.target().startsWith("L") && stub.target().endsWith(";")) {
                            target = Type.getType(stub.target());
                        } else {
                            target = Type.getObjectType(stub.target());
                        }
                    }
                    classStubs.put(value, new Pair<>(target, new Pair<Class<?>, Adapter>(clazz, stub)));
                }
            }
            try {
                for (Method method : clazz.getDeclaredMethods()) {
                    try {
                        if (method.isAnnotationPresent(Stub.class)) {
                            Stub stub = method.getAnnotation(Stub.class);
                            FullyQualifiedMemberNameAndDesc target = resolveStubTarget(method, stub.ref());
                            Type owner = target.getOwner();
                            MemberNameAndDesc member = target.toMemberNameAndDesc();
                            getStubMapper(owner, warnings).addStub(member, method, stub);
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
                            getStubMapper(owner, warnings).addModify(member, method, modify);
                        }
                    } catch (Throwable e) {
                        logger.warn("failed to create stub for " + clazz.getName(), e);
                    }
                }
            } catch (Throwable e) {
                logger.warn("failed to resolve methods for " + clazz.getName(), e);
            }
            try {
                // inner classes
                for (Class<?> inner : clazz.getDeclaredClasses()) {
                    stub(inner);
                }
            } catch (Throwable e) {
                logger.warn("failed to resolve inner classes for " + clazz.getName(), e);
            }
        } catch (Throwable e) {
            logger.warn("failed to create stub(s) for " + clazz.getName(), e);
        }
        if (!warnings.isEmpty() && logger.is(Logger.Level.WARN)) {
            if (downgrader.flags.checkInIgnoreWarnings(clazz.getName())) return;
            StringBuilder sb = new StringBuilder();
            for (String warning : warnings) {
                sb.append("    ").append(warning).append("\n");
            }
            logger.warn("Warnings for " + clazz.getName() + " (" + warnings.size() + ") : \n" + sb);
        }
    }

    public MethodInsnNode stubTypeInsnNode(TypeInsnNode insn, Set<String> warnings) {
        Type desc = Type.getObjectType(insn.desc);
        switch (desc.getSort()) {
            case Type.ARRAY:
                Type type = desc.getElementType();
                if (classStubs.containsKey(type)) {
                    type = classStubs.get(type).getFirst();
                }
                coverage.warnClass(type, warnings);
                insn.desc = Type.getType(desc.getDescriptor().substring(0, desc.getDimensions()) + type.getDescriptor()).getInternalName();
                return null;
            case Type.OBJECT:
                Type t = Type.getObjectType(insn.desc);
                if (classStubs.containsKey(t)) {
                    Pair<Type, Pair<Class<?>, Adapter>> stub = classStubs.get(Type.getObjectType(insn.desc));
                    // check if clazz has method `jvmdg$opcode
                    switch (insn.getOpcode()) {
                        case Opcodes.INSTANCEOF:
                            for (Method declaredMethod : stub.getSecond().getFirst().getDeclaredMethods()) {
                                if (declaredMethod.getName().equals("jvmdg$instanceof")) {
                                    return new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getType(stub.getSecond().getFirst()).getInternalName(), "jvmdg$instanceof", Type.getMethodDescriptor(declaredMethod), false);
                                }
                            }
                            break;
                        case Opcodes.CHECKCAST:
                            for (Method declaredMethod : stub.getSecond().getFirst().getDeclaredMethods()) {
                                if (declaredMethod.getName().equals("jvmdg$checkcast")) {
                                    return new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getType(stub.getSecond().getFirst()).getInternalName(), "jvmdg$checkcast", Type.getMethodDescriptor(declaredMethod), false);
                                }
                            }
                            break;
                        default:
                    }
                    insn.desc = stub.getFirst().getInternalName();
                    return null;
                }
                coverage.warnClass(t, warnings);
                break;
        }
        return null;
    }

    public void stubBSMArgs(ClassNode owner, MethodNode method, Set<ClassNode> extra, Handle bsm, String indyDesc, Object[] bsmArgs, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Set<String> warnings) throws IOException {
        for (int j = 0; j < bsmArgs.length; j++) {
            Object arg = bsmArgs[j];
            if (arg instanceof Handle) {
                arg = stubHandle(owner, method, extra, bsm, indyDesc, enableRuntime, memberResolver, superTypeResolver, warnings, (Handle) arg);
            } else if (arg instanceof Type) {
                arg = stubClass((Type) arg, warnings);
            } else if (arg instanceof ConstantDynamic) {
                arg = stubCondy(owner, method, extra, (ConstantDynamic) arg, enableRuntime, memberResolver, superTypeResolver, warnings);
            }
            bsmArgs[j] = arg;
        }
    }

    private Handle stubHandle(ClassNode owner, MethodNode method, Set<ClassNode> extra, Handle bsm, String indyDesc, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Set<String> warnings, Handle handle) throws IOException {
        handle = new Handle(
            handle.getTag(),
            stubClass(Type.getObjectType(handle.getOwner()), warnings).getInternalName(),
            handle.getName(),
            stubClass(Type.getType(handle.getDesc()), warnings).getDescriptor(),
            handle.isInterface()
        );
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
                if (bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
                    captured = Type.getMethodType(indyDesc).getArgumentTypes();
                }
                Type hOwner = Type.getObjectType(handle.getOwner());
                if (hOwner.getSort() == Type.ARRAY) {
                    return handle;
                }
                Type hDesc = Type.getMethodType(handle.getDesc());
                MemberNameAndDesc member = new MemberNameAndDesc(handle.getName(), hDesc);
                ClassMapping stubMapper = getStubMapper(hOwner, handle.isInterface(), memberResolver, superTypeResolver);
                boolean isStatic = handle.getTag() == Opcodes.H_INVOKESTATIC;
                boolean isSpecial = handle.getTag() == Opcodes.H_INVOKESPECIAL || handle.getTag() == Opcodes.H_NEWINVOKESPECIAL;
                Pair<Method, Stub> min = stubMapper.getStubFor(member, isStatic, enableRuntime, isSpecial, warnings);
                if (min != null) {
                    if (min.getSecond().downgradeVersion()) {
                        warnings.add("Invalid stub for bsm handle: " + handle.getOwner() + "." + handle.getName() + handle.getDesc());
                    } else if (!min.getSecond().abstractDefault()) {
                        Type hStaticDesc;
                        if (isStatic) {
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
                        boolean intf = m.getDeclaringClass().isInterface();
                        if (!desc.equals(hStaticDesc.getDescriptor())) {
                            // create wrapper as desc should exactly match.
                            newOwner = owner.name;
                            desc = hStaticDesc.getDescriptor();
                            intf = (owner.access & Opcodes.ACC_INTERFACE) != 0;
                            MethodNode found = null;
                            int num = 0;
                            for (MethodNode mn : owner.methods) {
                                if (mn instanceof HandleMethodNode) {
                                    Handle h = ((HandleMethodNode) mn).ref;
                                    if (h.getTag() == handle.getTag() &&
                                        h.getOwner().equals(handle.getOwner()) &&
                                        h.getName().equals(handle.getName()) &&
                                        h.getDesc().equals(handle.getDesc()) &&
                                        h.isInterface() == handle.isInterface()) {
                                        if (!mn.desc.equals(hStaticDesc.getDescriptor())) {
                                            num++;
                                        } else {
                                            found = mn;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (found != null) {
                                name = found.name;
                            } else {
                                HandleMethodNode mv = new HandleMethodNode(method.name, handle, num);
                                mv.access = Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC;
                                mv.desc = hStaticDesc.getDescriptor();
                                mv.visitCode();
                                Type returnType = hStaticDesc.getReturnType();
                                Type[] arguments = hStaticDesc.getArgumentTypes();
                                Type actualReturnType = Type.getType(m.getReturnType());
                                int k = 0;
                                for (Type argument : arguments) {
                                    mv.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), k);
                                    k += argument.getSize();
                                }
                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getType(m.getDeclaringClass()).getInternalName(), m.getName(), Type.getMethodDescriptor(m), false);
                                if (!actualReturnType.equals(returnType)) {
                                    mv.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
                                }
                                mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));

                                mv.visitMaxs(0, 0);
                                mv.visitEnd();
                                owner.methods.add(mv);
                                name = mv.name;
                            }
                        }
                        handle = new Handle(
                            Opcodes.H_INVOKESTATIC,
                            newOwner,
                            name,
                            desc,
                            intf
                        );
                    }
                } else {
                    Pair<Method, Modify> mod = stubMapper.getModifyFor(member, isStatic, warnings);
                    if (mod != null) {
                        if (handle.getTag() != Opcodes.H_NEWINVOKESPECIAL) {
                            warnings.add("Invalid modify for indy handle: " + handle.getOwner() + "." + handle.getName() + handle.getDesc());
                        } else {
                            Type returnType = Type.getObjectType(handle.getOwner());
                            Type[] arguments = hDesc.getArgumentTypes();

                            String name;
                            String desc = Type.getMethodDescriptor(returnType, arguments);
                            boolean intf = (owner.access & Opcodes.ACC_INTERFACE) != 0;

                            MethodNode found = null;
                            int num = 0;
                            for (MethodNode mn : owner.methods) {
                                if (mn instanceof HandleMethodNode) {
                                    Handle h = ((HandleMethodNode) mn).ref;
                                    if (h.getTag() == handle.getTag() &&
                                        h.getOwner().equals(handle.getOwner()) &&
                                        h.getName().equals(handle.getName()) &&
                                        h.getDesc().equals(handle.getDesc()) &&
                                        h.isInterface() == handle.isInterface()) {
                                        if (!mn.desc.equals(desc)) {
                                            num++;
                                        } else {
                                            found = mn;
                                            break;
                                        }
                                    }
                                }
//                                                if (mn.name.equals(name) && mn.desc.equals(desc)) {
//                                                    found = true;
//                                                    break;
//                                                }
                            }

                            if (found != null) {
                                name = found.name;
                            } else {
                                // construct wrapper
                                HandleMethodNode mn = new HandleMethodNode(method.name, handle, num);
                                mn.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
                                mn.desc = desc;
                                mn.visitCode();
                                mn.visitTypeInsn(Opcodes.NEW, returnType.getInternalName());
                                mn.visitInsn(Opcodes.DUP);
                                int k = 0;
                                for (Type argument : arguments) {
                                    mn.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), k);
                                    k += argument.getSize();
                                }
                                mn.visitMethodInsn(Opcodes.INVOKESPECIAL, returnType.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, arguments), false);
                                mn.visitInsn(Opcodes.ARETURN);
                                mn.visitMaxs(0, 0);
                                mn.visitEnd();
                                owner.methods.add(mn);

                                // invoke modify
                                try {
                                    List<Object> modifyArgs = Arrays.asList(mn, mn.instructions.size() - 2, owner, extra);
                                    mod.getFirst().invoke(null, modifyArgs.subList(0, mod.getFirst().getParameterTypes().length).toArray());
                                } catch (Throwable e) {
                                    throw new RuntimeException(e);
                                }

                                name = mn.name;
                            }

                            handle = new Handle(
                                Opcodes.H_INVOKESTATIC,
                                owner.name,
                                name,
                                desc,
                                intf
                            );

                        }
                    }
                }
                break;
        }
        return handle;
    }

    public ConstantDynamic stubCondy(ClassNode owner, MethodNode method, Set<ClassNode> extra, ConstantDynamic condy, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Set<String> warnings) throws IOException {
        Handle bsm = condy.getBootstrapMethod();
        Object[] bsmArgs = new Object[condy.getBootstrapMethodArgumentCount()];
        for (int i = 0; i < condy.getBootstrapMethodArgumentCount(); i++) {
            bsmArgs[i] = condy.getBootstrapMethodArgument(i);
        }
        Type desc = stubClass(Type.getType(condy.getDescriptor()), warnings);
        stubBSMArgs(owner, method, extra, bsm, desc.getDescriptor(), bsmArgs, enableRuntime, memberResolver, superTypeResolver, warnings);
        bsm = stubHandle(owner, method, extra, bsm, desc.getDescriptor(), enableRuntime, memberResolver, superTypeResolver, warnings, bsm);
        return new ConstantDynamic(
            condy.getName(),
            desc.getDescriptor(),
            bsm,
            bsmArgs
        );
    }

    public Type stubClass(Type desc, Set<String> warnings) {
        switch (desc.getSort()) {
            case Type.METHOD:
                Type[] args = desc.getArgumentTypes();
                Type ret = desc.getReturnType();
                for (int i = 0; i < args.length; i++) {
                    args[i] = stubClass(args[i], warnings);
                }
                ret = stubClass(ret, warnings);
                return Type.getMethodType(ret, args);
            case Type.ARRAY:
                Type type = desc.getElementType();
                if (classStubs.containsKey(type)) {
                    type = classStubs.get(type).getFirst();
                }
                return Type.getType(desc.getDescriptor().substring(0, desc.getDimensions()) + type.getDescriptor());
            case Type.OBJECT:
                if (classStubs.containsKey(desc)) {
                    return classStubs.get(desc).getFirst();
                }
//                coverage.warnClass(desc, warnings);
                return desc;
            default:
                return desc;
        }
    }

    public ClassNode stubMethods(ClassNode owner, Set<ClassNode> extra, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        if (owner.name.equals("module-info")) {
            return owner;
        }

        for (MethodNode method : new ArrayList<>(owner.methods)) {
            Set<String> warnings = new LinkedHashSet<>();
            MethodNode newMethod = stubMethod(method, owner, extra, enableRuntime, memberResolver, superTypeResolver, warnings);
            if (newMethod != method) {
                owner.methods.set(owner.methods.indexOf(method), newMethod);
            }
            if (!warnings.isEmpty() && logger.is(Logger.Level.WARN)) {
                if (!downgrader.flags.checkInIgnoreWarnings(owner.name + "." + method.name)) {
                    StringBuilder sb = new StringBuilder();
                    for (String warning : warnings) {
                        sb.append("    ").append(warning).append("\n");
                    }
                    logger.warn("Warnings for " + owner.name + "." + method.name + method.desc + " (" + warnings.size() + ") : \n" + sb);
                }
            }
        }
        return owner;
    }

    public MethodNode stubMethod(MethodNode method, ClassNode owner, Set<ClassNode> extra, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Set<String> warnings) throws IOException {
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode insn = method.instructions.get(i);
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) insn;
                min.owner = stubClass(Type.getObjectType(min.owner), warnings).getInternalName();
                min.desc = stubClass(Type.getMethodType(min.desc), warnings).getDescriptor();
                if (!min.owner.startsWith("[")) {
                    getStubMapper(Type.getObjectType(min.owner), min.itf, memberResolver, superTypeResolver).transform(method, i, owner, extra, enableRuntime, warnings);
                }
            } else if (insn instanceof TypeInsnNode) {
                TypeInsnNode tin = (TypeInsnNode) insn;
                MethodInsnNode min = stubTypeInsnNode(tin, warnings);
                if (min != null) {
                    method.instructions.set(tin, min);
                }
            } else if (insn instanceof FieldInsnNode) {
                FieldInsnNode fin = (FieldInsnNode) insn;
                fin.owner = stubClass(Type.getObjectType(fin.owner), warnings).getInternalName();
                fin.desc = stubClass(Type.getType(fin.desc), warnings).getDescriptor();
                //TODO: field stubs (upgrade to method?)
            } else if (insn instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                indy.desc = stubClass(Type.getMethodType(indy.desc), warnings).getDescriptor();
                indy.bsm = new Handle(
                    indy.bsm.getTag(),
                    stubClass(Type.getObjectType(indy.bsm.getOwner()), warnings).getInternalName(),
                    indy.bsm.getName(),
                    stubClass(Type.getMethodType(indy.bsm.getDesc()), warnings).getDescriptor(),
                    indy.bsm.isInterface()
                );
                stubBSMArgs(owner, method, extra, indy.bsm, indy.desc, indy.bsmArgs, enableRuntime, memberResolver, superTypeResolver, warnings);
                getStubMapper(Type.getObjectType(indy.bsm.getOwner()), indy.bsm.isInterface(), memberResolver, superTypeResolver).transform(method, i, owner, extra, enableRuntime, warnings);
            } else if (insn instanceof MultiANewArrayInsnNode) {
                MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) insn;
                manain.desc = stubClass(Type.getType(manain.desc), warnings).getDescriptor();
            } else if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (ldc.cst instanceof Type) {
                    ldc.cst = stubClass((Type) ldc.cst, warnings);
                } else if (ldc.cst instanceof ConstantDynamic) {
                    ConstantDynamic condy = (ConstantDynamic) ldc.cst;
                    ldc.cst = stubCondy(owner, method, extra, condy, enableRuntime, memberResolver, superTypeResolver, warnings);
                }
            } else if (insn instanceof FrameNode) {
                FrameNode fn = (FrameNode) insn;
                if (fn.local != null) {
                    for (int j = 0; j < fn.local.size(); j++) {
                        Object o = fn.local.get(j);
                        if (o instanceof String) {
                            fn.local.set(j, stubClass(Type.getObjectType((String) o), warnings).getInternalName());
                        }
                    }
                }
                if (fn.stack != null) {
                    for (int j = 0; j < fn.stack.size(); j++) {
                        Object o = fn.stack.get(j);
                        if (o instanceof String) {
                            fn.stack.set(j, stubClass(Type.getObjectType((String) o), warnings).getInternalName());
                        }
                    }
                }
            }
        }
        return method;
    }

    public void ensureInit(ClassDowngrader downgrader) {
        this.downgrader = downgrader;
        this.logger = downgrader.logger.subLogger(getClass());
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    if (downgrader.flags.debugSkipStubs.contains(inputVersion)) {
                        initialized = true;
                        return;
                    }
                    preInit();
                    init();
                    afterInit();
                    initialized = true;
                }
            }
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public ClassNode downgrade(final ClassDowngrader downgrader, ClassNode clazz, final Set<ClassNode> extra, final boolean enableRuntime, final Function<String, ClassNode> getReadOnly) throws IOException {
        String className = clazz.name;

        if (clazz.version != inputVersion)
            throw new IllegalArgumentException("Class " + className + " is not version " + inputVersion);

        ensureInit(downgrader);
        final Set<String> warnings = new LinkedHashSet<>();

        clazz.version = outputVersion;

        final IOFunction<Type, Set<MemberNameAndDesc>> getMembers = new IOFunction<Type, Set<MemberNameAndDesc>>() {
            @Override
            public Set<MemberNameAndDesc> apply(Type o) throws IOException {
                Set<MemberNameAndDesc> members = downgrader.getMembers(inputVersion, o, warnings);
                // if not found in the classloader, check getReadOnly for it. this should really only happen with the ZipDowngrader
                if (members == null) {
                    ClassNode ro = getReadOnly.apply(o.getInternalName());
                    if (ro != null) {
                        members = new HashSet<>();
                        for (MethodNode method : ro.methods) {
                            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) != 0) continue;
                            members.add(new MemberNameAndDesc(method.name, downgrader.stubClass(ro.version, Type.getMethodType(method.desc), warnings)));
                        }
                    }
                }
                // if not found in read-only, check extra
                if (members == null) {
                    for (ClassNode extraClass : extra) {
                        if (extraClass.name.equals(o.getInternalName())) {
                            members = new HashSet<>();
                            for (MethodNode method : extraClass.methods) {
                                if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) != 0) continue;
                                members.add(new MemberNameAndDesc(method.name, downgrader.stubClass(extraClass.version, Type.getMethodType(method.desc), warnings)));
                            }
                        }
                    }
                }
                return members;
            }
        };

        final IOFunction<Type, List<Pair<Type, Boolean>>> getSuperTypes = new IOFunction<Type, List<Pair<Type, Boolean>>>() {

            @Override
            public List<Pair<Type, Boolean>> apply(Type o) throws IOException {
                List<Pair<Type, Boolean>> types = downgrader.getSupertypes(inputVersion, o, warnings);
                // if not found in the classloader, check getReadOnly for it. this should really only happen with the ZipDowngrader
                if (types == null) {
                    ClassNode ro = getReadOnly.apply(o.getInternalName());
                    if (ro != null) {
                        types = new ArrayList<>();
                        types.add(new Pair<>(downgrader.stubClass(ro.version, Type.getObjectType(ro.superName), warnings), Boolean.FALSE));
                        for (String anInterface : ro.interfaces) {
                            types.add(new Pair<>(downgrader.stubClass(ro.version, Type.getObjectType(anInterface), warnings), Boolean.TRUE));
                        }
                    }
                }
                // if not found in read-only, check extra
                if (types == null) {
                    for (ClassNode extraClass : extra) {
                        if (extraClass.name.equals(o.getInternalName())) {
                            types = new ArrayList<>();
                            types.add(new Pair<>(downgrader.stubClass(extraClass.version, Type.getObjectType(extraClass.superName), warnings), Boolean.FALSE));
                            for (String anInterface : extraClass.interfaces) {
                                types.add(new Pair<>(downgrader.stubClass(extraClass.version, Type.getObjectType(anInterface), warnings), Boolean.TRUE));
                            }
                        }
                    }
                }
                return types;
            }
        };

        clazz = stubClasses(clazz, enableRuntime, warnings);
        if (clazz == null) {
            printWarnings(warnings, className);
            return null;
        }
        clazz = stubWithExtras(clazz, extra, new IOFunction<ClassNode, ClassNode>() {
            @Override
            public ClassNode apply(ClassNode classNode) throws IOException {
                return stubMethods(classNode, extra, enableRuntime, getMembers, getSuperTypes);
            }
        });
        if (clazz == null) {
            printWarnings(warnings, className);
            return null;
        }
        clazz = stubWithExtras(clazz, extra, new IOFunction<ClassNode, ClassNode>() {
            @Override
            public ClassNode apply(ClassNode classNode) throws IOException {
                return insertAbstractMethods(classNode, extra, getMembers, getSuperTypes);
            }
        });
        if (clazz == null) {
            printWarnings(warnings, className);
            return null;
        }
        clazz = stubWithExtras(clazz, extra, new IOFunction<ClassNode, ClassNode>() {
            @Override
            public ClassNode apply(ClassNode classNode) throws IOException {
                return otherTransforms(classNode, extra, getReadOnly, warnings);
            }
        });
        if (clazz == null) {
            printWarnings(warnings, className);
            return null;
        }
        printWarnings(warnings, className);
        return clazz;
    }

    public void printWarnings(Set<String> warnings, String className) {
        if (!warnings.isEmpty() && logger.is(Logger.Level.WARN)) {
            if (downgrader.flags.checkInIgnoreWarnings(className)) return;
            StringBuilder sb = new StringBuilder();
            for (String warning : warnings) {
                sb.append("    ").append(warning).append("\n");
            }
            logger.warn("Warnings for " + className + " (" + warnings.size() + ") : \n" + sb);
        }
    }

    public ClassNode stubWithExtras(ClassNode clazz, Set<ClassNode> extra, IOFunction<ClassNode, ClassNode> stubber) throws IOException {
        clazz = stubber.apply(clazz);
        if (clazz == null) return null;
        for (ClassNode extraClass : new ArrayList<>(extra)) {
            ClassNode extraRemapped = stubber.apply(extraClass);
            extra.remove(extraClass);
            if (extraRemapped == null) continue;
            extra.add(extraRemapped);
        }
        return clazz;
    }

    public ClassNode insertAbstractMethods(ClassNode clazz, Set<ClassNode> extra, IOFunction<Type, Set<MemberNameAndDesc>> getMembers, IOFunction<Type, List<Pair<Type, Boolean>>> getSuperTypes) throws IOException {
        if (clazz.name.equals("module-info")) {
            return clazz;
        }
        ClassMapping cm = getStubMapper(Type.getObjectType(clazz.name), (clazz.access & Opcodes.ACC_INTERFACE) != 0, getMembers, getSuperTypes);
        Map<MemberNameAndDesc, Pair<Method, Stub>> members = cm.getAbstracts();
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

    public ClassNode otherTransforms(ClassNode clazz, Set<ClassNode> extra, Function<String, ClassNode> getReadOnly, Set<String> warnings) {
        clazz = otherTransforms(clazz, extra, getReadOnly);
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

    public ClassNode stubClasses(ClassNode clazz, boolean enableRuntime, Set<String> warnings) {
        if (clazz.name.equals("module-info")) {
            return clazz;
        }

        // super
        Type type = Type.getObjectType(clazz.superName);
        if (classStubs.containsKey(type)) {
            clazz.superName = classStubs.get(type).getFirst().getInternalName();
        }

        List<String> removedInterfaces = new ArrayList<>();
        // interface
        if (clazz.interfaces != null) {
            for (int i = 0; i < clazz.interfaces.size(); i++) {
                type = Type.getObjectType(clazz.interfaces.get(i));
                if (classStubs.containsKey(type)) {
                    Pair<Type, Pair<Class<?>, Adapter>> stub = classStubs.get(type);
                    if (stub.getSecond().getSecond().keepInterface()) {
                        clazz.interfaces.set(i, stub.getFirst().getInternalName());
                    } else {
                        removedInterfaces.add(clazz.interfaces.remove(i));
                        i--;
                    }
                }
            }
        }

        // innerclass
        if (clazz.innerClasses != null) {
            for (InnerClassNode inner : clazz.innerClasses) {
                type = Type.getObjectType(inner.name);
                if (classStubs.containsKey(type)) {
                    Pair<Type, Pair<Class<?>, Adapter>> stub = classStubs.get(type);
                    inner.name = stub.getFirst().getInternalName();
                }
                if (inner.outerName != null) {
                    type = Type.getObjectType(inner.outerName);
                    if (classStubs.containsKey(type)) {
                        Pair<Type, Pair<Class<?>, Adapter>> stub = classStubs.get(type);
                        inner.outerName = stub.getFirst().getInternalName();
                    }
                }
            }
        }

        if (!removedInterfaces.isEmpty()) {
            List<String> removed = new ArrayList<>();
            for (String removedInterface : removedInterfaces) {
                removed.add(Type.getObjectType(removedInterface).getClassName());
            }
            boolean found = false;
            for (AnnotationNode an : clazz.visibleAnnotations) {
                if (an.desc.equals(Type.getType(RemovedInterfaces.class).getDescriptor())) {
                    ((List<String>) an.values.get(1)).addAll(removed);
                    found = true;
                    break;
                }
            }
            if (!found) {
                AnnotationNode an = new AnnotationNode(Type.getType(RemovedInterfaces.class).getDescriptor());
                an.values = new ArrayList<>();
                an.values.add("value");
                an.values.add(removed);
                clazz.visibleAnnotations.add(an);
            }
        }

        // signature
        if (clazz.signature != null) {
            clazz.signature = transformSignature(clazz.signature, warnings);
        }

        // field descriptor
        if (clazz.fields != null) {
            for (FieldNode field : clazz.fields) {
                type = Type.getType(field.desc);
                field.desc = stubClass(type, warnings).getDescriptor();
                if (field.signature != null) {
                    field.signature = transformSignature(field.signature, warnings);
                }
            }
        }

        // method descriptor
        if (clazz.methods != null) {
            for (MethodNode method : clazz.methods) {
                type = Type.getMethodType(method.desc);
                method.desc = stubClass(type, warnings).getDescriptor();
                if (method.signature != null) {
                    method.signature = transformSignature(method.signature, warnings);
                }
                if (method.localVariables != null) {
                    for (LocalVariableNode local : method.localVariables) {
                        type = Type.getType(local.desc);
                        local.desc = stubClass(type, warnings).getDescriptor();
                        if (local.signature != null) {
                            local.signature = transformSignature(local.signature, warnings);
                        }
                    }
                }
                if (method.tryCatchBlocks != null) {
                    for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) {
                        if (tryCatch.type != null) {
                            type = Type.getObjectType(tryCatch.type);
                            tryCatch.type = stubClass(type, warnings).getInternalName();
                        }
                    }
                }
            }
        }
        return clazz;
    }

    public String transformSignature(String signature, final Set<String> warnings) {
        SignatureReader reader = new SignatureReader(signature);
        SignatureWriter writer = new SignatureWriter() {
            @Override
            public void visitClassType(String name) {
                super.visitClassType(stubClass(Type.getObjectType(name), warnings).getInternalName());
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

    public static class HandleMethodNode extends MethodNode {
        public final Handle ref;

        public HandleMethodNode(String caller, Handle ref, int num) {
            super(Opcodes.ASM9);
            caller = caller.replace("<", "$").replace(">", "$");
            this.ref = ref;
            if (ref.getTag() == Opcodes.H_NEWINVOKESPECIAL) {
                name = "jvmdowngrader$construct$";
            } else {
                name = "jvmdowngrader$call$";
            }
            name += caller + "$" + num;
        }
    }

}
