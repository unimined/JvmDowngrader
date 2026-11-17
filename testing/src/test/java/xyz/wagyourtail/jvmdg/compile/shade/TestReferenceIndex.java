package xyz.wagyourtail.jvmdg.compile.shade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestReferenceIndex {

    private static final RefExtractor.Filter INCLUDE_ALL = new RefExtractor.Filter() {
        @Override
        public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
            return true;
        }
    };

    private static ReferenceIndex graph() {
        return new ReferenceIndex(new Logger(TestReferenceIndex.class, Logger.Level.FATAL, false, System.out));
    }

    private static ClassNode baseClass(String name) {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = name;
        node.superName = "java/lang/Object";
        return node;
    }

    private static void writeClass(Path root, String internalName, ClassNode node) throws IOException {
        Path target = root.resolve(internalName + ".class");
        Files.createDirectories(target.getParent());
        Files.write(target, ASMUtils.classNodeToBytes(node));
    }

    private static void writeMultiRelease(Path root, String internalName, ClassNode node) throws IOException {
        writeClass(root.resolve("META-INF/versions/9"), internalName, node);
    }

    // --- member-scanner extension ---

    private static final String CUSTOM_DESC = "Lexample/Marker;";

    /** A consumer-side member scanner: marker annotation pulls a helper ref + a resource. */
    private static class MarkerScanner implements MemberScanner {
        @Override
        public void scanClass(ClassNode node, MemberSink sink) {
        }

        @Override
        public void scanField(FieldNode field, MemberNameAndDesc member, MemberSink sink) {
        }

        @Override
        public void scanMethod(MethodNode method, MemberNameAndDesc member, MemberSink sink) {
            if (method.visibleAnnotations != null) {
                for (AnnotationNode annotation : method.visibleAnnotations) {
                    if (annotation.desc.equals(CUSTOM_DESC)) {
                        sink.requiresMember(new FullyQualifiedMemberNameAndDesc(
                            Type.getObjectType("api/Helper"), "assist", Type.getMethodType("()V")));
                        sink.requiresResource("marker-data.bin");
                    }
                }
            }
        }
    }

    @Test
    public void customMemberScannerContributes() throws Exception {
        List<MemberScanner> extra = new ArrayList<MemberScanner>();
        extra.add(new MarkerScanner());
        ReferenceIndex index = new ReferenceIndex(
            new Logger(TestReferenceIndex.class, Logger.Level.FATAL, false, System.out),
            new MemberRefExtractor(false, extra), false, false);

        ClassNode node = baseClass("in/Caller");
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "call", "()V", null, null);
        AnnotationNode marker = new AnnotationNode(CUSTOM_DESC);
        method.visibleAnnotations = new ArrayList<AnnotationNode>(1);
        method.visibleAnnotations.add(marker);
        node.methods.add(method);

        index.add(node, 0, INCLUDE_ALL);
        ExtractedRefs refs = index.refsFor(Type.getObjectType("in/Caller"), 0);
        MemberNameAndDesc callMember = new MemberNameAndDesc("call", Type.getMethodType("()V"));
        assertTrue(refs.requiredForMembers.get(callMember).contains(
            new FullyQualifiedMemberNameAndDesc(Type.getObjectType("api/Helper"), "assist", Type.getMethodType("()V"))));
        assertArrayEquals(new String[]{"marker-data.bin"}, refs.resourceList.get(callMember));
    }

    // --- multi-release path handling (dir root was an absolute-path crash; separators were "/" hardcoded) ---

    @Test
    public void directoryRootMultiReleaseScans(@TempDir Path dir) throws Exception {
        writeClass(dir, "com/example/Foo", baseClass("com/example/Foo"));
        writeMultiRelease(dir, "com/example/Foo", baseClass("com/example/Foo"));
        writeMultiRelease(dir, "com/example/Only9", baseClass("com/example/Only9"));

        ReferenceIndex g = graph();
        assertDoesNotThrow(() -> IndexScanner.scanRoot(g, dir, INCLUDE_ALL), "directory roots used to NumberFormatException");

        Map<Type, Set<Integer>> keys = g.keys();
        assertEquals(new HashSet<>(Arrays.asList(0, 9)), keys.get(Type.getObjectType("com/example/Foo")));
        assertEquals(new HashSet<>(Collections.singletonList(9)), keys.get(Type.getObjectType("com/example/Only9")));
        assertEquals(new TreeSet<>(Arrays.asList(0, 9)), g.versions());
    }

    @Test
    public void jarRootMultiReleaseScans(@TempDir Path dir) throws Exception {
        Path jar = dir.resolve("test.jar");
        Files.write(jar, new byte[]{0x50, 0x4B, 0x05, 0x06, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        try (FileSystem fs = Utils.openZipFileSystem(jar, true)) {
            Path root = fs.getPath("/");
            writeClass(root, "com/example/Foo", baseClass("com/example/Foo"));
            writeMultiRelease(root, "com/example/Foo", baseClass("com/example/Foo"));
            writeMultiRelease(root, "com/example/Only9", baseClass("com/example/Only9"));

            ReferenceIndex g = graph();
            IndexScanner.scanRoot(g, root, INCLUDE_ALL);

            Map<Type, Set<Integer>> keys = g.keys();
            assertEquals(new HashSet<>(Arrays.asList(0, 9)), keys.get(Type.getObjectType("com/example/Foo")));
            assertEquals(new HashSet<>(Collections.singletonList(9)), keys.get(Type.getObjectType("com/example/Only9")));
        }
    }

    @Test
    public void metaInfNonVersionsEntriesAreNotMultiRelease(@TempDir Path dir) throws Exception {
        // old startsWith("META-INF/versions") matched things like META-INF/versionsX too
        writeClass(dir, "META-INF/versionsX/com/example/Fake",
            baseClass("META-INF/versionsX/com/example/Fake"));
        writeClass(dir, "META-INF/other/com/example/Other",
            baseClass("META-INF/other/com/example/Other"));

        ReferenceIndex g = graph();
        IndexScanner.scanRoot(g, dir, INCLUDE_ALL);

        Map<Type, Set<Integer>> keys = g.keys();
        assertEquals(new TreeSet<>(Collections.singletonList(0)), g.versions());
        assertEquals(Collections.singleton(0), keys.get(Type.getObjectType("META-INF/versionsX/com/example/Fake")));
        assertEquals(Collections.singleton(0), keys.get(Type.getObjectType("META-INF/other/com/example/Other")));
        for (Set<Integer> versions : keys.values()) {
            assertEquals(Collections.singleton(0), versions, "nothing may register as a MR version");
        }
    }

    // --- reference categories ---

    private static AnnotationNode ann(String desc, Object... nameValuePairs) {
        AnnotationNode an = new AnnotationNode(desc);
        an.values = new ArrayList<>();
        for (Object o : nameValuePairs) {
            an.values.add(o);
        }
        return an;
    }

    private static ClassNode buildUsesClass() {
        ClassNode c = baseClass("app/Uses");
        c.signature = "Lstub/SigBase;";
        c.visibleAnnotations = new ArrayList<>(Collections.singletonList(
            ann("Lstub/ClassAnno;", "value", Type.getObjectType("stub/AnnoVal"))));
        c.innerClasses = new ArrayList<>(Collections.singletonList(
            new InnerClassNode("app/Uses$Inner", "stub/InnerOuter", "Inner", Opcodes.ACC_PUBLIC)));
        c.outerClass = "stub/Outer";
        c.outerMethod = "m";
        c.outerMethodDesc = "(Lstub/OuterArg;)V";
        c.nestHostClass = "stub/NestHost";
        c.nestMembers = new ArrayList<>(Collections.singletonList("stub/NestMember"));
        c.permittedSubclasses = new ArrayList<>(Collections.singletonList("stub/Permitted"));
        RecordComponentNode rc = new RecordComponentNode("comp", "Lstub/RcType;", "Lstub/RcSig;");
        c.recordComponents = new ArrayList<>(Collections.singletonList(rc));

        FieldNode f = new FieldNode(Opcodes.ACC_PUBLIC, "f", "Lstub/FieldType;", "Lstub/FieldSig;", null);
        f.invisibleAnnotations = new ArrayList<>(Collections.singletonList(ann("Lstub/FieldAnno;")));
        c.fields.add(f);

        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "m", "(I)V", "Lstub/MethodSig;", new String[]{"stub/Thrown"});
        m.visibleAnnotations = new ArrayList<>(Collections.singletonList(ann("Lstub/MethodAnno;")));
        m.visibleParameterAnnotations = new List[]{new ArrayList<>(Collections.singletonList(ann("Lstub/ParamAnno;")))};
        m.annotationDefault = Type.getObjectType("stub/DefVal");
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        m.instructions.add(start);
        m.instructions.add(end);
        m.instructions.add(handler);
        m.tryCatchBlocks = new ArrayList<>(Collections.singletonList(new TryCatchBlockNode(start, end, handler, "stub/Caught")));
        m.instructions.add(new LdcInsnNode(Type.getObjectType("stub/LdcType")));
        m.instructions.add(new LdcInsnNode(new Handle(Opcodes.H_INVOKESTATIC, "stub/HandleOwner", "h", "()V", false)));
        c.methods.add(m);
        return c;
    }

    @Test
    public void allReferenceCategoriesAreCollected(@TempDir Path dir) throws Exception {
        writeClass(dir, "app/Uses", buildUsesClass());

        ReferenceIndex g = graph();
        IndexScanner.scanRoot(g, dir, INCLUDE_ALL);
        Set<FullyQualifiedMemberNameAndDesc> refs = g.allRefs(0);

        Set<String> owners = new HashSet<>();
        for (FullyQualifiedMemberNameAndDesc ref : refs) {
            owners.add(ref.getOwner().getInternalName());
        }

        for (String expected : new String[]{
            "stub/SigBase", "stub/ClassAnno", "stub/AnnoVal", "stub/InnerOuter", "stub/Outer",
            "stub/OuterArg", "stub/NestHost", "stub/NestMember", "stub/Permitted",
            "stub/RcType", "stub/RcSig", "stub/FieldType", "stub/FieldSig", "stub/FieldAnno",
            "stub/MethodSig", "stub/Thrown", "stub/MethodAnno", "stub/ParamAnno", "stub/DefVal",
            "stub/Caught", "stub/LdcType", "stub/HandleOwner"
        }) {
            assertTrue(owners.contains(expected), "missing reference to " + expected);
        }
    }

    @Test
    public void catchTypesAttachToTheirMethod(@TempDir Path dir) throws Exception {
        writeClass(dir, "app/Uses", buildUsesClass());

        ReferenceIndex g = graph();
        IndexScanner.scanRoot(g, dir, INCLUDE_ALL);
        Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> usages = g.usages(0);

        FullyQualifiedMemberNameAndDesc caught = FullyQualifiedMemberNameAndDesc.of(Type.getObjectType("stub/Caught"));
        Set<FullyQualifiedMemberNameAndDesc> users = usages.get(caught);
        assertNotNull(users, "catch type must be recorded");
        FullyQualifiedMemberNameAndDesc methodRef =
            new MemberNameAndDesc("m", Type.getMethodType("(I)V")).toFullyQualified(Type.getObjectType("app/Uses"));
        assertTrue(users.contains(methodRef), "catch type must be attributed to the owning method, not just the class");
    }

    @Test
    public void concurrentScanIsDeterministic(@TempDir Path dir) throws Exception {
        // many classes with cross-references, scanned through the async path —
        // results must be identical every run (guards racy collection regressions)
        for (int i = 0; i < 40; i++) {
            String name = "det/K" + i;
            ClassNode c = baseClass(name);
            c.superName = "det/K" + ((i + 1) % 40);
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
            m.instructions.add(new TypeInsnNode(Opcodes.NEW, "det/K" + ((i + 7) % 40)));
            m.instructions.add(new InsnNode(Opcodes.DUP));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "det/K" + ((i + 7) % 40), "<init>", "()V", false));
            m.instructions.add(new InsnNode(Opcodes.POP));
            m.instructions.add(new InsnNode(Opcodes.RETURN));
            c.methods.add(m);
            writeClass(dir, name, c);
        }
        // a same-(type,version) duplicate exercises the synchronized scan path
        Files.createDirectories(dir.resolve("dupe"));
        writeClass(dir, "dupe", baseClass("dupe")); // resolves to dupe.class at root level
        // identical class under a second name is impossible (path must match);
        // instead rely on the 40-class fan above for concurrency coverage

        Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> ref = null;
        for (int run = 0; run < 5; run++) {
            ReferenceIndex g = graph();
            IndexScanner.scanRoot(g, dir, INCLUDE_ALL);
            Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> usages = g.usages(0);
            if (ref == null) {
                ref = usages;
            } else {
                assertEquals(ref, usages, "scan run " + run + " diverged — nondeterministic results");
            }
        }
    }

    @Test
    public void filterExcludesUnwantedRefs(@TempDir Path dir) throws Exception {
        writeClass(dir, "app/Uses", buildUsesClass());

        ReferenceIndex g = graph();
        // only include the ldc-referenced type
        IndexScanner.scanRoot(g, dir, new RefExtractor.Filter() {
            @Override
            public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
                return member.getOwner().getInternalName().equals("stub/LdcType");
            }
        });
        Set<FullyQualifiedMemberNameAndDesc> refs = g.allRefs(0);
        assertEquals(1, refs.size());
        assertEquals("stub/LdcType", refs.iterator().next().getOwner().getInternalName());
    }

    // --- reachability ---

    @Test
    public void closureExpandsClassRefsToInstanceMembers(@TempDir Path dir) throws Exception {
        // api/Target has instance method m + requires api/Dep via m's insns
        ClassNode target = baseClass("api/Target");
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
        m.instructions.add(new TypeInsnNode(Opcodes.NEW, "api/Dep"));
        m.instructions.add(new InsnNode(Opcodes.DUP));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "api/Dep", "<init>", "()V", false));
        m.instructions.add(new InsnNode(Opcodes.POP));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        target.methods.add(m);
        writeClass(dir, "api/Target", target);
        writeClass(dir, "api/Dep", baseClass("api/Dep"));

        ReferenceIndex g = graph();
        IndexScanner.scanRoot(g, dir, INCLUDE_ALL);
        Pair<Set<FullyQualifiedMemberNameAndDesc>, Set<String>> closed =
            g.closure(Collections.singleton(FullyQualifiedMemberNameAndDesc.of(Type.getObjectType("api/Target"))), 0);
        Set<String> names = new HashSet<>();
        for (FullyQualifiedMemberNameAndDesc r : closed.getFirst()) {
            names.add(r.getOwner().getInternalName() + "." + (r.getName() == null ? "*" : r.getName()));
        }
        assertTrue(names.contains("api/Target.*"), "seed class ref retained");
        assertTrue(names.contains("api/Target.m"), "class ref expands to instance methods");
        assertTrue(names.contains("api/Dep.*"), "member refs pull their types");
    }

    @Test
    public void closureWithUsagesTracksPuller(@TempDir Path dir) throws Exception {
        ClassNode user = baseClass("app/User");
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "api/Helper", "h", "()V", false));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        user.methods.add(m);
        writeClass(dir, "app/User", user);
        writeClass(dir, "api/Helper", baseClass("api/Helper"));

        ReferenceIndex g = graph();
        IndexScanner.scanRoot(g, dir, INCLUDE_ALL);
        Pair<Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>, Set<String>> closed =
            g.closureWithUsages(g.usages(0), 0);
        FullyQualifiedMemberNameAndDesc h = new FullyQualifiedMemberNameAndDesc(
            Type.getObjectType("api/Helper"), "h", Type.getMethodType("()V"));
        Set<FullyQualifiedMemberNameAndDesc> users = closed.getFirst().get(h);
        assertNotNull(users, "usage map must track the caller");
        assertTrue(users.contains(new FullyQualifiedMemberNameAndDesc(
            Type.getObjectType("app/User"), "m", Type.getMethodType("()V"))));
    }

    @Test
    public void insnsForIsExactVersionAndOwnerScoped(@TempDir Path dir) throws Exception {
        writeClass(dir, "app/Uses", buildUsesClass());
        ReferenceIndex g = new ReferenceIndex(
            new Logger(TestReferenceIndex.class, Logger.Level.FATAL, false, System.out), true, true);
        IndexScanner.scanRoot(g, dir, INCLUDE_ALL);
        FullyQualifiedMemberNameAndDesc h = new FullyQualifiedMemberNameAndDesc(
            Type.getObjectType("stub/HandleOwner"), "h", Type.getMethodType("()V"));
        assertEquals(1, g.insnsFor(h, 0, null).size());
        assertEquals(1, g.insnsFor(h, 0, Type.getObjectType("app/Uses")).size());
        assertEquals(0, g.insnsFor(h, 0, Type.getObjectType("app/Other")).size());
        assertEquals(0, g.insnsFor(h, 9, null).size(), "exact slot — no floor on insn lookup");
    }

    @Test
    public void resolveVersionedFloorsDown() {
        Map<Integer, String> m = new HashMap<>();
        m.put(0, "base");
        m.put(9, "nine");
        assertEquals("nine", ReferenceIndex.resolveVersioned(m, 11));
        assertEquals("base", ReferenceIndex.resolveVersioned(m, 8));
        assertEquals("base", ReferenceIndex.resolveVersioned(m, 0));
        assertNull(ReferenceIndex.resolveVersioned(null, 0));
    }

}
