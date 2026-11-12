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
        assertEquals(a.requiresRuntime, b.requiresRuntime);
        assertEquals(a.abstractDefault, b.abstractDefault);
        assertEquals(a.noSpecial, b.noSpecial);
        assertArrayEquals(a.excludeChild, b.excludeChild);
    }

    private static void assertCtor(CtorEntry a, CtorEntry b) {
        assertEquals(a.getClass(), b.getClass());
        assertEquals(a.requiresRuntime, b.requiresRuntime);
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
    }

    private static void assertClassStub(ClassStubEntry a, ClassStubEntry b) {
        assertEquals(a.target.getInternalName(), b.target.getInternalName());
        assertEquals(a.stubType.getInternalName(), b.stubType.getInternalName());
        assertEquals(a.keepInterface, b.keepInterface);
        assertFQMND(a.instanceofHook, b.instanceofHook);
        assertFQMND(a.checkcastHook, b.checkcastHook);
        assertArrayEquals(a.jeps, b.jeps);
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
