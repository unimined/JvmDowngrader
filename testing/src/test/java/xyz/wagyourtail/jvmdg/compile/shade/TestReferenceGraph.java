package xyz.wagyourtail.jvmdg.compile.shade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestReferenceGraph {

    private static final ReferenceGraph.Filter INCLUDE_ALL = new ReferenceGraph.Filter() {
        @Override
        public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
            return true;
        }
    };

    private static ReferenceGraph graph() {
        return new ReferenceGraph(new Logger(TestReferenceGraph.class, Logger.Level.FATAL, false, System.out));
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

    // --- multi-release path handling (dir root was an absolute-path crash; separators were "/" hardcoded) ---

    @Test
    public void directoryRootMultiReleaseScans(@TempDir Path dir) throws Exception {
        writeClass(dir, "com/example/Foo", baseClass("com/example/Foo"));
        writeMultiRelease(dir, "com/example/Foo", baseClass("com/example/Foo"));
        writeMultiRelease(dir, "com/example/Only9", baseClass("com/example/Only9"));

        ReferenceGraph g = graph();
        assertDoesNotThrow(() -> g.scan(dir, INCLUDE_ALL), "directory roots used to NumberFormatException");

        Map<Type, Set<Integer>> keys = g.getKeys();
        assertEquals(new HashSet<>(Arrays.asList(0, 9)), keys.get(Type.getObjectType("com/example/Foo")));
        assertEquals(new HashSet<>(Collections.singletonList(9)), keys.get(Type.getObjectType("com/example/Only9")));
        assertEquals(new TreeSet<>(Arrays.asList(0, 9)), g.getVersions());
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

            ReferenceGraph g = graph();
            g.scan(root, INCLUDE_ALL);

            Map<Type, Set<Integer>> keys = g.getKeys();
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

        ReferenceGraph g = graph();
        g.scan(dir, INCLUDE_ALL);

        Map<Type, Set<Integer>> keys = g.getKeys();
        assertEquals(new TreeSet<>(Collections.singletonList(0)), g.getVersions());
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

        ReferenceGraph g = graph();
        g.scan(dir, INCLUDE_ALL);
        Set<FullyQualifiedMemberNameAndDesc> refs = g.getAllRefs(0);

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

        ReferenceGraph g = graph();
        g.scan(dir, INCLUDE_ALL);
        Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> usages = g.getAllUsagesForRefs(0);

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
            ReferenceGraph g = graph();
            g.scan(dir, INCLUDE_ALL);
            Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> usages = g.getAllUsagesForRefs(0);
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

        ReferenceGraph g = graph();
        // only include the ldc-referenced type
        g.scan(dir, new ReferenceGraph.Filter() {
            @Override
            public boolean shouldInclude(FullyQualifiedMemberNameAndDesc member) {
                return member.getOwner().getInternalName().equals("stub/LdcType");
            }
        });
        Set<FullyQualifiedMemberNameAndDesc> refs = g.getAllRefs(0);
        assertEquals(1, refs.size());
        assertEquals("stub/LdcType", refs.iterator().next().getOwner().getInternalName());
    }
}
