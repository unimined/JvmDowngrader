package xyz.wagyourtail.jvmdg.version;

import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.asm.AnnotationUtils;
import xyz.wagyourtail.jvmdg.asm.index.WorkingClasses;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.stub.all.RemovedInterfaces;
import xyz.wagyourtail.jvmdg.util.*;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.CtorEntry;
import xyz.wagyourtail.jvmdg.version.map.CtorFactory;
import xyz.wagyourtail.jvmdg.version.map.CtorRewrite;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberStubTransform;
import xyz.wagyourtail.jvmdg.version.map.StructuralRemapTransform;
import xyz.wagyourtail.jvmdg.version.map.StubEntry;
import xyz.wagyourtail.jvmdg.version.map.StubRegistry;
import xyz.wagyourtail.jvmdg.version.map.TypeRemap;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public abstract class VersionProvider implements TypeRemap {
    public final Map<Type, Pair<Type, Pair<Class<?>, Adapter>>> classStubs = new LinkedHashMap<>();
    public final Map<Type, ClassMapping> stubMappings = new LinkedHashMap<>();
    private final StructuralRemapTransform structuralRemap = new StructuralRemapTransform(this);
    /** registered @Stub/@Modify entries for this provider's input version, keyed by stubbed owner */
    public final StubRegistry stubRegistry = new StubRegistry();
    public final int inputVersion;
    public final int outputVersion;

    public final Coverage coverage;

    /**
     * Provider priority — higher wins when two providers claim the same
     * input version.
     */
    public final int priority;

    /**
     * @deprecated misspelled; use {@link #priority}. Kept for SPI
     * compatibility with providers compiled against older releases.
     */
    @Deprecated
    public final int priotity;

    /**
     * lateinit
     * bound during ensureInit
     */
    public ClassDowngrader downgrader;
    protected Logger logger;

    private volatile boolean initialized = false;
    private boolean initializing = false;
    // once init() fails, retries deterministically fail the same way (e.g.
    // providers whose stub classes cannot even load on the running JVM);
    // caching avoids re-running the whole init per caller.
    private Throwable initFailure = null;

    protected VersionProvider(int inputVersion, int outputVersion, int priority) {
        this.inputVersion = inputVersion;
        this.outputVersion = outputVersion;
        this.coverage = new Coverage(inputVersion, this);
        this.priority = priority;
        this.priotity = priority;
    }

    /**
     * Name lookup over the {@code extra} working set — O(1) when it is a
     * {@link WorkingClasses}, linear scan for any other {@link Set} a caller
     * might pass.
     */
    private static ClassNode findByName(Set<ClassNode> nodes, String name) {
        if (nodes instanceof WorkingClasses) {
            return ((WorkingClasses) nodes).get(name);
        }
        for (ClassNode node : nodes) {
            if (node.name.equals(name)) {
                return node;
            }
        }
        return null;
    }

    public void afterInit() {
        if (logger.is(Logger.Level.DEBUG)) {
            StringBuilder sb = new StringBuilder("Stubs: \n");
            for (Map.Entry<Type, Pair<Type, Pair<Class<?>, Adapter>>> stub : classStubs.entrySet()) {
                sb.append(stub.getKey().getInternalName()).append(" -> ").append(stub.getValue().getFirst()).append("\n");
            }
            for (Map.Entry<Type, ClassMapping> entry : stubMappings.entrySet()) {
                for (Map.Entry<MemberNameAndDesc, StubEntry> member : entry.getValue().getMethodStubMap().entrySet()) {
                    sb.append(entry.getKey().getInternalName())
                        .append(".").append(member.getKey().getName())
                        .append(member.getKey().getDesc()).append(" -> ")
                        .append(member.getValue().method.getDeclaringClass().getCanonicalName().replace('.', '/'))
                        .append(";")
                        .append(member.getValue().method.getName())
                        .append(Type.getMethodDescriptor(member.getValue().method))
                        .append("\n");
                }
            }
            logger.debug(sb.toString());
        }
    }

    public abstract void preInit();

    public abstract void init();

    public synchronized ClassMapping getStubMapper(final Type type, final Set<String> warnings) throws IOException {
        return getStubMapper(type, new IOSupplier<Boolean>() {
            @Override
            public Boolean get() throws IOException {
                return downgrader.isInterface(outputVersion, type, warnings) == Boolean.TRUE;
            }
        }, warnings);
    }

    public synchronized ClassMapping getStubMapper(Type type, final boolean isInterface, final Set<String> warnings) throws IOException {
        return getStubMapper(type, new IOSupplier<Boolean>() {
            @Override
            public Boolean get() {
                return isInterface;
            }
        }, warnings);
    }

    public synchronized ClassMapping getStubMapper(Type type, IOSupplier<Boolean> isInterface, final Set<String> warnings) throws IOException {
        return getStubMapper(type, isInterface, new IOFunction<Type, Set<MemberNameAndDesc>>() {

            @Override
            public Set<MemberNameAndDesc> apply(Type o) throws IOException {
                return downgrader.getMembers(inputVersion, o, warnings);
            }

        }, warnings);
    }

    public synchronized ClassMapping getStubMapper(Type type, final boolean isInterface, final IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, final Set<String> warnings) throws IOException {
        return getStubMapper(type, new IOSupplier<Boolean>() {
            @Override
            public Boolean get() {
                return isInterface;
            }
        }, memberResolver, warnings);
    }

    public synchronized ClassMapping getStubMapper(Type type, final IOSupplier<Boolean> isInterface, final IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, final Set<String> warnings) throws IOException {
        return getStubMapper(type, isInterface, memberResolver, new IOFunction<Type, List<Pair<Type, Boolean>>>() {

            @Override
            public List<Pair<Type, Boolean>> apply(Type o) throws IOException {
                return downgrader.getSupertypes(inputVersion, o, warnings);
            }

        });
    }

    public synchronized ClassMapping getStubMapper(final Type type, final boolean isInterface, final IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, final IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        return getStubMapper(type, new IOSupplier<Boolean>() {
            @Override
            public Boolean get() {
                return isInterface;
            }
        }, memberResolver, superTypeResolver);
    }

    public synchronized ClassMapping getStubMapper(final Type type, final IOSupplier<Boolean> isInterface, final IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, final IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
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
            }, type, isInterface.get(), memberResolver, this);
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
        }, type, isInterface.get(), memberResolver, this);
        stubMappings.put(type, mapping);
        return mapping;
    }

    public void stub(Class<?> clazz) {
        new StubScanner(this).scan(clazz);
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

    protected Handle stubHandle(ClassNode owner, MethodNode method, Set<ClassNode> extra, Handle bsm, String indyDesc, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Set<String> warnings, Handle handle) throws IOException {
        handle = new Handle(
            handle.getTag(),
            stubClass(Type.getObjectType(handle.getOwner()), warnings).getInternalName(),
            handle.getName(),
            stubClass(Type.getType(handle.getDesc()), warnings).getDescriptor(),
            handle.isInterface()
        );
        Type[] captured = null;
        if (bsm != null && bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
            captured = Type.getMethodType(indyDesc).getArgumentTypes();
        }
        Type hOwner = Type.getObjectType(handle.getOwner());
        if (hOwner.getSort() == Type.ARRAY) {
            return handle;
        }
        Type hDesc = Type.getType(handle.getDesc());
        MemberNameAndDesc member = new MemberNameAndDesc(handle.getName(), hDesc);
        ClassMapping stubMapper = getStubMapper(hOwner, handle.isInterface(), memberResolver, superTypeResolver);
        StubEntry min = stubMapper.getStubFor(member, enableRuntime, handle.getTag(), warnings);
        if (min != null) {
            if (min.downgradeVersion) {
                warnings.add("Invalid stub for bsm handle: " + handle.getOwner() + "." + handle.getName() + handle.getDesc());
            } else if (!min.abstractDefault) {
                // the descriptor as if it's an INVOKESTATIC
                Type hStaticDesc;
                switch (handle.getTag()) {
                    case Opcodes.H_GETFIELD:
                        hStaticDesc = Type.getMethodType(Type.getType(hDesc.getDescriptor()), Type.getObjectType(handle.getOwner()));
                        break;
                    case Opcodes.H_GETSTATIC:
                        hStaticDesc = Type.getMethodType("()" + hDesc.getDescriptor());
                        break;
                    case Opcodes.H_PUTFIELD:
                        hStaticDesc = Type.getMethodType(Type.VOID_TYPE, Type.getObjectType(handle.getOwner()), Type.getType(hDesc.getDescriptor()));
                        break;
                    case Opcodes.H_PUTSTATIC:
                        hStaticDesc = Type.getMethodType(Type.VOID_TYPE, Type.getType(hDesc.getDescriptor()));
                        break;
                    case Opcodes.H_INVOKESTATIC:
                        hStaticDesc = hDesc;
                        break;
                    case Opcodes.H_NEWINVOKESPECIAL:
                        warnings.add("@Stub should not be used for NEWINVOKESPECIAL in" + owner.name + ";" + method.name + ";" + method.desc + "; targetting " + handle.getOwner() + ";" + handle.getName() + ";" + handle.getDesc());
                    case Opcodes.H_INVOKEVIRTUAL:
                    case Opcodes.H_INVOKESPECIAL:
                    case Opcodes.H_INVOKEINTERFACE:
                        Type[] params = new Type[hDesc.getArgumentCount() + 1];
                        params[0] = hOwner;
                        System.arraycopy(hDesc.getArgumentTypes(), 0, params, 1, hDesc.getArgumentCount());
                        if (captured != null) {
                            // replace actual with capture, fixes invokeInterface on LambdaMetafactory
                            System.arraycopy(captured, 0, params, 0, captured.length);
                        }
                        hStaticDesc = Type.getMethodType(hDesc.getReturnType(), params);
                        break;
                    default:
                        throw new IllegalStateException("unknown tag: " + handle.getTag());
                }

                Method m = min.method;
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
                        mv.access = Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | (downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC);
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
            Method mod = stubMapper.getModifyFor(member, handle.getTag(), warnings);
            CtorEntry ce = null;
            if (mod == null && member.getName().equals("<init>") &&
                handle.getTag() == Opcodes.H_NEWINVOKESPECIAL) {
                ce = stubMapper.getCtorFor(member, enableRuntime, warnings);
            }
            if (mod != null || ce != null) {
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
                    }

                    if (found != null) {
                        name = found.name;
                    } else {
                        // construct wrapper
                        HandleMethodNode mn = new HandleMethodNode(method.name, handle, num);
                        mn.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
                        mn.desc = desc;
                        mn.visitCode();
                        int k = 0;
                        if (ce instanceof CtorFactory) {
                            CtorFactory factory = (CtorFactory) ce;
                            for (Type argument : arguments) {
                                mn.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), k);
                                k += argument.getSize();
                            }
                            if (factory.downgradeVersion) {
                                mn.visitLdcInsn(stubMapper.provider().inputVersion);
                            }
                            Method fm = factory.method;
                            mn.visitMethodInsn(Opcodes.INVOKESTATIC,
                                Type.getType(fm.getDeclaringClass()).getInternalName(),
                                fm.getName(), Type.getMethodDescriptor(fm),
                                fm.getDeclaringClass().isInterface());
                            if (!Type.getReturnType(fm).equals(returnType)) {
                                mn.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
                            }
                            mn.visitInsn(Opcodes.ARETURN);
                        } else {
                            mn.visitTypeInsn(Opcodes.NEW, returnType.getInternalName());
                            mn.visitInsn(Opcodes.DUP);
                            for (Type argument : arguments) {
                                mn.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), k);
                                k += argument.getSize();
                            }
                            mn.visitMethodInsn(Opcodes.INVOKESPECIAL, returnType.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, arguments), false);
                            mn.visitInsn(Opcodes.ARETURN);
                        }
                        mn.visitMaxs(0, 0);
                        mn.visitEnd();
                        owner.methods.add(mn);

                        if (ce instanceof CtorRewrite) {
                            // same rewrite as a plain NEW/DUP callsite — the wrapper
                            // emits exactly that shape for it to operate on
                            int iIndex = mn.instructions.size() - 2;
                            MemberStubTransform.rewriteCtor(mn, mn.instructions.get(iIndex),
                                iIndex, owner, returnType.getInternalName(), member,
                                (CtorRewrite) ce, stubMapper, warnings);
                        } else if (mod != null) {
                            // invoke modify
                            try {
                                List<Object> modifyArgs = Arrays.asList(mn, mn.instructions.size() - 2, owner, extra);
                                mod.invoke(null, modifyArgs.subList(0, mod.getParameterTypes().length).toArray());
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
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

    @Override
    public Type remap(Type type, Set<String> warnings) {
        return stubClass(type, warnings);
    }

    @Override
    public Pair<Type, Pair<Class<?>, Adapter>> stubFor(Type type) {
        return classStubs.get(type);
    }

    @Override
    public void warnUnstubbed(Type type, Set<String> warnings) {
        coverage.warnClass(type, warnings);
    }

    private Map<Type, Type> reverseClassStubs;
    private int reverseClassStubsSize = -1;

    /**
     * Translates an adapter (fake) type back to its real (JDK) type.
     * Needed when matching members on api-internal call sites: after api
     * downgrading, reflective signatures inside the api jar carry fake types,
     * while registered stub members and pre-transform call sites carry real
     * ones. Identity for types with no adapter.
     */
    public Type unstubClass(Type type) {
        if (type.getSort() == Type.ARRAY) {
            Type element = type.getElementType();
            Type unstubbed = unstubClass(element);
            if (unstubbed != element) {
                return Type.getType(type.getDescriptor().substring(0, type.getDimensions()) + unstubbed.getDescriptor());
            }
            return type;
        }
        if (type.getSort() != Type.OBJECT) {
            return type;
        }
        // adapters are registered across all providers in the chain — consult
        // every provider from this one's inputVersion down (starting at
        // outputVersion would exclude this provider's own adapters)
        List<VersionProvider> providers = downgrader.versionProviders(this.inputVersion);
        int total = 0;
        for (VersionProvider vp : providers) {
            total += vp.classStubs.size();
        }
        if (reverseClassStubs == null || reverseClassStubsSize != total) {
            Map<Type, Type> rev = new HashMap<>();
            for (VersionProvider vp : providers) {
                for (Map.Entry<Type, Pair<Type, Pair<Class<?>, Adapter>>> e : vp.classStubs.entrySet()) {
                    rev.put(e.getValue().getFirst(), e.getKey());
                }
            }
            reverseClassStubs = rev;
            reverseClassStubsSize = total;
        }
        Type unstubbed = reverseClassStubs.get(type);
        return unstubbed == null ? type : unstubbed;
    }

    /**
     * Member-set resolver closure for a stub pass — classpath index first,
     * then the original-bytes view ({@code getReadOnly}), then the emitted
     * extras; member descriptors are projected through
     * {@link ClassDowngrader#stubClass} at {@link #inputVersion} position.
     */
    public IOFunction<Type, Set<MemberNameAndDesc>> memberResolver(final Set<ClassNode> extra, final Function<String, ClassNode> getReadOnly, final Set<String> warnings) {
        final ClassDowngrader downgrader = this.downgrader;
        return new IOFunction<Type, Set<MemberNameAndDesc>>() {
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
                        for (FieldNode field : ro.fields) {
                            if ((field.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) != 0) continue;
                            members.add(new MemberNameAndDesc(field.name, downgrader.stubClass(ro.version, Type.getType(field.desc), warnings)));
                        }
                    }
                }
                // if not found in read-only, check extra
                if (members == null) {
                    ClassNode extraClass = findByName(extra, o.getInternalName());
                    if (extraClass != null) {
                        members = new HashSet<>();
                        for (MethodNode method : extraClass.methods) {
                            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) != 0) continue;
                            members.add(new MemberNameAndDesc(method.name, downgrader.stubClass(extraClass.version, Type.getMethodType(method.desc), warnings)));
                        }
                        for (FieldNode field : extraClass.fields) {
                            if ((field.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) != 0) continue;
                            members.add(new MemberNameAndDesc(field.name, downgrader.stubClass(extraClass.version, Type.getType(field.desc), warnings)));
                        }
                    }
                }
                return members;
            }
        };
    }

    /**
     * Supertype-list resolver closure — same layering as
     * {@link #memberResolver}; supertypes are projected through
     * {@link ClassDowngrader#stubClass} keyed on the read class's own file
     * version.
     */
    public IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver(final Set<ClassNode> extra, final Function<String, ClassNode> getReadOnly, final Set<String> warnings) {
        final ClassDowngrader downgrader = this.downgrader;
        return new IOFunction<Type, List<Pair<Type, Boolean>>>() {

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
                    ClassNode extraClass = findByName(extra, o.getInternalName());
                    if (extraClass != null) {
                        types = new ArrayList<>();
                        types.add(new Pair<>(downgrader.stubClass(extraClass.version, Type.getObjectType(extraClass.superName), warnings), Boolean.FALSE));
                        for (String anInterface : extraClass.interfaces) {
                            types.add(new Pair<>(downgrader.stubClass(extraClass.version, Type.getObjectType(anInterface), warnings), Boolean.TRUE));
                        }
                    }
                }
                return types;
            }
        };
    }

    public ClassNode stubMethods(ClassNode owner, Set<ClassNode> extra, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Function<String, ClassNode> getReadOnly) throws IOException {
        if (owner.name.equals("module-info")) {
            return owner;
        }

        for (MethodNode method : new ArrayList<>(owner.methods)) {
            Set<String> warnings = new LinkedHashSet<>();
            MethodNode newMethod = stubMethod(method, owner, extra, enableRuntime, memberResolver, superTypeResolver, getReadOnly, warnings);
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

    public MethodNode stubMethod(MethodNode method, ClassNode owner, Set<ClassNode> extra, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, final Function<String, ClassNode> getReadOnly, final Set<String> warnings) throws IOException {
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode insn = method.instructions.get(i);
            AbstractInsnNode replacement = structuralRemap.remapInsn(insn, warnings);
            if (replacement != null) {
                method.instructions.set(insn, replacement);
                continue;
            }
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) insn;
                if (!min.owner.startsWith("[")) {
                    getStubMapper(Type.getObjectType(min.owner), min.itf, memberResolver, superTypeResolver)
                        .transform(method, i, owner, extra, enableRuntime, warnings);
                }
            } else if (insn instanceof FieldInsnNode) {
                final FieldInsnNode fin = (FieldInsnNode) insn;
                if (!fin.owner.startsWith("[")) {
                    getStubMapper(Type.getObjectType(fin.owner), new IOSupplier<Boolean>() {
                        @Override
                        public Boolean get() throws IOException {
                            return downgrader.isInterface(outputVersion, Type.getObjectType(fin.owner), warnings) == Boolean.TRUE;
                        }
                    }, memberResolver, superTypeResolver)
                        .transform(method, i, owner, extra, enableRuntime, warnings);
                }
            } else if (insn instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                stubBSMArgs(owner, method, extra, indy.bsm, indy.desc, indy.bsmArgs, enableRuntime, memberResolver, superTypeResolver, warnings);
                getStubMapper(Type.getObjectType(indy.bsm.getOwner()), indy.bsm.isInterface(), memberResolver, superTypeResolver)
                    .transform(method, i, owner, extra, enableRuntime, warnings);
            } else if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (ldc.cst instanceof ConstantDynamic) {
                    ldc.cst = stubCondy(owner, method, extra, (ConstantDynamic) ldc.cst, enableRuntime, memberResolver, superTypeResolver, warnings);
                }
            }
        }
        return method;
    }


    public void ensureInit(ClassDowngrader downgrader) {
        this.downgrader = downgrader;
        if (this.logger == null) {
            this.logger = downgrader.logger.subLogger(getClass());
        }
        if (initFailure != null) {
            rethrowInitFailure();
        }
        if (!initialized) {
            synchronized (this) {
                if (initFailure != null) {
                    rethrowInitFailure();
                }
                if (!initialized) {
                    if (initializing) {
                        // re-entrant call from the thread already running init():
                        // registering a stub class loads it through the
                        // DowngradingClassLoader, whose transform re-enters
                        // ensureInit. Re-running init() would re-request classes
                        // that are mid-definition -> ClassCircularityError.
                        // Proceeding with a partially-registered provider is
                        // strictly better than crashing.
                        return;
                    }
                    if (downgrader.flags.debugSkipStubs.contains(inputVersion)) {
                        initialized = true;
                        return;
                    }
                    initializing = true;
                    try {
                        preInit();
                        init();
                        afterInit();
                        initialized = true;
                    } catch (Throwable t) {
                        initFailure = t;
                        Utils.<RuntimeException>sneakyThrow(t);
                    } finally {
                        initializing = false;
                    }
                }
            }
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void rethrowInitFailure() {
        Utils.<RuntimeException>sneakyThrow(initFailure);
    }

    public ClassNode downgrade(final ClassDowngrader downgrader, ClassNode clazz, final Set<ClassNode> extra, final boolean enableRuntime, final Function<String, ClassNode> getReadOnly) throws IOException {
        String className = clazz.name;

        if (clazz.version != inputVersion)
            throw new IllegalArgumentException("Class " + className + " is not version " + inputVersion);

        ensureInit(downgrader);
        final Set<String> warnings = new LinkedHashSet<>();

        clazz.version = outputVersion;

        if (clazz.name.equals("module-info")) {
            if (clazz.module.requires == null) {
                clazz.module.requires = new ArrayList<>();
            } else {
                boolean contains = true;
                for (ModuleRequireNode node : clazz.module.requires) {
                    if (node.module.equals("xyz.wagyourtail.jvmdg.java_api")) {
                        contains = false;
                        break;
                    }
                }
                // else
                if (contains) {
                    clazz.module.requires.add(new ModuleRequireNode("xyz.wagyourtail.jvmdg.java_api", 0, Flags.jvmdgVersion));
                }
            }
        }

        final IOFunction<Type, Set<MemberNameAndDesc>> getMembers = memberResolver(extra, getReadOnly, warnings);
        final IOFunction<Type, List<Pair<Type, Boolean>>> getSuperTypes = superTypeResolver(extra, getReadOnly, warnings);

        clazz = stubClasses(clazz, enableRuntime, warnings);
        if (clazz == null) {
            printWarnings(warnings, className);
            return null;
        }
        clazz = stubWithExtras(clazz, extra, new IOFunction<ClassNode, ClassNode>() {
            @Override
            public ClassNode apply(ClassNode classNode) throws IOException {
                return stubMethods(classNode, extra, enableRuntime, getMembers, getSuperTypes, getReadOnly);
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
                return otherTransforms(classNode, extra, getReadOnly, warnings, enableRuntime, getMembers, getSuperTypes);
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
        Map<MemberNameAndDesc, StubEntry> members = cm.getAbstracts();
        for (Map.Entry<MemberNameAndDesc, StubEntry> member : members.entrySet()) {
            boolean contains = false;
            for (MethodNode method : clazz.methods) {
                if (method.name.equals(member.getKey().getName()) && method.desc.equals(member.getKey().getDesc().getDescriptor())) {
                    contains = true;
                    break;
                }
            }
            if (contains) continue;
            MethodVisitor mv = clazz.visitMethod(Opcodes.ACC_PUBLIC | (downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), member.getKey().getName(), member.getKey().getDesc().getDescriptor(), null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            Type[] params = member.getKey().getDesc().getArgumentTypes();
            int i = 1;
            for (Type param : params) {
                mv.visitVarInsn(param.getOpcode(Opcodes.ILOAD), i);
                i += param.getSize();
            }
            if (member.getValue().downgradeVersion) {
                mv.visitLdcInsn(this.inputVersion);
            }
            Class<?> declaring = member.getValue().method.getDeclaringClass();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getType(declaring).getInternalName(), member.getValue().method.getName(), Type.getMethodDescriptor(member.getValue().method), declaring.isInterface());
            Type returnType = member.getKey().getDesc().getReturnType();
            mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        return clazz;
    }

    public ClassNode otherTransforms(ClassNode clazz, Set<ClassNode> extra, Function<String, ClassNode> getReadOnly, Set<String> warnings, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        clazz = otherTransforms(clazz, extra, getReadOnly, warnings);
        return clazz;
    }

    public ClassNode otherTransforms(ClassNode clazz, Set<ClassNode> extra, Function<String, ClassNode> getReadOnly, Set<String> warnings) throws IOException {
        clazz = otherTransforms(clazz, extra, getReadOnly);
        return clazz;
    }

    public ClassNode otherTransforms(ClassNode clazz, Set<ClassNode> extra, Function<String, ClassNode> getReadOnly) throws IOException {
        clazz = otherTransforms(clazz, extra);
        return clazz;
    }

    public ClassNode otherTransforms(ClassNode clazz, Set<ClassNode> extra) throws IOException {
        clazz = otherTransforms(clazz);
        return clazz;
    }

    public ClassNode otherTransforms(ClassNode clazz) throws IOException {
        return clazz;
    }

    public ClassNode stubClasses(ClassNode clazz, boolean enableRuntime, Set<String> warnings) {
        if (clazz.name.equals("module-info")) {
            return clazz;
        }

        Type type = Type.getObjectType(clazz.superName);
        if (classStubs.containsKey(type)) {
            clazz.superName = classStubs.get(type).getFirst().getInternalName();
        }

        List<String> removedInterfaces = new ArrayList<>();
        if (clazz.interfaces != null) {
            for (int i = 0; i < clazz.interfaces.size(); i++) {
                type = Type.getObjectType(clazz.interfaces.get(i));
                if (classStubs.containsKey(type)) {
                    Pair<Type, Pair<Class<?>, Adapter>> stub = classStubs.get(type);
                    if (stub.getSecond().getSecond().keepInterface()) {
                        clazz.interfaces.set(i, stub.getFirst().getInternalName());
                    } else {
                        removedInterfaces.add(clazz.interfaces.remove(i--));
                    }
                }
            }
        }

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
            if (clazz.visibleAnnotations != null) {
                for (AnnotationNode an : clazz.visibleAnnotations) {
                    if (an.desc.equals(Type.getType(RemovedInterfaces.class).getDescriptor())) {
                        ((List<String>) an.values.get(1)).addAll(removed);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                AnnotationNode an = new AnnotationNode(Type.getType(RemovedInterfaces.class).getDescriptor());
                an.values = new ArrayList<>();
                an.values.add("value");
                an.values.add(removed);
                if (clazz.visibleAnnotations == null) {
                    clazz.visibleAnnotations = new ArrayList<>();
                }
                clazz.visibleAnnotations.add(an);
            }
        }

        if (clazz.signature != null) {
            clazz.signature = transformSignature(clazz.signature, warnings);
        }

        transformAnnotations(clazz.visibleAnnotations, warnings);
        transformAnnotations(clazz.invisibleAnnotations, warnings);
        transformAnnotations(clazz.visibleTypeAnnotations, warnings);
        transformAnnotations(clazz.invisibleTypeAnnotations, warnings);

        if (clazz.fields != null) {
            for (FieldNode field : clazz.fields) {
                type = Type.getType(field.desc);
                field.desc = stubClass(type, warnings).getDescriptor();
                if (field.signature != null) {
                    field.signature = transformSignature(field.signature, warnings);
                }

                transformAnnotations(field.visibleAnnotations, warnings);
                transformAnnotations(field.invisibleAnnotations, warnings);
                transformAnnotations(field.visibleTypeAnnotations, warnings);
                transformAnnotations(field.invisibleTypeAnnotations, warnings);
            }
        }

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

                transformAnnotations(method.visibleAnnotations, warnings);
                transformAnnotations(method.invisibleAnnotations, warnings);
                transformAnnotations(method.visibleTypeAnnotations, warnings);
                transformAnnotations(method.invisibleTypeAnnotations, warnings);
                transformAnnotations(method.visibleLocalVariableAnnotations, warnings);
                transformAnnotations(method.invisibleLocalVariableAnnotations, warnings);

                if (method.visibleParameterAnnotations != null) {
                    for (List<AnnotationNode> annotationNodes : method.visibleParameterAnnotations) {
                        transformAnnotations(annotationNodes, warnings);
                    }
                }

                if (method.invisibleParameterAnnotations != null) {
                    for (List<AnnotationNode> annotationNodes : method.invisibleParameterAnnotations) {
                        transformAnnotations(annotationNodes, warnings);
                    }
                }
            }
        }

        if (clazz.recordComponents != null) {
            for (RecordComponentNode recordComponent : clazz.recordComponents) {
                recordComponent.descriptor = stubClass(Type.getType(recordComponent.descriptor), warnings).getDescriptor();

                if (recordComponent.signature != null) {
                    recordComponent.signature = transformSignature(recordComponent.signature, warnings);
                }

                transformAnnotations(recordComponent.visibleAnnotations, warnings);
                transformAnnotations(recordComponent.invisibleAnnotations, warnings);
                transformAnnotations(recordComponent.visibleTypeAnnotations, warnings);
                transformAnnotations(recordComponent.invisibleTypeAnnotations, warnings);
            }
        }

        return clazz;
    }

    public <E extends AnnotationNode> void transformAnnotations(List<E> annotations, final Set<String> warnings) {
        if (annotations == null) return;
        for (E annotation : new ArrayList<>(annotations)) {
            Type annotationType = Type.getType(annotation.desc);
            if (classStubs.containsKey(annotationType)) {
                Pair<Type, Pair<Class<?>, Adapter>> stub = classStubs.get(annotationType);
                if (stub.getSecond().getSecond().keepInterface()) {
                    E newAnnotation = AnnotationUtils.copyAnnotationNode(annotation);
                    transformAnnotation(newAnnotation, warnings);
                    annotations.add(newAnnotation);
                } else {
                    transformAnnotation(annotation, warnings);
                }
            }
        }
    }

    public void transformAnnotation(AnnotationNode node, final Set<String> warnings) {
        node.desc = stubClass(Type.getType(node.desc), warnings).getDescriptor();
        if (node.values != null) {
            for (int i = 0; i < node.values.size(); i += 2) {
                Object value = node.values.get(i + 1);
                if (value instanceof Type) {
                    node.values.set(i + 1, stubClass((Type) value, warnings));
                } else if (value instanceof AnnotationNode) {
                    transformAnnotation((AnnotationNode) value, warnings);
                } else if (value instanceof List<?>) {
                    List<Object> list = (List<Object>) value;
                    for (int j = 0; j < list.size(); j++) {
                        Object listValue = list.get(j);
                        if (listValue instanceof Type) {
                            list.set(j, stubClass((Type) listValue, warnings));
                        } else if (listValue instanceof AnnotationNode) {
                            transformAnnotation((AnnotationNode) listValue, warnings);
                        }
                    }
                }
            }
        }
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
