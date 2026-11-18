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
import xyz.wagyourtail.jvmdg.stub.all.GainedInterfaces;
import xyz.wagyourtail.jvmdg.stub.all.JvmDowngraded;
import xyz.wagyourtail.jvmdg.stub.all.RemovedInterfaces;
import xyz.wagyourtail.jvmdg.util.*;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;
import xyz.wagyourtail.jvmdg.version.map.ClassStubEntry;
import xyz.wagyourtail.jvmdg.version.map.CtorEntry;
import xyz.wagyourtail.jvmdg.version.map.CtorFactory;
import xyz.wagyourtail.jvmdg.version.map.CtorRewrite;
import xyz.wagyourtail.jvmdg.version.map.DispatchSites;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.HandleStubber;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberStubTransform;
import xyz.wagyourtail.jvmdg.version.map.MethodAnalyses;
import xyz.wagyourtail.jvmdg.version.map.MethodRef;
import xyz.wagyourtail.jvmdg.version.map.StructuralRemapTransform;
import xyz.wagyourtail.jvmdg.version.map.StubEntry;
import xyz.wagyourtail.jvmdg.version.map.StubRegistry;
import xyz.wagyourtail.jvmdg.version.map.TypeRemap;
import xyz.wagyourtail.jvmdg.version.idx.DerivedExclusions;
import xyz.wagyourtail.jvmdg.version.idx.DerivedGained;
import xyz.wagyourtail.jvmdg.version.idx.DerivedMemberSince;
import xyz.wagyourtail.jvmdg.version.idx.StubIndex;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class VersionProvider implements TypeRemap {
    /**
     * This provider's stub registry data — the object both load paths
     * (reflective {@link #init()} scan and {@code stubs.idx}) populate.
     * The {@code public final} map fields below are aliases to its maps,
     * kept for SPI compatibility; new code should go through the catalog.
     */
    public final StubCatalog catalog = new StubCatalog(this);
    /** alias of {@link #catalog} — see {@link StubCatalog#classStubs} */
    public final Map<Type, ClassStubEntry> classStubs = catalog.classStubs;
    /** alias of {@link #catalog} — see {@link StubCatalog#adapterImpls} */
    public final Map<String, List<String[]>> adapterImpls = catalog.adapterImpls;
    /** alias of {@link #catalog} — see {@link StubCatalog#gained} */
    public final Map<String, Set<String>> gained = catalog.gained;
    /** alias of {@link #catalog} — see {@link StubCatalog#gainedMembers} */
    public final Map<String, Set<String>> gainedMembers = catalog.gainedMembers;
    /** alias of {@link #catalog} — see {@link StubCatalog#gainedStubSites} */
    public final Map<String, Map<String, Set<String>>> gainedStubSites = catalog.gainedStubSites;
    /** alias of {@link #catalog} — see {@link StubCatalog#stubMappings} */
    public final Map<Type, ClassMapping> stubMappings = catalog.stubMappings;
    private final StructuralRemapTransform structuralRemap = new StructuralRemapTransform(this);
    /**
     * The run-scoped method-analysis store, injected by
     * {@code StubMethodsTransform} on each apply — the store is
     * ctx-shared and {@link MethodNode}-identity-keyed, so it is never
     * nulled back out: applies run concurrently on one provider and a
     * {@code finally}-clear would race a sibling's {@code stubMethod}.
     * Volatile so a sibling thread's injection is visible; a late reader
     * just falls back to a fresh holder. Engine-internal; not for
     * provider code.
     */
    public volatile MethodAnalyses methodAnalyses;
    /** alias of {@link #catalog} — see {@link StubCatalog#stubRegistry} */
    public final StubRegistry stubRegistry = catalog.stubRegistry;
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
            for (Map.Entry<Type, ClassStubEntry> stub : classStubs.entrySet()) {
                sb.append(stub.getKey().getInternalName()).append(" -> ").append(stub.getValue().target).append("\n");
            }
            for (Map.Entry<Type, ClassMapping> entry : stubMappings.entrySet()) {
                for (Map.Entry<MemberNameAndDesc, StubEntry> member : entry.getValue().getMethodStubMap().entrySet()) {
                    sb.append(entry.getKey().getInternalName())
                        .append(".").append(member.getKey().getName())
                        .append(member.getKey().getDesc()).append(" -> ")
                        .append(member.getValue().handler.member.getOwner().getInternalName())
                        .append(";")
                        .append(member.getValue().handler.member.getName())
                        .append(member.getValue().handler.member.getDesc().getDescriptor())
                        .append("\n");
                }
            }
            logger.debug(sb.toString());
        }
    }

    /**
     * ServiceLoader-discovered {@link ReflectionContributor}s — the
     * reflection-stub surface's single declaration point, applied at every
     * provider boundary (registries are per-boundary by design). Sorted by
     * class name for deterministic registration order.
     */
    private static final List<ReflectionContributor> CONTRIBUTORS = loadContributors();

    private static List<ReflectionContributor> loadContributors() {
        List<ReflectionContributor> out = new ArrayList<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = VersionProvider.class.getClassLoader();
        }
        for (ReflectionContributor c : ServiceLoader.load(ReflectionContributor.class, loader)) {
            out.add(c);
        }
        out.sort(Comparator.comparing(c -> c.getClass().getName()));
        return Collections.unmodifiableList(out);
    }

    /**
     * Contributes every discovered {@link ReflectionContributor} to this
     * provider's registry — runs inside {@link #ensureInit}'s window,
     * before {@link #init()}. Override to add (or skip) pre-registration
     * work; call {@code super.preInit()} to keep the contributor surface.
     */
    public void preInit() {
        for (ReflectionContributor contributor : CONTRIBUTORS) {
            contributor.contribute(this);
        }
    }

    /**
     * Reflective stub registration. Bundled providers carry a generated
     * {@code init()} body (emitted into the api jar by {@code
     * StubIndexWriter}'s classpath enumeration — every marker-annotated
     * class under {@code jvmdg/j<N>/}); external providers override this
     * as the SPI registration point. Runs only on the scan path — when
     * {@code stubs.idx} loads, registration comes from the index.
     */
    public void init() {
    }

    /**
     * Instantiates this boundary's {@link BoundaryTransform}s into
     * {@link #boundaryTransforms} — runs at the end of {@link
     * #ensureInit}'s window on both load paths, after the registry is
     * complete and before the {@code order()}-keyed sort. Instantiation
     * is deliberately NOT in {@code <init>}: providers are constructed
     * inside {@link ClassDowngrader#collectProviders}'s ServiceLoader
     * pass, where loading a transform class would re-enter a half-built
     * downgrader; and inside the init window the class transforms
     * against the finished registry. External providers may still
     * ctor-populate the field — the sort below normalizes both — but
     * any class that loads through the {@link
     * xyz.wagyourtail.jvmdg.classloader.DowngradingClassLoader}
     * (i.e. jvmdg-bundled transforms) must instantiate here.
     */
    public void registerBoundaryTransforms() {
    }

    /**
     * The identity this provider serializes under in {@code stubs.idx}
     * and the flattened per-provider resources — the runtime class name
     * for real providers ({@code xyz.wagyourtail.jvmdg.providers.Java<N>Downgrader}),
     * overridden at index-build time when the writer stands in generic
     * instances for the generated classes it is about to emit.
     */
    public String providerKey() {
        return getClass().getName();
    }

    public ClassMapping getStubMapper(final Type type, final Set<String> warnings) throws IOException {
        return getStubMapper(type, new IOSupplier<Boolean>() {
            @Override
            public Boolean get() throws IOException {
                return downgrader.isInterface(outputVersion, type, warnings) == Boolean.TRUE;
            }
        }, warnings);
    }

    public ClassMapping getStubMapper(Type type, final boolean isInterface, final Set<String> warnings) throws IOException {
        return getStubMapper(type, new IOSupplier<Boolean>() {
            @Override
            public Boolean get() {
                return isInterface;
            }
        }, warnings);
    }

    public ClassMapping getStubMapper(Type type, IOSupplier<Boolean> isInterface, final Set<String> warnings) throws IOException {
        return getStubMapper(type, isInterface, new IOFunction<Type, Set<MemberNameAndDesc>>() {

            @Override
            public Set<MemberNameAndDesc> apply(Type o) throws IOException {
                return downgrader.getMembers(inputVersion, o, warnings);
            }

        }, warnings);
    }

    public ClassMapping getStubMapper(Type type, final IOSupplier<Boolean> isInterface, final IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, final Set<String> warnings) throws IOException {
        return getStubMapper(type, isInterface, memberResolver, new IOFunction<Type, List<Pair<Type, Boolean>>>() {

            @Override
            public List<Pair<Type, Boolean>> apply(Type o) throws IOException {
                return downgrader.getSupertypes(inputVersion, o, warnings);
            }

        });
    }

    public ClassMapping getStubMapper(final Type type, final boolean isInterface, final IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, final IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        return catalog.getStubMapper(type, isInterface, memberResolver, superTypeResolver);
    }

    /** resolver entry point — the registry walk lives on {@link StubCatalog#getStubMapper(Type, IOSupplier, IOFunction, IOFunction)} */
    public ClassMapping getStubMapper(final Type type, final IOSupplier<Boolean> isInterface, final IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, final IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        return catalog.getStubMapper(type, isInterface, memberResolver, superTypeResolver);
    }

    /**
     * Gen-time registration seam — {@code StubIndexWriter} sets the
     * enumerated stub-class list before {@link #ensureInit} so
     * registration happens inside the init window, ahead of
     * {@code AdapterForwarders.registerAll} and the derived-resource
     * loaders that consume the completed registry. Null in every other
     * context — not SPI.
     */
    public List<String> genStubClasses;

    public void stub(Class<?> clazz) {
        new StubScanner(this).scan(clazz);
    }



    private HandleStubber stubber;

    private HandleStubber stubber() {
        if (stubber == null) {
            stubber = new HandleStubber(this);
        }
        return stubber;
    }

    /** arg-slot rewriting into stub vocabulary — lives in {@link HandleStubber}; kept for SPI/compat */
    public void stubBSMArgs(ClassNode owner, MethodNode method, Set<ClassNode> extra, Handle bsm, String indyDesc, Object[] bsmArgs, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Set<String> warnings) throws IOException {
        stubber().stubBSMArgs(owner, method, extra, bsm, indyDesc, bsmArgs, enableRuntime, memberResolver, superTypeResolver, warnings);
    }

    /** handle rewriting into stub vocabulary — lives in {@link HandleStubber}; providers call this for custom handle sites */
    public Handle stubHandle(ClassNode owner, MethodNode method, Set<ClassNode> extra, Handle bsm, String indyDesc, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Set<String> warnings, Handle handle) throws IOException {
        return stubber().stubHandle(owner, method, extra, bsm, indyDesc, enableRuntime, memberResolver, superTypeResolver, warnings, handle);
    }

    /** condy rewriting into stub vocabulary — lives in {@link HandleStubber}; kept for SPI/compat */
    public ConstantDynamic stubCondy(ClassNode owner, MethodNode method, Set<ClassNode> extra, ConstantDynamic condy, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Set<String> warnings) throws IOException {
        return stubber().stubCondy(owner, method, extra, condy, enableRuntime, memberResolver, superTypeResolver, warnings);
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
                    type = classStubs.get(type).target;
                }
                return Type.getType(desc.getDescriptor().substring(0, desc.getDimensions()) + type.getDescriptor());
            case Type.OBJECT:
                if (classStubs.containsKey(desc)) {
                    return classStubs.get(desc).target;
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
    public ClassStubEntry stubFor(Type type) {
        return classStubs.get(type);
    }

    /**
     * {@link TypeRemap#gainedFor} — the boundary-filtered gained table keys
     * the interface's generated dispatcher owner; the interface itself is
     * real at the output so it never appears in {@link #classStubs}.
     */
    @Override
    public String gainedFor(Type type) {
        return gained.containsKey(type.getInternalName())
            ? xyz.wagyourtail.jvmdg.version.map.GainedSites.dispatchOwner(type.getInternalName())
            : null;
    }

    /**
     * Registry-write filter matching the reflective scanner's per-member
     * checks: a handler is skipped when its {@code @JEP} tags intersect
     * {@code debugSkipJeps} or its own/declaring-class FQMND is in
     * {@code debugSkipStub}. Enforced on every registration path (scan and
     * index) so registries are identical under the same flags.
     */
    public boolean shouldSkip(MethodRef handler) {
        if (handler == null) {
            return false;
        }
        for (int jep : handler.jeps) {
            if (downgrader.flags.debugSkipJeps.contains(jep)) {
                return true;
            }
        }
        return downgrader.flags.debugSkipStub.contains(handler.member)
            || downgrader.flags.debugSkipStub.contains(FullyQualifiedMemberNameAndDesc.of(handler.member.getOwner()));
    }

    /** class-type registration variant — stub-class JEPs + stub-class FQMND */
    public boolean shouldSkip(ClassStubEntry entry) {
        for (int jep : entry.jeps) {
            if (downgrader.flags.debugSkipJeps.contains(jep)) {
                return true;
            }
        }
        return downgrader.flags.debugSkipStub.contains(FullyQualifiedMemberNameAndDesc.of(entry.stubType));
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
        // outputVersion would exclude this provider's own adapters); a
        // detached provider (unit tests) has no chain — its own stubs are
        // the only ones it could know
        List<VersionProvider> providers = downgrader != null
            ? downgrader.versionProviders(this.inputVersion)
            : Collections.singletonList(this);
        int total = 0;
        for (VersionProvider vp : providers) {
            total += vp.classStubs.size();
        }
        if (reverseClassStubs == null || reverseClassStubsSize != total) {
            Map<Type, Type> rev = new HashMap<>();
            for (VersionProvider vp : providers) {
                for (Map.Entry<Type, ClassStubEntry> e : vp.classStubs.entrySet()) {
                    rev.put(e.getValue().target, e.getKey());
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
        xyz.wagyourtail.jvmdg.config.ScopedRule scopedRule;
        try {
            scopedRule = downgrader.flags.scopedRules.isEmpty() ? null :
                xyz.wagyourtail.jvmdg.config.ScopedRule.resolve(downgrader.flags.scopedRules, owner.name);
        } catch (xyz.wagyourtail.jvmdg.config.ManifestConfigReader.ConfigException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        // methodAnalyses is injected per-apply by StubMethodsTransform when a
        // shared run-scoped store exists; legacy callers get a private holder
        StructuralRemapTransform.MethodAnalysis methodAnalysis = methodAnalyses != null ?
            methodAnalyses.getOrCreate(owner.name, method) :
            structuralRemap.analysis(owner.name, method);
        java.util.Map<AbstractInsnNode, org.objectweb.asm.tree.InsnList> classCallRewrites =
            structuralRemap.remapClassCalls(methodAnalysis, this.inputVersion);
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode insn = method.instructions.get(i);
            org.objectweb.asm.tree.InsnList routed = classCallRewrites.remove(insn);
            if (routed != null) {
                method.instructions.insert(insn, routed);
                method.instructions.remove(insn);
                continue;
            }
            org.objectweb.asm.tree.InsnList replacement = structuralRemap.remapInsn(insn, warnings, this.inputVersion, owner.name);
            if (replacement != null) {
                method.instructions.insert(insn, replacement);
                method.instructions.remove(insn);
                continue;
            }
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) insn;
                if (!min.owner.startsWith("[")) {
                    getStubMapper(Type.getObjectType(min.owner), min.itf, memberResolver, superTypeResolver)
                        .transform(method, i, owner, extra, enableRuntime, warnings, scopedRule);
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
                        .transform(method, i, owner, extra, enableRuntime, warnings, scopedRule);
                }
            } else if (insn instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                stubBSMArgs(owner, method, extra, indy.bsm, indy.desc, indy.bsmArgs, enableRuntime, memberResolver, superTypeResolver, warnings);
                getStubMapper(Type.getObjectType(indy.bsm.getOwner()), indy.bsm.isInterface(), memberResolver, superTypeResolver)
                    .transform(method, i, owner, extra, enableRuntime, warnings, scopedRule);
            } else if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (ldc.cst instanceof ConstantDynamic) {
                    ldc.cst = stubCondy(owner, method, extra, (ConstantDynamic) ldc.cst, enableRuntime, memberResolver, superTypeResolver, warnings);
                }
            }
        }
        return method;
    }


    /**
     * {@code Flags.allowPreview} of the bound downgrader — false for
     * detached/test providers that never got {@link #ensureInit}.
     */
    public boolean allowPreview() {
        return downgrader != null && downgrader.flags.allowPreview;
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
                        if (loadStubIndex(downgrader)) {
                            stubIndexLoaded = true;
                        } else {
                            preInit();
                            // StubIndexWriter's stand-in providers have no
                            // compiled init() — they register the classpath
                            // enumeration here so the idx is built from the
                            // same list ProviderGenerator bakes into the
                            // emitted init() bodies
                            if (genStubClasses != null) {
                                for (String fqcn : genStubClasses) {
                                    try {
                                        stub(Class.forName(fqcn, false, getClass().getClassLoader()));
                                    } catch (ClassNotFoundException e) {
                                        throw new IOException("enumerated stub class not on classpath: " + fqcn, e);
                                    }
                                }
                            } else {
                                // bundled providers' init() bodies are
                                // generated; external providers' are the
                                // SPI registration point
                                init();
                            }
                            // OWN_INTERFACE forwarder registrations need the
                            // complete adapterImpls picture — a descendant
                            // adapter's impls pull up to ancestors, so
                            // adaptability is only decidable post-scan
                            AdapterForwarders.registerAll(this);
                            // scan-built entries carry only the declared
                            // excludeChild — union the derived set the idx
                            // path already has baked in (no resource = older
                            // api jar = nothing derived)
                            DerivedExclusions.apply(catalog);
                            // memberSince feeds kind classification (BEHAVIOR
                            // vs MISSING_API) — the idx carries it inline, the
                            // scan path loads the flattened resource
                            DerivedMemberSince.apply(catalog);
                        }
                        // gained-supertypes rows are provider-level resource
                        // data — the same resource feeds both load paths
                        DerivedGained.apply(catalog);
                        // transform instantiation is deferred past registry
                        // completion (and past collectProviders — see the
                        // hook's javadoc); the sort normalizes hook-added
                        // and ctor-added transforms identically on both
                        // load paths
                        registerBoundaryTransforms();
                        boundaryTransforms.sort(Comparator.comparingInt(BoundaryTransform::order).thenComparing(t -> t.getClass().getName()));
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

    /** set when the registry came from {@code stubs.idx} rather than the reflective scan — test/debug observable */
    public boolean stubIndexLoaded = false;

    /**
     * Try to load this provider's section of {@code META-INF/jvmdg/stubs.idx}
     * (written at api-jar build time from this same scan) instead of running
     * {@link #preInit()}/{@link #init()} reflectively. Returns false when no
     * index resource exists, none of the resources has a section for this
     * provider, or {@link Flags#debugNoStubIdx} is set — callers fall back to
     * the scan. A present-but-malformed or version-incompatible index is a
     * hard failure: the api jar is corrupt or mismatched.
     */
    private boolean loadStubIndex(ClassDowngrader downgrader) throws IOException {
        if (downgrader.flags.debugNoStubIdx) return false;
        ClassLoader cl = getClass().getClassLoader();
        Enumeration<URL> resources = cl == null
            ? ClassLoader.getSystemResources(StubIndex.RESOURCE)
            : cl.getResources(StubIndex.RESOURCE);
        String myName = providerKey();
        while (resources.hasMoreElements()) {
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(Utils.openUncached(resources.nextElement())))) {
                StubIndex.Header header = StubIndex.readHeader(in);
                checkIndexVersion(header);
                StubIndex.Snapshot snapshot = StubIndex.read(in, this::resolveStubClass).get(myName);
                if (snapshot != null) {
                    StubIndex.apply(snapshot, catalog);
                    // materialize ClassMapping resolver nodes exactly like the
                    // scan does — enumerators (coverage, corpus, debug dumps)
                    // read stubMappings, not stubRegistry
                    Set<String> warnings = new LinkedHashSet<>();
                    for (String ownerName : snapshot.owners.keySet()) {
                        getStubMapper(Type.getObjectType(ownerName), warnings);
                    }
                    if (!warnings.isEmpty() && logger.is(Logger.Level.WARN)) {
                        StringBuilder sb = new StringBuilder();
                        for (String warning : warnings) {
                            sb.append("\t").append(warning).append("\n");
                        }
                        logger.warn("Warnings for " + myName + " stub index (" + warnings.size() + ") : \n" + sb);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /** fail-fast gate: index format must be supported, jvmdg major.minor must match the running version */
    private static void checkIndexVersion(StubIndex.Header header) throws IOException {
        if (header.formatVersion != StubIndex.FORMAT_VERSION) {
            throw new IOException("stubs.idx format " + header.formatVersion
                + " is not supported by this jvmdg (" + StubIndex.FORMAT_VERSION + ")");
        }
        String runtime = Flags.jvmdgVersion;
        if (runtime == null) return; // unpackaged dev build — nothing to check against
        String[] idx = header.jvmdgVersion.split("\\.", 3);
        String[] run = runtime.split("\\.", 3);
        if (idx.length < 2 || run.length < 2 || !idx[0].equals(run[0]) || !idx[1].equals(run[1])) {
            throw new IOException("stubs.idx built for jvmdg " + header.jvmdgVersion
                + " is incompatible with running jvmdg " + runtime);
        }
    }

    /** lazy {@link MethodRef} lookup: handler/stub classes resolve through this provider's own classloader */
    private Class<?> resolveStubClass(String className) {
        try {
            return Class.forName(className, false, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("stubs.idx references unloadable class " + className, e);
        }
    }

    private void rethrowInitFailure() {
        Utils.<RuntimeException>sneakyThrow(initFailure);
    }

    /**
     * Per-class boundary prologue shared by {@link #downgrade} and the
     * steps path ({@code ProviderInitTransform}): stamps the
     * {@link JvmDowngraded} provenance annotation on first pass, rewrites
     * the class version to this boundary's output, and injects the
     * java-api module requirement into {@code module-info}. Runs after
     * {@link #ensureInit} — reads {@link #downgrader}'s flags.
     */
    public ClassNode initClass(ClassNode clazz) {
        if (!hasProvenance(clazz)) {
            // first boundary — clazz.version is still the authored major
            // (masked: a preview class carries minor 0xFFFF, and `from` is
            // compared against plain majors by the reflection runtime);
            // record it before the rewrite below so reflection stubs can
            // answer in the callsite's emitted vocabulary at runtime
            if (clazz.visibleAnnotations == null) {
                clazz.visibleAnnotations = new ArrayList<>();
            }
            AnnotationNode provenance = new AnnotationNode(Type.getDescriptor(JvmDowngraded.class));
            provenance.values = Arrays.asList(
                "from", Utils.majorClassVersion(clazz.version),
                "to", downgrader.flags.classVersion);
            clazz.visibleAnnotations.add(provenance);
        }
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
        return clazz;
    }

    /** already stamped (a later boundary, or an upstream transformer) — keep the first-seen {@code from} */
    private static boolean hasProvenance(ClassNode node) {
        if (node.visibleAnnotations == null) {
            return false;
        }
        String desc = Type.getDescriptor(JvmDowngraded.class);
        for (AnnotationNode an : node.visibleAnnotations) {
            if (desc.equals(an.desc)) {
                return true;
            }
        }
        return false;
    }

    public ClassNode downgrade(final ClassDowngrader downgrader, ClassNode clazz, final Set<ClassNode> extra, final boolean enableRuntime, final Function<String, ClassNode> getReadOnly) throws IOException {
        String className = clazz.name;

        // major-compare: a preview class carries minor 0xFFFF and belongs to
        // its major's boundary (reaching here means allowPreview let it in)
        if (Utils.majorClassVersion(clazz.version) != inputVersion)
            throw new IllegalArgumentException("Class " + className + " is not version " + inputVersion);

        ensureInit(downgrader);
        final Set<String> warnings = new LinkedHashSet<>();

        clazz = initClass(clazz);

        final IOFunction<Type, Set<MemberNameAndDesc>> getMembers = memberResolver(extra, getReadOnly, warnings);
        final IOFunction<Type, List<Pair<Type, Boolean>>> getSuperTypes = superTypeResolver(extra, getReadOnly, warnings);

        clazz = stubClasses(clazz, enableRuntime, getMembers, getSuperTypes, warnings);
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
                return insertAbstractMethods(classNode, extra, enableRuntime, warnings, getMembers, getSuperTypes);
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
        return insertAbstractMethods(clazz, extra, false, null, getMembers, getSuperTypes);
    }

    /**
     * {@link #insertAbstractMethods(ClassNode, Set, IOFunction, IOFunction)}
     * plus the gained-supertype bridge pass: a class whose supertype
     * closure contains a gained-row member {@code T} is-a {@code I} at the
     * authored version (the interface lands in {@code interfaces} during
     * {@link #stubClasses}), so every {@code gained-calls} member it
     * doesn't serve natively needs a bridge to the registered impl —
     * {@code invokeinterface I.m} on it otherwise throws
     * {@link AbstractMethodError} where the authored JDK dispatches the
     * default/inherited impl. {@link ClassMapping#getStubFor} supplies
     * both gates at once: it returns {@code null} when the class or a
     * supertype declares the member natively (incl. {@code excludeChild}
     * children), and the impl {@link MethodRef} to call otherwise —
     * bridges never route through {@code $jvmdg$dispatch} since the
     * bridge's own declaration would mark the class and recurse.
     */
    public ClassNode insertAbstractMethods(ClassNode clazz, Set<ClassNode> extra, boolean enableRuntime, Set<String> warnings, IOFunction<Type, Set<MemberNameAndDesc>> getMembers, IOFunction<Type, List<Pair<Type, Boolean>>> getSuperTypes) throws IOException {
        if (clazz.name.equals("module-info")) {
            return clazz;
        }
        ClassMapping cm = getStubMapper(Type.getObjectType(clazz.name), (clazz.access & Opcodes.ACC_INTERFACE) != 0, getMembers, getSuperTypes);
        Map<MemberNameAndDesc, StubEntry> members = cm.getAbstracts();
        for (Map.Entry<MemberNameAndDesc, StubEntry> member : members.entrySet()) {
            if (declaresMethod(clazz, member.getKey().getName(), member.getKey().getDesc().getDescriptor())) {
                continue;
            }
            emitBridge(clazz, member.getKey().getName(), member.getKey().getDesc(), member.getValue());
        }
        if ((clazz.access & Opcodes.ACC_INTERFACE) == 0 && !gained.isEmpty() && !gainedMembers.isEmpty()) {
            Set<String> walked = supertypeClosure(cm);
            for (Map.Entry<String, Set<String>> g : gained.entrySet()) {
                // same closure-membership gate as stubClasses' injection —
                // the interface being in walked through its own gained edge
                // must not suppress bridges
                if (Collections.disjoint(g.getValue(), walked)) {
                    continue;
                }
                Set<String> mems = gainedMembers.get(g.getKey());
                if (mems == null) {
                    continue;
                }
                for (String key : new TreeSet<>(mems)) {
                    int sp = key.indexOf(' ');
                    MemberNameAndDesc member = cm.unstubMember(key.substring(0, sp),
                        Type.getMethodType(key.substring(sp + 1)));
                    StubEntry entry = cm.getStubFor(member, enableRuntime, Opcodes.INVOKEVIRTUAL, warnings == null ? new HashSet<String>() : warnings);
                    if (entry == null) {
                        continue;
                    }
                    String desc = stubClass(member.getDesc(), warnings).getDescriptor();
                    if (declaresMethod(clazz, member.getName(), desc)) {
                        continue;
                    }
                    emitBridge(clazz, member.getName(), Type.getMethodType(desc), entry);
                }
            }
        }
        return clazz;
    }

    /** internal names of every supertype in {@code cm}'s resolver closure — the membership set both the gained-interface injection ({@link #stubClasses}) and the gained-bridge pass ({@link #insertAbstractMethods}) gate on; the two must agree, so they share the walk */
    private Set<String> supertypeClosure(ClassMapping cm) {
        Set<String> walked = new HashSet<>();
        Deque<ClassMapping> walk = new ArrayDeque<>(cm.getParents());
        Set<ClassMapping> seen = new HashSet<>();
        while (!walk.isEmpty()) {
            ClassMapping parent = walk.removeFirst();
            if (!seen.add(parent)) {
                continue;
            }
            walked.add(unstubClass(parent.current).getInternalName());
            walk.addAll(parent.getParents());
        }
        return walked;
    }

    /** {@code clazz} declares {@code name desc} — used to skip bridges the class already serves */
    private static boolean declaresMethod(ClassNode clazz, String name, String desc) {
        for (MethodNode method : clazz.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return true;
            }
        }
        return false;
    }

    /** insert the public impl-calling bridge for {@code member}/{@code entry} — shared by the abstractDefault and gained passes */
    private void emitBridge(ClassNode clazz, String name, Type desc, StubEntry entry) {
        MethodVisitor mv = clazz.visitMethod(Opcodes.ACC_PUBLIC | (downgrader.flags.debugNoSynthetic ? 0 : Opcodes.ACC_SYNTHETIC), name, desc.getDescriptor(), null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        int i = 1;
        for (Type param : desc.getArgumentTypes()) {
            mv.visitVarInsn(param.getOpcode(Opcodes.ILOAD), i);
            i += param.getSize();
        }
        if (entry.downgradeVersion) {
            mv.visitLdcInsn(this.inputVersion);
        }
        MethodRef handler = entry.handler;
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, handler.member.getOwner().getInternalName(), handler.member.getName(), handler.member.getDesc().getDescriptor(), handler.itf);
        mv.visitInsn(desc.getReturnType().getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Ordered boundary transform units run by the fullest
     * {@link #otherTransforms} arity after the legacy arity chain — populated
     * in the provider constructor (bundled providers' generated {@code
     * <init>} adds the {@code jvmdg/j<N>/transform/} impls the classpath
     * enumeration found at api-jar build time) and sorted at the end of
     * {@link #ensureInit} by {@link BoundaryTransform#order()} then class
     * name. Overriding {@code otherTransforms} at any arity remains the
     * SPI escape hatch; the chain still delegates to identity.
     */
    public List<BoundaryTransform> boundaryTransforms = new ArrayList<>();

    /**
     * The {@code j<N>} tag naming this provider's version-scope — {@code j}
     * + {@code major(inputVersion) - 44} ({@code V1_7} → {@code j7}, {@code
     * V25} → {@code j25}). Names the per-version generated resources
     * ({@code META-INF/jvmdg/transforms/}, {@code META-INF/jvmdg/stubs/})
     * and the {@code jvmdg.j<N>} source-set packages they mirror.
     */
    public String versionTag() {
        return "j" + (Utils.majorClassVersion(inputVersion) - 44);
    }

    public ClassNode otherTransforms(ClassNode clazz, Set<ClassNode> extra, Function<String, ClassNode> getReadOnly, Set<String> warnings, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver) throws IOException {
        clazz = otherTransforms(clazz, extra, getReadOnly, warnings);
        for (BoundaryTransform transform : boundaryTransforms) {
            clazz = transform.apply(this, clazz, extra, getReadOnly, warnings, enableRuntime, memberResolver, superTypeResolver);
        }
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
        try {
            return stubClasses(clazz, enableRuntime, null, null, warnings);
        } catch (IOException e) {
            // unreachable — a null resolver pair skips the resolution walk
            throw new RuntimeException(e);
        }
    }

    /**
     * {@link #stubClasses(ClassNode, boolean, Set)} plus the resolver pair
     * {@code $Ov$} marker injection needs: the class's supertype closure is
     * walked for stubbed members, and each member the class declares (same
     * name + argument types, non-static non-private — the authored descs are
     * still pre-remap at this point, matching the registered member keys)
     * adds the marker to {@code interfaces} so the generated dispatch's
     * {@code instanceof} sees the override. Null resolvers skip the walk —
     * unit tests that exercise only the class-stub remap pass none.
     */
    public ClassNode stubClasses(ClassNode clazz, boolean enableRuntime, IOFunction<Type, Set<MemberNameAndDesc>> memberResolver, IOFunction<Type, List<Pair<Type, Boolean>>> superTypeResolver, Set<String> warnings) throws IOException {
        if (clazz.name.equals("module-info")) {
            return clazz;
        }

        // jvmdg marker interfaces restoring retro-supertype membership:
        // `class C implements X` / `extends X` loses X to the target remap,
        // so `x instanceof X` could never answer true again — the marker's
        // name is a member of X's retrotypes row, restoring it. SUPERTYPE
        // only; OWN_INTERFACE keeps the stub interface itself, whose name
        // is already in the row.
        Set<String> markers = new LinkedHashSet<>();
        // gained interfaces injected into the interfaces list below —
        // recorded for the @GainedInterfaces annotation at the tail
        Set<String> injectedInterfaces = new LinkedHashSet<>();
        Type type = clazz.superName == null ? null : Type.getObjectType(clazz.superName);
        if (type != null && classStubs.containsKey(type)) {
            ClassStubEntry stub = classStubs.get(type);
            clazz.superName = stub.target.getInternalName();
            if (stub.mode == Adapter.Mode.SUPERTYPE) {
                markers.add(stub.stubType.getInternalName() + "$Marker");
            }
        }

        List<String> removedInterfaces = new ArrayList<>();
        if (clazz.interfaces != null) {
            for (int i = 0; i < clazz.interfaces.size(); i++) {
                type = Type.getObjectType(clazz.interfaces.get(i));
                if (classStubs.containsKey(type)) {
                    ClassStubEntry stub = classStubs.get(type);
                    if (stub.keepInterface) {
                        clazz.interfaces.set(i, stub.target.getInternalName());
                    } else {
                        removedInterfaces.add(clazz.interfaces.remove(i--));
                    }
                    if (stub.mode == Adapter.Mode.SUPERTYPE) {
                        markers.add(stub.stubType.getInternalName() + "$Marker");
                    }
                }
            }
        }
        if (!markers.isEmpty()) {
            if (clazz.interfaces == null) {
                clazz.interfaces = new ArrayList<>();
            }
            for (String marker : markers) {
                if (!clazz.interfaces.contains(marker)) {
                    clazz.interfaces.add(marker);
                }
            }
        }

        // gained supertypes: a supertype in the closure gained an
        // interface at this boundary (e.g. ThreadPoolExecutor became
        // AutoCloseable at 63) — the membership walk feeds the injection
        // below. Instance-stub dispatch needs no marker: the indy callsite
        // resolves the receiver's declared member at runtime.
        if (memberResolver != null && superTypeResolver != null) {
            ClassMapping mapping = getStubMapper(Type.getObjectType(clazz.name),
                (clazz.access & Opcodes.ACC_INTERFACE) != 0, memberResolver, superTypeResolver);
            Set<String> walked = supertypeClosure(mapping);
            // inject the interface into the class so
            // instanceof/checkcast/invokeinterface verify like the authored
            // JDK. The interface is real at the final target by the
            // DerivedGained window filter, so it lands unremapped. The
            // resolver sees the source-version hierarchy where the gained
            // member already IS the interface, so a member in the closure is
            // the only signal — it fires even when the interface is also in
            // walked through that same edge (a native path without a gained
            // member makes the class natively-is-a and the row never
            // intersects). Members the class doesn't serve get impl bridges
            // in insertAbstractMethods.
            if (!gained.isEmpty()) {
                if (clazz.interfaces == null) {
                    clazz.interfaces = new ArrayList<>();
                }
                for (Map.Entry<String, Set<String>> g : gained.entrySet()) {
                    String iface = g.getKey();
                    if (!clazz.interfaces.contains(iface) && !Collections.disjoint(g.getValue(), walked)) {
                        clazz.interfaces.add(iface);
                        injectedInterfaces.add(iface);
                    }
                }
            }
        }

        if (clazz.innerClasses != null) {
            for (InnerClassNode inner : clazz.innerClasses) {
                type = Type.getObjectType(inner.name);
                if (classStubs.containsKey(type)) {
                    ClassStubEntry stub = classStubs.get(type);
                    inner.name = stub.target.getInternalName();
                }
                if (inner.outerName != null) {
                    type = Type.getObjectType(inner.outerName);
                    if (classStubs.containsKey(type)) {
                        ClassStubEntry stub = classStubs.get(type);
                        inner.outerName = stub.target.getInternalName();
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

        // gained interfaces injected above are transform artifacts the
        // source never declared — record them so the getInterfaces stubs
        // can filter them back out of reflection results. Only the gained
        // entries land here: generated $Marker/$Ov$ names are filtered by
        // name pattern at runtime (survives shade prefixing), and unlike
        // RemovedInterfaces these are real JDK names — never remapped.
        if (!injectedInterfaces.isEmpty()) {
            List<String> injected = new ArrayList<>();
            for (String injectedInterface : injectedInterfaces) {
                injected.add(Type.getObjectType(injectedInterface).getClassName());
            }
            boolean found = false;
            if (clazz.visibleAnnotations != null) {
                for (AnnotationNode an : clazz.visibleAnnotations) {
                    if (an.desc.equals(Type.getType(GainedInterfaces.class).getDescriptor())) {
                        ((List<String>) an.values.get(1)).addAll(injected);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                AnnotationNode an = new AnnotationNode(Type.getType(GainedInterfaces.class).getDescriptor());
                an.values = new ArrayList<>();
                an.values.add("value");
                an.values.add(injected);
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

        // remap collapse detection: two members that were distinct at the
        // source version can land on the same erased descriptor after the
        // type remap (e.g. overloads on X and a type stubbed to X) — that's
        // a hard error, not a silent first-one-wins
        Map<String, String> seenFields = new HashMap<>();
        Map<String, String> seenMethods = new HashMap<>();

        if (clazz.fields != null) {
            for (FieldNode field : clazz.fields) {
                type = Type.getType(field.desc);
                String origDesc = field.desc;
                field.desc = stubClass(type, warnings).getDescriptor();
                checkMemberCollision(seenFields, clazz.name + "." + field.name, origDesc, field.desc);
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
                String origDesc = method.desc;
                method.desc = stubClass(type, warnings).getDescriptor();
                checkMemberCollision(seenMethods, clazz.name + "." + method.name, origDesc, method.desc);
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

    /**
     * Records {@code origDesc → postDesc} under {@code memberFqn}; two
     * distinct source members remapping to the same erased name+desc is a
     * hard error naming both — the JVM would see a duplicate member.
     * Identical source signatures re-declared are pre-existing dups, not
     * remap-induced, and pass through.
     */
    private static void checkMemberCollision(Map<String, String> seen, String memberFqn, String origDesc, String postDesc) {
        String key = memberFqn + " " + postDesc;
        String prev = seen.putIfAbsent(key, origDesc);
        if (prev != null && !prev.equals(origDesc)) {
            throw new IllegalStateException("member desc collision after type remap in " + memberFqn +
                ": '" + prev + "' and '" + origDesc + "' both became '" + postDesc + "'");
        }
    }

    public <E extends AnnotationNode> void transformAnnotations(List<E> annotations, final Set<String> warnings) {
        if (annotations == null) return;
        for (E annotation : new ArrayList<>(annotations)) {
            Type annotationType = Type.getType(annotation.desc);
            if (classStubs.containsKey(annotationType)) {
                ClassStubEntry stub = classStubs.get(annotationType);
                if (stub.keepInterface) {
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


}
