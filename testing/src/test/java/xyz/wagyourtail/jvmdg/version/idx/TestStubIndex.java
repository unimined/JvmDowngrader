package xyz.wagyourtail.jvmdg.version.idx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.ClassStubEntry;
import xyz.wagyourtail.jvmdg.version.map.CtorEntry;
import xyz.wagyourtail.jvmdg.version.map.CtorFactory;
import xyz.wagyourtail.jvmdg.version.map.CtorRewrite;
import xyz.wagyourtail.jvmdg.version.map.DispatchSites;
import xyz.wagyourtail.jvmdg.version.map.FieldStubEntry;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MethodRef;
import xyz.wagyourtail.jvmdg.version.map.StubEntry;
import xyz.wagyourtail.jvmdg.version.map.StubRegistry;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class TestStubIndex {

    @Adapter(value = "com/example/Target")
    static class TargetStub {
        public static boolean jvmdg$instanceof(Object o) {
            return true;
        }

        public static Object jvmdg$checkcast(Object o) {
            return o;
        }

        @Stub(ref = @Ref(value = "Lcom/example/Target;", member = "size", desc = "()I"))
        public static int size(TargetStub self) {
            return 0;
        }
    }

    static class ModifyStub {
        @Modify(ref = @Ref(value = "Lcom/example/Target;", member = "hook", desc = "()V"))
        public static void hook(MethodNode method, int index, ClassNode classNode, Set extra, boolean noSynthetic) {
        }
    }

    static class TestProvider extends VersionProvider {
        TestProvider() {
            super(Opcodes.V11, Opcodes.V1_8, 0);
        }

        @Override
        public void preInit() {
        }

        @Override
        public void init() {
        }
    }

    private static MethodRef ref(String owner, String name, String desc, int... jeps) {
        return new MethodRef(
            new FullyQualifiedMemberNameAndDesc(Type.getObjectType(owner), name, Type.getMethodType(desc)),
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, false, jeps);
    }

    private static StubEntry entry(String owner, String name, String desc, int... jeps) {
        return new StubEntry(ref(owner, name, desc, jeps), false, true, false, false, new String[0]);
    }

    /** registers every serializable entry shape: stubs, fields, ctor factory + rewrite, keyed + wildcard modify */
    private static TestProvider populatedProvider(ClassDowngrader d) {
        TestProvider p = new TestProvider();
        p.ensureInit(d);
        p.stub(TargetStub.class);
        p.stub(ModifyStub.class);
        StubRegistry.Owner owner = p.stubRegistry.forOwner(Type.getObjectType("com/example/Target"));
        owner.methodStub.put(new MemberNameAndDesc("name", Type.getMethodType("()Ljava/lang/String;")).ignoreReturnValue(),
            new StubEntry(ref("x/H", "name", "(Lcom/example/Target;)Ljava/lang/String;"), true, false, false, true,
                new String[]{"com/example/Child"}));
        // a callerClass entry — the forName shape — so the flag round-trips
        owner.methodStub.put(new MemberNameAndDesc("load", Type.getMethodType("(Ljava/lang/String;)Ljava/lang/Class;")),
            new StubEntry(ref("x/H", "load", "(Ljava/lang/String;Ljava/lang/Class;I)Ljava/lang/Class;"),
                true, true, false, false, false, new String[0]));
        // a PREVIEW-conditioned member + ctor so the condition bits round-trip
        owner.methodStub.put(new MemberNameAndDesc("previewOp", Type.getMethodType("()V")),
            new StubEntry(ref("x/H", "previewOp", "(Lcom/example/Target;)V"), false, false,
                xyz.wagyourtail.jvmdg.version.StubCondition.PREVIEW, false, false, new String[0]));
        owner.ctor.put(new MemberNameAndDesc("<init>", Type.getMethodType("(Z)V")),
            new CtorFactory(ref("x/H", "createPreview", "(Z)Lcom/example/Target;"), false,
                xyz.wagyourtail.jvmdg.version.StubCondition.PREVIEW));
        owner.fieldStub.put(new MemberNameAndDesc("count", Type.getType("I")),
            new FieldStubEntry(entry("x/H", "getCount", "(Lcom/example/Target;)I"),
                entry("x/H", "setCount", "(Lcom/example/Target;I)V")));
        owner.ctor.put(new MemberNameAndDesc("<init>", Type.getMethodType("(I)V")),
            new CtorFactory(ref("x/H", "create", "(I)Lcom/example/Target;"), true, false));
        owner.ctor.put(new MemberNameAndDesc("<init>", Type.getMethodType("(Ljava/lang/String;II)V")),
            new CtorRewrite(new Type[]{Type.getType("Ljava/lang/String;")}, new int[]{0},
                new MethodRef[]{null, ref("x/H", "cap", "(I)I"), null},
                new int[][]{null, new int[]{1}, null},
                ref("x/H", "before", "()V"), new int[0],
                ref("x/H", "after", "()V"), new int[]{0},
                false, true));
        owner.memberModify.put(new MemberNameAndDesc("call", Type.getMethodType("()V")), ref("x/M", "m1", "()V"));
        owner.memberModify.put(null, ref("x/M", "all", "()V"));
        owner.indyDesugars.put(new MemberNameAndDesc("bsm", Type.getMethodType("()Ljava/lang/invoke/CallSite;")), ref("x/D", "desugar", "()V"));
        p.stubRegistry.memberSince.put(
            new FullyQualifiedMemberNameAndDesc(Type.getObjectType("com/example/Target"), "name",
                Type.getMethodType("()Ljava/lang/String;")), 52);
        p.stubRegistry.memberKind.put(
            new FullyQualifiedMemberNameAndDesc(Type.getObjectType("com/example/Target"), "name",
                Type.getMethodType("()Ljava/lang/String;")),
            xyz.wagyourtail.jvmdg.version.StubKind.LANGUAGE);
        return p;
    }

    @TempDir
    Path tempDir;

    private static StubIndex.Snapshot roundTrip(VersionProvider source) throws IOException {
        Path file = Files.createTempFile("stubs", ".idx");
        StubIndex.write(file, "3.0.0", Collections.singletonList(source));
        return StubIndex.read(Files.newInputStream(file), null).get(source.getClass().getName());
    }

    private static void assertFQMND(FullyQualifiedMemberNameAndDesc a, FullyQualifiedMemberNameAndDesc b) {
        if (a == null) {
            assertNull(b);
            return;
        }
        assertNotNull(b);
        assertEquals(a.getOwner().getInternalName(), b.getOwner().getInternalName());
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getDesc().getDescriptor(), b.getDesc().getDescriptor());
        assertEquals(a.isIgnoreReturnValue(), b.isIgnoreReturnValue());
    }

    private static void assertRef(MethodRef a, MethodRef b) {
        if (a == null) {
            assertNull(b);
            return;
        }
        assertNotNull(b);
        assertFQMND(a.member, b.member);
        assertEquals(a.modifiers, b.modifiers);
        assertEquals(a.itf, b.itf);
        assertArrayEquals(a.jeps, b.jeps);
    }

    private static void assertEntry(StubEntry a, StubEntry b) {
        if (a == null) {
            assertNull(b);
            return;
        }
        assertNotNull(b);
        assertRef(a.handler, b.handler);
        assertEquals(a.downgradeVersion, b.downgradeVersion);
        assertEquals(a.callerClass, b.callerClass);
        assertEquals(a.requiresRuntime, b.requiresRuntime);
        assertEquals(a.condition, b.condition);
        assertEquals(a.abstractDefault, b.abstractDefault);
        assertEquals(a.noSpecial, b.noSpecial);
        assertArrayEquals(a.excludeChild, b.excludeChild);
    }

    private static void assertCtor(CtorEntry a, CtorEntry b) {
        assertEquals(a.getClass(), b.getClass());
        assertEquals(a.requiresRuntime, b.requiresRuntime);
        assertEquals(a.condition, b.condition);
        if (a instanceof CtorFactory) {
            CtorFactory fa = (CtorFactory) a;
            CtorFactory fb = (CtorFactory) b;
            assertRef(fa.handler, fb.handler);
            assertEquals(fa.downgradeVersion, fb.downgradeVersion);
        } else {
            CtorRewrite ra = (CtorRewrite) a;
            CtorRewrite rb = (CtorRewrite) b;
            assertEquals(ra.downgradeVersion, rb.downgradeVersion);
            assertEquals(ra.toArgs.length, rb.toArgs.length);
            for (int i = 0; i < ra.toArgs.length; i++) assertEquals(ra.toArgs[i].getDescriptor(), rb.toArgs[i].getDescriptor());
            assertArrayEquals(ra.keep, rb.keep);
            assertEquals(ra.argMethods.length, rb.argMethods.length);
            for (int i = 0; i < ra.argMethods.length; i++) {
                assertRef(ra.argMethods[i], rb.argMethods[i]);
                assertArrayEquals(ra.argFrom[i], rb.argFrom[i]);
            }
            assertRef(ra.pre, rb.pre);
            assertArrayEquals(ra.preFrom, rb.preFrom);
            assertRef(ra.post, rb.post);
            assertArrayEquals(ra.postFrom, rb.postFrom);
        }
    }

    private static void assertOwner(StubRegistry.Owner a, StubRegistry.Owner b) {
        assertEquals(a.methodStub.keySet(), b.methodStub.keySet());
        for (Map.Entry<MemberNameAndDesc, StubEntry> e : a.methodStub.entrySet()) {
            assertEntry(e.getValue(), b.methodStub.get(e.getKey()));
        }
        assertEquals(a.fieldStub.keySet(), b.fieldStub.keySet());
        for (Map.Entry<MemberNameAndDesc, FieldStubEntry> e : a.fieldStub.entrySet()) {
            assertEntry(e.getValue().getter, b.fieldStub.get(e.getKey()).getter);
            assertEntry(e.getValue().setter, b.fieldStub.get(e.getKey()).setter);
        }
        assertEquals(a.ctor.keySet(), b.ctor.keySet());
        for (Map.Entry<MemberNameAndDesc, CtorEntry> e : a.ctor.entrySet()) {
            assertCtor(e.getValue(), b.ctor.get(e.getKey()));
        }
        assertEquals(a.memberModify.size(), b.memberModify.size());
        for (Map.Entry<MemberNameAndDesc, MethodRef> e : a.memberModify.entrySet()) {
            assertTrue(b.memberModify.containsKey(e.getKey()), "missing modify for " + e.getKey());
            assertRef(e.getValue(), b.memberModify.get(e.getKey()));
        }
        assertEquals(a.indyDesugars.size(), b.indyDesugars.size());
        for (Map.Entry<MemberNameAndDesc, MethodRef> e : a.indyDesugars.entrySet()) {
            assertTrue(b.indyDesugars.containsKey(e.getKey()), "missing indyDesugar for " + e.getKey());
            assertRef(e.getValue(), b.indyDesugars.get(e.getKey()));
        }
    }

    private static void assertClassStub(ClassStubEntry a, ClassStubEntry b) {
        assertEquals(a.target.getInternalName(), b.target.getInternalName());
        assertEquals(a.stubType.getInternalName(), b.stubType.getInternalName());
        assertEquals(a.keepInterface, b.keepInterface);
        assertEquals(a.itf, b.itf);
        assertFQMND(a.instanceofHook, b.instanceofHook);
        assertFQMND(a.checkcastHook, b.checkcastHook);
        assertArrayEquals(a.jeps, b.jeps);
        assertEquals(a.mode, b.mode);
        assertFQMND(a.wrap, b.wrap);
        assertFQMND(a.polymorphic, b.polymorphic);
        assertEquals(a.polymorphicNames, b.polymorphicNames);
        // implementedBy: the annotation-declared set is identical on both
        // paths, but the idx side additionally carries build-derived
        // implementors (populateImplementedBy) the scan can't see — so the
        // valid relation is one side ⊆ the other, not equality
        assertTrue(a.implementedBy.containsAll(b.implementedBy)
                || b.implementedBy.containsAll(a.implementedBy),
            "implementedBy sets not subset-related: " + a.implementedBy + " vs " + b.implementedBy);
    }

    private static void assertRegistryEqual(VersionProvider source, StubIndex.Snapshot snap) {
        assertNotNull(snap, "missing provider section for " + source.getClass().getName());
        assertEquals(source.classStubs.keySet(), snap.classStubs.keySet());
        for (Map.Entry<Type, ClassStubEntry> e : source.classStubs.entrySet()) {
            assertClassStub(e.getValue(), snap.classStubs.get(e.getKey()));
        }
        Map<String, StubRegistry.Owner> a = source.stubRegistry.owners();
        assertEquals(a.keySet(), snap.owners.keySet());
        for (Map.Entry<String, StubRegistry.Owner> e : a.entrySet()) {
            assertOwner(e.getValue(), snap.owners.get(e.getKey()));
        }
        assertEquals(source.stubRegistry.memberSince, snap.memberSince, "memberSince differs");
        assertEquals(source.stubRegistry.memberKind, snap.memberKind, "memberKind differs");
    }

    /** kind classification: override > memberModify=LANGUAGE > since vs target */
    @Test
    public void kindClassification() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider p = populatedProvider(d);
            StubRegistry reg = p.stubRegistry;
            FullyQualifiedMemberNameAndDesc site =
                new FullyQualifiedMemberNameAndDesc(Type.getObjectType("com/example/Target"),
                    "name", Type.getMethodType("()Ljava/lang/String;"));
            // explicit override wins regardless of since/target
            assertEquals(xyz.wagyourtail.jvmdg.version.StubKind.LANGUAGE,
                reg.kindOf(site, false, Opcodes.V1_8));
            reg.memberKind.remove(site);
            // since=52 exists on a V1_8 target -> BEHAVIOR; on V1_7 -> MISSING_API
            assertEquals(xyz.wagyourtail.jvmdg.version.StubKind.BEHAVIOR,
                reg.kindOf(site, false, Opcodes.V1_8));
            assertEquals(xyz.wagyourtail.jvmdg.version.StubKind.MISSING_API,
                reg.kindOf(site, false, Opcodes.V1_7));
            // memberModify entries are LANGUAGE
            assertEquals(xyz.wagyourtail.jvmdg.version.StubKind.LANGUAGE,
                reg.kindOf(new FullyQualifiedMemberNameAndDesc(
                    Type.getObjectType("com/example/Target"), "call", Type.getMethodType("()V")),
                    true, Opcodes.V1_8));
            // unknown since -> MISSING_API on every target
            assertEquals(xyz.wagyourtail.jvmdg.version.StubKind.MISSING_API,
                reg.kindOf(new FullyQualifiedMemberNameAndDesc(
                    Type.getObjectType("java/lang/String"), "nope", Type.getMethodType("()V")),
                    false, Opcodes.V11));
        }
    }

    /** the built api jar's idx carries ct.sym-derived since data for real members */
    @Test
    public void apiJarIndexCarriesMemberSince() throws Exception {
        try (ClassDowngrader d = apiJarDowngrader()) {
            int found = 0;
            for (VersionProvider vp : d.versionProviders(Opcodes.V9)) {
                vp.ensureInit(d);
                found += vp.stubRegistry.memberSince.size();
            }
            assertTrue(found > 0, "no memberSince data in the api jar index");
        }
    }

    /**
     * Reflection members the api jar intercepts exist on every supported
     * target, so their rows classify {@link xyz.wagyourtail.jvmdg.version.StubKind#BEHAVIOR}
     * — letting {@code noBehavior} opt them out instead of masquerading as
     * missing API.
     */
    @Test
    public void apiJarReflectionMembersAreBehavior() throws Exception {
        try (ClassDowngrader d = apiJarDowngrader()) {
            int checked = 0;
            for (VersionProvider vp : d.versionProviders(Opcodes.V9)) {
                vp.ensureInit(d);
                for (String[] m : new String[][]{
                    {"forName", "(Ljava/lang/String;)Ljava/lang/Class;"},
                    {"getInterfaces", "()[Ljava/lang/Class;"},
                    {"getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"}}) {
                    FullyQualifiedMemberNameAndDesc site =
                        new FullyQualifiedMemberNameAndDesc(
                            Type.getObjectType("java/lang/Class"), m[0],
                            Type.getMethodType(m[1]));
                    if (vp.stubRegistry.memberSince.containsKey(site)) {
                        assertEquals(
                            xyz.wagyourtail.jvmdg.version.StubKind.BEHAVIOR,
                            vp.stubRegistry.kindOf(site, false, Opcodes.V1_8),
                            m[0] + " should classify BEHAVIOR on a V1_8 target");
                        checked++;
                    }
                }
            }
            assertTrue(checked > 0, "no reflection member rows found in the api jar index");
        }
    }

    @Test
    public void roundTripPreservesRegistry() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider source = populatedProvider(d);
            assertRegistryEqual(source, roundTrip(source));
        }
    }

    @Test
    public void applyRebuildsProviderRegistry() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider source = populatedProvider(d);
            StubIndex.Snapshot snap = roundTrip(source);
            TestProvider loaded = new TestProvider();
            loaded.ensureInit(d); // binds downgrader for shouldSkip, as in the real load path
            StubIndex.apply(snap, loaded);
            assertEquals(source.classStubs.keySet(), loaded.classStubs.keySet());
            assertEquals(source.stubRegistry.owners().keySet(), loaded.stubRegistry.owners().keySet());
            StubIndex.Snapshot resnap = roundTrip(loaded);
            assertRegistryEqual(loaded, resnap);
        }
    }

    @Test
    public void encodingIsDeterministic() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider source = populatedProvider(d);
            Path a = tempDir.resolve("a.idx");
            Path b = tempDir.resolve("b.idx");
            StubIndex.write(a, "3.0.0", Collections.singletonList(source));
            StubIndex.write(b, "3.0.0", Collections.singletonList(source));
            assertArrayEquals(Files.readAllBytes(a), Files.readAllBytes(b));
        }
    }

    @Test
    public void rejectsBadMagic() throws Exception {
        Path bad = tempDir.resolve("bad.idx");
        Files.write(bad, new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 0, 0});
        assertThrows(IOException.class, () -> StubIndex.readHeader(
            new DataInputStream(Files.newInputStream(bad))));
    }

    private static ClassDowngrader apiJarDowngrader() {
        String apiPath = System.getProperty("jvmdg.test.javaApiPath");
        assumeTrue(apiPath != null, "api jar path not set (integration env)");
        Flags flags = new Flags();
        flags.api = Collections.singletonList(new File(apiPath));
        flags.classVersion = Opcodes.V1_8;
        return ClassDowngrader.downgradeTo(flags);
    }

    @Test
    public void providersLoadFromApiJarIndex() throws Exception {
        try (ClassDowngrader d = apiJarDowngrader()) {
            List<VersionProvider> providers = d.versionProviders(Opcodes.V9);
            assertFalse(providers.isEmpty());
            for (VersionProvider vp : providers) {
                vp.ensureInit(d);
                assertTrue(vp.stubIndexLoaded, vp.getClass().getName() + " did not load stubs.idx");
                assertFalse(vp.stubRegistry.owners().isEmpty(),
                    vp.getClass().getName() + " index section populated no owners");
            }
        }
    }

    private static void assertProviderEqual(VersionProvider a, VersionProvider b) {
        assertEquals(a.classStubs.keySet(), b.classStubs.keySet(),
            a.getClass().getName() + " classStubs differ");
        for (Map.Entry<Type, ClassStubEntry> e : a.classStubs.entrySet()) {
            assertClassStub(e.getValue(), b.classStubs.get(e.getKey()));
        }
        Map<String, StubRegistry.Owner> ao = a.stubRegistry.owners();
        assertEquals(ao.keySet(), b.stubRegistry.owners().keySet(),
            a.getClass().getName() + " owners differ");
        for (Map.Entry<String, StubRegistry.Owner> e : ao.entrySet()) {
            assertOwner(e.getValue(), b.stubRegistry.owners().get(e.getKey()));
        }
    }

    /** the oracle: idx-loaded registries must equal the reflective scan's, per provider */
    @Test
    public void indexMatchesReflectiveScan() throws Exception {
        try (ClassDowngrader idx = apiJarDowngrader(); ClassDowngrader scan = apiJarDowngrader()) {
            scan.flags.debugNoStubIdx = true;
            Map<Integer, VersionProvider> scanned = new HashMap<>();
            for (VersionProvider vp : scan.versionProviders(Opcodes.V9)) {
                vp.ensureInit(scan);
                scanned.put(vp.inputVersion, vp);
            }
            for (VersionProvider idxP : idx.versionProviders(Opcodes.V9)) {
                idxP.ensureInit(idx);
                assertTrue(idxP.stubIndexLoaded, idxP.getClass().getName() + " did not load idx");
                VersionProvider scanP = scanned.get(idxP.inputVersion);
                assertNotNull(scanP, "no scanned provider for " + idxP.getClass().getName());
                assertProviderEqual(idxP, scanP);
            }
        }
    }

    @Test
    public void debugNoStubIdxFallsBackToScan() throws Exception {
        try (ClassDowngrader d = apiJarDowngrader()) {
            d.flags.debugNoStubIdx = true;
            for (VersionProvider vp : d.versionProviders(Opcodes.V9)) {
                vp.ensureInit(d);
                assertFalse(vp.stubIndexLoaded, vp.getClass().getName() + " loaded idx despite debugNoStubIdx");
                assertFalse(vp.stubRegistry.owners().isEmpty(),
                    vp.getClass().getName() + " scan fallback registered nothing");
            }
        }
    }

    /**
     * Derived {@code excludeChild}: the build-time ct.sym derivation must
     * reproduce every declared exclusion and add the siblings the manual
     * lists miss. Exercises the scan path ({@code debugNoStubIdx}) so the
     * {@code META-INF/jvmdg/excludes} resource application is covered too.
     */
    @Test
    public void derivedExcludeChildSupersetOfManual() throws Exception {
        try (ClassDowngrader d = apiJarDowngrader()) {
            d.flags.debugNoStubIdx = true;
            Map<Integer, VersionProvider> providers = new HashMap<>();
            for (VersionProvider vp : d.versionProviders(Opcodes.V21)) {
                vp.ensureInit(d);
                providers.put(vp.inputVersion, vp);
            }
            // J_U_R_RandomGenerator.doubles — java/util/Random,
            // SplittableRandom and ThreadLocalRandom all declare doubles()
            // natively and are RandomGenerator subtypes; derivation must
            // cover all three.
            StubEntry doubles = stubAt(providers.get(Opcodes.V17),
                "xyz/wagyourtail/jvmdg/j17/stub/java_base/J_U_R_RandomGenerator",
                "doubles", "()Ljava/util/stream/DoubleStream;");
            assertNotNull(doubles, "no RandomGenerator.doubles stub on the j17 provider");
            assertExcludeContains(doubles.excludeChild,
                "java/util/Random", "java/util/SplittableRandom", "java/util/concurrent/ThreadLocalRandom");
            // CharSequence.isEmpty declared "java/lang/String"; CharBuffer
            // declares isEmpty only since 15 — newer than the j14 target —
            // so it must NOT be excluded (the stub has to cover it).
            StubEntry isEmpty = stubAt(providers.get(Opcodes.V15),
                "java/lang/CharSequence", "isEmpty", "()Z");
            assertNotNull(isEmpty, "no CharSequence.isEmpty stub on the j15 provider");
            assertExcludeContains(isEmpty.excludeChild, "java/lang/String");
            assertFalse(Arrays.asList(isEmpty.excludeChild).contains("java/nio/CharBuffer"),
                "CharBuffer.isEmpty exists only since 15 — exclusion would keep a call the target lacks");
            // Collection.addFirst declared "java/util/Deque" — derivation
            // adds the Deque-family impls that natively declare addFirst.
            StubEntry addFirst = stubAt(providers.get(Opcodes.V21),
                "java/util/Collection", "addFirst", "(Ljava/lang/Object;)V");
            assertNotNull(addFirst, "no Collection.addFirst stub on the j21 provider");
            assertExcludeContains(addFirst.excludeChild,
                "java/util/Deque", "java/util/ArrayDeque", "java/util/LinkedList");
            // List.addFirst is the very member being stubbed (new at 21,
            // absent on the j20 target) — it must not exclude itself.
            assertFalse(Arrays.asList(addFirst.excludeChild).contains("java/util/List"),
                "List.addFirst exists only since 21 — exclusion would keep a call the target lacks");
        }
    }

    /**
     * Derived gained-supertype rows: {@code META-INF/jvmdg/gained} binds each
     * (interface, member) pair to the release the member acquired it, and
     * providers keep only the bindings their boundary actually drops —
     * {@code outputVersion < since <= inputVersion}. Both load paths read the
     * same resource, so idx and scan must agree.
     */
    @Test
    public void gainedRowsScopedToProviderBoundary() throws Exception {
        try (ClassDowngrader idx = apiJarDowngrader(); ClassDowngrader scan = apiJarDowngrader()) {
            scan.flags.debugNoStubIdx = true;
            Map<Integer, VersionProvider> scanned = new HashMap<>();
            for (VersionProvider vp : scan.versionProviders(Opcodes.V26)) {
                vp.ensureInit(scan);
                scanned.put(vp.inputVersion, vp);
            }
            for (VersionProvider vp : idx.versionProviders(Opcodes.V26)) {
                vp.ensureInit(idx);
                VersionProvider scanP = scanned.get(vp.inputVersion);
                assertNotNull(scanP, "no scanned provider for " + vp.getClass().getName());
                assertEquals(scanP.gained, vp.gained,
                    "gained rows differ between idx and scan at " + vp.getClass().getName());
            }
            // HttpClient implements AutoCloseable only since 21 — the V21
            // step owns that binding; the V19 ExecutorService family binding
            // must not bleed into it.
            Set<String> ac21 = scanned.get(Opcodes.V21).gained.get("java/lang/AutoCloseable");
            assertNotNull(ac21, "HttpClient -> AutoCloseable (since 65) must land on the V21 boundary");
            assertTrue(ac21.contains("java/net/http/HttpClient"));
            assertFalse(ac21.contains("java/util/concurrent/ExecutorService"),
                "ExecutorService gained at 63 — native since j19, not the V21 boundary's to rewrite");
            // ExecutorService + its JDK impls acquired AutoCloseable at 19 —
            // only the V19 step (output 62 < 63 <= input 63) rewrites them.
            Set<String> ac19 = scanned.get(Opcodes.V19).gained.get("java/lang/AutoCloseable");
            assertNotNull(ac19, "ExecutorService -> AutoCloseable (since 63) must land on the V19 boundary");
            assertTrue(ac19.contains("java/util/concurrent/ExecutorService"));
            assertTrue(ac19.contains("java/util/concurrent/ThreadPoolExecutor"),
                "impl classes gained transitively at the same release");
            assertFalse(ac19.contains("java/net/http/HttpClient"),
                "HttpClient gains at 65 — must not appear on the V19 boundary");
            // V20->V19 output: ExecutorService is AutoCloseable natively on
            // j19, HttpClient isn't yet — the step's row (if any) may only
            // hold members whose binding appeared exactly at 64.
            Set<String> ac20 = scanned.get(Opcodes.V20).gained.get("java/lang/AutoCloseable");
            if (ac20 != null) {
                assertFalse(ac20.contains("java/util/concurrent/ExecutorService"),
                    "ExecutorService gained at 63 — native at the V20 input already");
                assertFalse(ac20.contains("java/net/http/HttpClient"),
                    "HttpClient gains at 65 — above the V20 boundary's input");
            }
        }
    }

    /**
     * Gained-supertype site routing end-to-end: a V65 class's
     * {@code instanceof}/{@code checkcast}/{@code invokeinterface}/
     * {@code Class}-query sites on {@code AutoCloseable} route through the
     * generated dispatcher with the class's authored version as the
     * ceiling; a non-gained interface's sites stay native.
     */
    @Test
    public void gainedInterfaceSitesRouteThroughDispatcher() throws Exception {
        try (ClassDowngrader d = apiJarDowngrader()) {
            VersionProvider vp = null;
            for (VersionProvider p : d.versionProviders(Opcodes.V21)) {
                p.ensureInit(d);
                if (p.inputVersion == Opcodes.V21) {
                    vp = p;
                }
            }
            assertNotNull(vp, "no V21 provider in the api jar");
            assertTrue(vp.gained.containsKey("java/lang/AutoCloseable"),
                "V21 boundary owns the HttpClient binding");
            String disp = "xyz/wagyourtail/jvmdg/gen/gained/J_L_AutoCloseable";

            ClassNode owner = new ClassNode();
            owner.version = Opcodes.V21;
            owner.access = Opcodes.ACC_PUBLIC;
            owner.name = "com/example/GainedSites";
            owner.superName = "java/lang/Object";
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "f", "(Ljava/lang/Object;)V", null, null);
            m.instructions = new org.objectweb.asm.tree.InsnList();
            m.instructions.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.INSTANCEOF, "java/lang/AutoCloseable"));
            m.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
            m.instructions.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.CHECKCAST, "java/lang/AutoCloseable"));
            m.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
            m.instructions.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "java/lang/AutoCloseable", "close", "()V", true));
            // a non-gained interface's call stays native (receiver + arg:
            // the frame analysis over the whole method must not underflow)
            m.instructions.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "java/lang/Appendable", "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;", true));
            m.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
            m.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(Type.getObjectType("java/lang/AutoCloseable")));
            m.instructions.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "java/lang/Class", "isInstance", "(Ljava/lang/Object;)Z", false));
            m.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
            m.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
            m.maxLocals = 1;
            m.maxStack = 4;
            owner.methods = new ArrayList<>();
            owner.methods.add(m);

            vp.stubMethod(m, owner, new HashSet<ClassNode>(), false,
                t -> Collections.emptySet(), t -> Collections.emptyList(),
                n -> null, new LinkedHashSet<>());

            // collect the emitted invokestatics in order
            List<String> calls = new ArrayList<>();
            for (int i = 0; i < m.instructions.size(); i++) {
                org.objectweb.asm.tree.AbstractInsnNode insn = m.instructions.get(i);
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode &&
                    insn.getOpcode() == Opcodes.INVOKESTATIC) {
                    org.objectweb.asm.tree.MethodInsnNode c =
                        (org.objectweb.asm.tree.MethodInsnNode) insn;
                    calls.add(c.owner + "." + c.name + c.desc);
                }
            }
            assertEquals(Arrays.asList(
                    disp + ".jvmdg$isInstance(Ljava/lang/Object;I)Z",
                    disp + ".jvmdg$cast(Ljava/lang/Object;I)Ljava/lang/AutoCloseable;",
                    disp + ".close$()V(Ljava/lang/Object;I)V",
                    disp + ".jvmdg$isInstance(Ljava/lang/Object;I)Z"),
                calls, "every gained site routes to the interface's dispatcher");
            // the native invokeinterface on the non-gained Appendable survives
            boolean nativeLeft = false;
            for (int i = 0; i < m.instructions.size(); i++) {
                org.objectweb.asm.tree.AbstractInsnNode insn = m.instructions.get(i);
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode &&
                    insn.getOpcode() == Opcodes.INVOKEINTERFACE &&
                    ((org.objectweb.asm.tree.MethodInsnNode) insn).owner.equals("java/lang/Appendable")) {
                    nativeLeft = true;
                }
            }
            assertTrue(nativeLeft, "a non-gained interface's call must stay native");
        }
    }

    /**
     * Override-preserving dispatch end-to-end: an {@code invokeinterface} on
     * a stubbed instance member emits an {@code invokedynamic} — member name,
     * self-first handler desc, {@code DispatchRuntime.bootstrap} carrying the
     * member MethodType + impl Handle — while a static member keeps the impl
     * owner.
     */
    @Test
    public void instanceMemberSitesRouteThroughDispatch() throws Exception {
        try (ClassDowngrader d = apiJarDowngrader()) {
            VersionProvider vp = null;
            for (VersionProvider p : d.versionProviders(Opcodes.V19)) {
                p.ensureInit(d);
                if (p.inputVersion == Opcodes.V19) {
                    vp = p;
                }
            }
            assertNotNull(vp, "no V19 provider in the api jar");
            String stub = "xyz/wagyourtail/jvmdg/j19/stub/java_base/J_U_C_ExecutorService";

            ClassNode owner = new ClassNode();
            owner.version = Opcodes.V19;
            owner.access = Opcodes.ACC_PUBLIC;
            owner.name = "com/example/ExecCalls";
            owner.superName = "java/lang/Object";
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "f", "(Ljava/util/concurrent/ExecutorService;)V", null, null);
            m.instructions = new org.objectweb.asm.tree.InsnList();
            m.instructions.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "java/util/concurrent/ExecutorService", "close", "()V", true));
            m.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
            m.maxLocals = 1;
            m.maxStack = 1;
            owner.methods = new ArrayList<>();
            owner.methods.add(m);

            vp.stubMethod(m, owner, new HashSet<ClassNode>(), false,
                t -> Collections.emptySet(), t -> Collections.emptyList(),
                n -> null, new LinkedHashSet<>());

            List<String> calls = new ArrayList<>();
            org.objectweb.asm.tree.InvokeDynamicInsnNode dispatch = null;
            for (int i = 0; i < m.instructions.size(); i++) {
                org.objectweb.asm.tree.AbstractInsnNode insn = m.instructions.get(i);
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode) {
                    org.objectweb.asm.tree.MethodInsnNode c =
                        (org.objectweb.asm.tree.MethodInsnNode) insn;
                    calls.add(c.getOpcode() + ":" + c.owner + "." + c.name + c.desc);
                } else if (insn instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode) {
                    dispatch = (org.objectweb.asm.tree.InvokeDynamicInsnNode) insn;
                }
            }
            assertEquals(Collections.<String>emptyList(), calls,
                "the instance stub site leaves no direct invoke");
            assertNotNull(dispatch, "the instance stub site must emit an invokedynamic");
            assertEquals("close", dispatch.name);
            assertEquals("(Ljava/util/concurrent/ExecutorService;)V", dispatch.desc);
            assertEquals(Type.getObjectType(stub), dispatch.bsmArgs[1],
                "bsm arg 1 is the impl owner's class");
            assertEquals("close (Ljava/util/concurrent/ExecutorService;)V", dispatch.bsmArgs[2],
                "bsm arg 2 is the impl 'name desc'");
            assertEquals(Type.getMethodType("()V"), dispatch.bsmArgs[0]);
            assertEquals(DispatchSites.RUNTIME, dispatch.bsm.getOwner());
            assertEquals("bootstrap", dispatch.bsm.getName());
        }
    }

    private static StubEntry stubAt(VersionProvider vp, String owner, String name, String desc) {
        StubRegistry.Owner o = vp.stubRegistry.owners().get(owner);
        assertNotNull(o, "no stub registry owner " + owner);
        return o.methodStub.get(new MemberNameAndDesc(name, Type.getMethodType(desc)));
    }

    private static void assertExcludeContains(String[] excludeChild, String... expected) {
        List<String> have = Arrays.asList(excludeChild);
        for (String e : expected) {
            assertTrue(have.contains(e) || have.contains("L" + e + ";"),
                "excludeChild " + have + " missing " + e);
        }
    }

    /** perf gate: loading the index must not be slower than the reflective scan it replaces */
    @Test
    public void idxLoadIsFasterThanReflectiveScan() throws Exception {
        long idxNs = Long.MAX_VALUE, scanNs = Long.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            try (ClassDowngrader d = apiJarDowngrader()) {
                long t0 = System.nanoTime();
                for (VersionProvider vp : d.versionProviders(Opcodes.V9)) {
                    vp.ensureInit(d);
                }
                idxNs = Math.min(idxNs, System.nanoTime() - t0);
            }
            try (ClassDowngrader d = apiJarDowngrader()) {
                d.flags.debugNoStubIdx = true;
                long t0 = System.nanoTime();
                for (VersionProvider vp : d.versionProviders(Opcodes.V9)) {
                    vp.ensureInit(d);
                }
                scanNs = Math.min(scanNs, System.nanoTime() - t0);
            }
        }
        System.out.println("[stubs.idx] provider init: idx=" + idxNs / 1_000_000 + "ms scan=" + scanNs / 1_000_000 + "ms");
        assertTrue(idxNs <= scanNs,
            "idx path slower than scan: idx=" + idxNs / 1_000_000 + "ms scan=" + scanNs / 1_000_000 + "ms");
    }

    @Test
    public void headerCarriesVersions() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(bos);
        o.write(StubIndex.MAGIC);
        o.writeShort(StubIndex.FORMAT_VERSION);
        o.writeUTF("9.9.9-test");
        o.writeShort(0);
        o.flush();
        StubIndex.Header h = StubIndex.readHeader(new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));
        assertEquals(StubIndex.FORMAT_VERSION, h.formatVersion);
        assertEquals("9.9.9-test", h.jvmdgVersion);
    }
}
