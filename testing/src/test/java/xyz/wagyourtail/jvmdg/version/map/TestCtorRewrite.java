package xyz.wagyourtail.jvmdg.version.map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitFactory;
import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitPre;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.InitRewrites;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;
import xyz.wagyourtail.jvmdg.version.scan.InitFactoryHandler;
import xyz.wagyourtail.jvmdg.version.scan.StubHandler;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.FileReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CtorRewrite}/{@link CtorFactory} emission for the shapes the stub
 * corpus can't express: {@code this()}/{@code super()} delegation receivers,
 * unverifiable construction shapes, and registration-time validation. Plain
 * {@code NEW}-site behavioral parity is the corpus's job — the migrated api
 * rewrites diff output against native execution there.
 */
public class TestCtorRewrite implements Opcodes {

    public static void tail(FileReader fr, Charset cs) {
    }

    public static void tailWide(FileReader fr, long l) {
    }

    public static void tailSelfOnly(FileReader fr) {
    }

    public static void tailVersion(FileReader fr, Charset cs, int v) {
    }

    public static String toName(Charset cs) {
        return cs.name();
    }

    public static String constName() {
        return "UTF-8";
    }

    public static void checkArgs(String s, Charset cs) {
    }

    public static FileReader create(String s, Charset c) {
        return null;
    }

    public static void createVoid(String s, Charset c) {
    }

    public FileReader createInstance(String s, Charset c) {
        return null;
    }

    private static Method method(String name, Class<?>... params) {
        try {
            return TestCtorRewrite.class.getDeclaredMethod(name, params);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** (String,Charset) → (String), keep arg 0, tail(fr, droppedCharset) — the FileReader shape */
    private static final CtorRewrite FILE_READER_RETARGET = new CtorRewrite(
        Type.getArgumentTypes("(Ljava/lang/String;)V"), new int[]{0},
        new Method[1], new int[1][],
        null, new int[0],
        method("tail", FileReader.class, Charset.class), new int[]{1},
        false, false);

    private static final CtorFactory FILE_READER_FACTORY = new CtorFactory(
        method("create", String.class, Charset.class), false, false);

    private static MemberResolver resolver(final CtorEntry entry) {
        return resolver(entry, null);
    }

    private static MemberResolver resolver(final CtorEntry entry, final VersionProvider vp) {
        return new MemberResolver() {
            @Override
            public StubEntry getStubFor(MemberNameAndDesc member, boolean runtimeAvailable, int opcode, Set<String> warnings) {
                return null;
            }
            @Override
            public CtorEntry getCtorFor(MemberNameAndDesc member, boolean runtimeAvailable, Set<String> warnings) {
                return member.getName().equals("<init>") ? entry : null;
            }
            @Override public MethodRef getModifyFor(MemberNameAndDesc member, int opcode, Set<String> warnings) { return null; }
            @Override public MethodRef rawModify(MemberNameAndDesc member) { return null; }
            @Override public MemberNameAndDesc unstubMember(String name, Type desc) {
                return new MemberNameAndDesc(name, desc);
            }
            @Override public VersionProvider provider() { return vp; }
        };
    }

    private static InsnList list(AbstractInsnNode... insns) {
        InsnList l = new InsnList();
        for (AbstractInsnNode i : insns) {
            l.add(i);
        }
        return l;
    }

    private static ClassNode clazz(String name, String superName, MethodNode m) {
        ClassNode c = new ClassNode();
        c.name = name;
        c.superName = superName;
        c.methods.add(m);
        return c;
    }

    private static MethodInsnNode findInsn(InsnList insns, int opcode) {
        for (int i = 0; i < insns.size(); i++) {
            AbstractInsnNode n = insns.get(i);
            if (n.getOpcode() == opcode) return (MethodInsnNode) n;
        }
        return null;
    }

    private static String descOf(InsnList insns, int opcode) {
        MethodInsnNode n = findInsn(insns, opcode);
        return n == null ? null : n.desc;
    }

    private Set<String> transform(MemberResolver mr, MethodNode m, int index, ClassNode c) {
        Set<String> warnings = new HashSet<>();
        new MemberStubTransform().transformInsn(mr, m, index, c, new HashSet<ClassNode>(), true, warnings);
        return warnings;
    }

    /** the canonical new-site call: NEW+DUP, ldc "f", getstatic UTF_8, invokespecial (String,Charset)V */
    private static FieldInsnNode utf8() {
        return new FieldInsnNode(GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;");
    }

    private static MethodInsnNode charsetInit() {
        return new MethodInsnNode(INVOKESPECIAL, "java/io/FileReader", "<init>",
            "(Ljava/lang/String;Ljava/nio/charset/Charset;)V", false);
    }

    @Test
    public void newSiteRetargetsAndTails() {
        // args spilled, kept arg reloaded for (String)V, tail gets (init'd ref, charset)
        FieldInsnNode cs = utf8();
        MethodInsnNode init = charsetInit();
        MethodNode m = new MethodNode(ACC_PUBLIC, "m", "()V", null, null);
        m.instructions = list(
            new TypeInsnNode(NEW, "java/io/FileReader"),
            new InsnNode(DUP),
            new LdcInsnNode("f"),
            cs,
            init,
            new VarInsnNode(ASTORE, 1),
            new InsnNode(RETURN)
        );
        m.maxLocals = 2;
        ClassNode c = clazz("foo/Caller", "java/lang/Object", m);
        Set<String> warnings = transform(resolver(FILE_READER_RETARGET), m, m.instructions.indexOf(init), c);

        assertTrue(warnings.isEmpty(), "no warnings expected: " + warnings);
        assertEquals(NEW, m.instructions.get(0).getOpcode());
        assertEquals(DUP, m.instructions.get(1).getOpcode());
        assertEquals("(Ljava/lang/String;)V", descOf(m.instructions, INVOKESPECIAL));
        MethodInsnNode tail = findInsn(m.instructions, INVOKESTATIC);
        assertNotNull(tail);
        assertEquals("tail", tail.name);
        assertEquals("(Ljava/io/FileReader;Ljava/nio/charset/Charset;)V", tail.desc);
        // between cs push and invokespecial: ASTORE charset tmp, ASTORE string tmp, ALOAD string
        int csIdx = m.instructions.indexOf(cs);
        assertEquals(ASTORE, m.instructions.get(csIdx + 1).getOpcode());
        assertEquals(ASTORE, m.instructions.get(csIdx + 2).getOpcode());
        assertEquals(ALOAD, m.instructions.get(csIdx + 3).getOpcode());
        // after invokespecial: DUP + ALOAD charset + invokestatic
        int initIdx = m.instructions.indexOf(findInsn(m.instructions, INVOKESPECIAL));
        assertEquals(DUP, m.instructions.get(initIdx + 1).getOpcode());
        assertEquals(ALOAD, m.instructions.get(initIdx + 2).getOpcode());
        assertEquals(INVOKESTATIC, m.instructions.get(initIdx + 3).getOpcode());
        assertEquals(4, m.maxLocals);
    }

    @Test
    public void superDelegationTailsOnThis() {
        // super(s, cs) inside <init>: ALOAD 0 stays, tail reloads this — no DUP
        MethodInsnNode init = charsetInit();
        MethodNode m = new MethodNode(ACC_PUBLIC, "<init>", "(Ljava/lang/String;Ljava/nio/charset/Charset;)V", null, null);
        m.instructions = list(
            new VarInsnNode(ALOAD, 0),
            new VarInsnNode(ALOAD, 1),
            new VarInsnNode(ALOAD, 2),
            init,
            new InsnNode(RETURN)
        );
        m.maxLocals = 3;
        ClassNode c = clazz("foo/SubReader", "java/io/FileReader", m);
        Set<String> warnings = transform(resolver(FILE_READER_RETARGET), m, m.instructions.indexOf(init), c);

        assertTrue(warnings.isEmpty(), "no warnings expected: " + warnings);
        assertEquals("(Ljava/lang/String;)V", descOf(m.instructions, INVOKESPECIAL));
        int initIdx = m.instructions.indexOf(findInsn(m.instructions, INVOKESPECIAL));
        assertEquals(ALOAD, m.instructions.get(initIdx + 1).getOpcode());
        assertEquals(0, ((VarInsnNode) m.instructions.get(initIdx + 1)).var);
        assertEquals(INVOKESTATIC, m.instructions.get(initIdx + 3).getOpcode());
        for (int i = 1; i < m.instructions.size(); i++) {
            assertNotEquals(DUP, m.instructions.get(i).getOpcode(), "delegation must not emit DUP");
        }
    }

    @Test
    public void preAndPostAroundDelegation() {
        // pre runs on spilled args before the delegation call; post tails on this
        CtorRewrite rw = new CtorRewrite(
            Type.getArgumentTypes("(Ljava/lang/String;)V"), new int[]{0},
            new Method[1], new int[1][],
            method("checkArgs", String.class, Charset.class), new int[]{0, 1},
            method("tail", FileReader.class, Charset.class), new int[]{1},
            false, false);
        MethodInsnNode init = charsetInit();
        MethodNode m = new MethodNode(ACC_PUBLIC, "<init>", "(Ljava/lang/String;Ljava/nio/charset/Charset;)V", null, null);
        m.instructions = list(
            new VarInsnNode(ALOAD, 0),
            new VarInsnNode(ALOAD, 1),
            new VarInsnNode(ALOAD, 2),
            init,
            new InsnNode(RETURN)
        );
        m.maxLocals = 3;
        ClassNode c = clazz("foo/SubReader", "java/io/FileReader", m);
        Set<String> warnings = transform(resolver(rw), m, m.instructions.indexOf(init), c);

        assertTrue(warnings.isEmpty(), "no warnings expected: " + warnings);
        int initIdx = m.instructions.indexOf(findInsn(m.instructions, INVOKESPECIAL));
        // pre: first invokestatic before the retargeted call
        MethodInsnNode preInsn = null;
        int statics = 0;
        for (int i = 0; i < initIdx; i++) {
            if (m.instructions.get(i).getOpcode() == INVOKESTATIC) {
                preInsn = (MethodInsnNode) m.instructions.get(i);
                statics++;
            }
        }
        assertEquals(1, statics, "exactly one pre invokestatic before <init>");
        assertEquals("checkArgs", preInsn.name);
        // post: ALOAD 0 + ALOAD charset tmp + tail
        assertEquals(ALOAD, m.instructions.get(initIdx + 1).getOpcode());
        assertEquals(0, ((VarInsnNode) m.instructions.get(initIdx + 1)).var);
        assertEquals(INVOKESTATIC, m.instructions.get(initIdx + 3).getOpcode());
    }

    @Test
    public void converterFillsSlotFromOrigArg() {
        // (String,Charset) → (String,String): keep arg0, converter cs→name()
        CtorRewrite rw = new CtorRewrite(
            Type.getArgumentTypes("(Ljava/lang/String;Ljava/lang/String;)V"), new int[]{0, -1},
            new Method[]{null, method("toName", Charset.class)}, new int[][]{null, new int[]{1}},
            null, new int[0], null, new int[0], false, false);
        FieldInsnNode cs = utf8();
        MethodInsnNode init = charsetInit();
        MethodNode m = new MethodNode(ACC_PUBLIC, "m", "()V", null, null);
        m.instructions = list(
            new TypeInsnNode(NEW, "java/io/FileReader"),
            new InsnNode(DUP),
            new LdcInsnNode("f"),
            cs,
            init,
            new InsnNode(POP),
            new InsnNode(RETURN)
        );
        m.maxLocals = 1;
        ClassNode c = clazz("foo/Caller", "java/lang/Object", m);
        Set<String> warnings = transform(resolver(rw), m, m.instructions.indexOf(init), c);

        assertTrue(warnings.isEmpty(), "no warnings expected: " + warnings);
        assertEquals("(Ljava/lang/String;Ljava/lang/String;)V", descOf(m.instructions, INVOKESPECIAL));
        int initIdx = m.instructions.indexOf(findInsn(m.instructions, INVOKESPECIAL));
        // just before: ALOAD string tmp, ALOAD charset tmp, invokestatic toName
        assertEquals(INVOKESTATIC, m.instructions.get(initIdx - 1).getOpcode());
        assertEquals("toName", ((MethodInsnNode) m.instructions.get(initIdx - 1)).name);
        assertEquals(ALOAD, m.instructions.get(initIdx - 2).getOpcode());
        // no post → no DUP after the call
        assertNotEquals(DUP, m.instructions.get(initIdx + 1).getOpcode());
    }

    @Test
    public void unverifiableShapeWarnsAndLeaves() {
        MethodInsnNode init = charsetInit();
        MethodNode m = new MethodNode(ACC_PUBLIC, "m", "()V", null, null);
        m.instructions = list(
            new VarInsnNode(ALOAD, 1),
            new VarInsnNode(ALOAD, 2),
            init,
            new InsnNode(RETURN)
        );
        int sizeBefore = m.instructions.size();
        ClassNode c = clazz("foo/Caller", "java/lang/Object", m);
        Set<String> warnings = transform(resolver(FILE_READER_RETARGET), m, m.instructions.indexOf(init), c);

        assertEquals(1, warnings.size());
        assertEquals(sizeBefore, m.instructions.size());
        assertEquals("(Ljava/lang/String;Ljava/nio/charset/Charset;)V", descOf(m.instructions, INVOKESPECIAL));
    }

    @Test
    public void factoryReplacesNewSiteWithInvokestatic() {
        FieldInsnNode cs = utf8();
        MethodInsnNode init = charsetInit();
        MethodNode m = new MethodNode(ACC_PUBLIC, "m", "()V", null, null);
        m.instructions = list(
            new TypeInsnNode(NEW, "java/io/FileReader"),
            new InsnNode(DUP),
            new LdcInsnNode("f"),
            cs,
            init,
            new VarInsnNode(ASTORE, 1),
            new InsnNode(RETURN)
        );
        m.maxLocals = 2;
        ClassNode c = clazz("foo/Caller", "java/lang/Object", m);
        Set<String> warnings = transform(resolver(FILE_READER_FACTORY), m, m.instructions.indexOf(init), c);

        assertTrue(warnings.isEmpty(), "no warnings expected: " + warnings);
        for (int i = 0; i < m.instructions.size(); i++) {
            assertNotEquals(NEW, m.instructions.get(i).getOpcode(), "NEW must be removed");
            assertNotEquals(DUP, m.instructions.get(i).getOpcode(), "DUP must be removed");
        }
        assertNull(findInsn(m.instructions, INVOKESPECIAL));
        MethodInsnNode call = findInsn(m.instructions, INVOKESTATIC);
        assertNotNull(call);
        assertEquals("create", call.name);
        assertEquals("(Ljava/lang/String;Ljava/nio/charset/Charset;)Ljava/io/FileReader;", call.desc);
    }

    @Test
    public void factoryOnDelegationWarnsAndLeaves() {
        MethodInsnNode init = charsetInit();
        MethodNode m = new MethodNode(ACC_PUBLIC, "<init>", "(Ljava/lang/String;Ljava/nio/charset/Charset;)V", null, null);
        m.instructions = list(
            new VarInsnNode(ALOAD, 0),
            new VarInsnNode(ALOAD, 1),
            new VarInsnNode(ALOAD, 2),
            init,
            new InsnNode(RETURN)
        );
        int sizeBefore = m.instructions.size();
        ClassNode c = clazz("foo/SubReader", "java/io/FileReader", m);
        Set<String> warnings = transform(resolver(FILE_READER_FACTORY), m, m.instructions.indexOf(init), c);

        assertEquals(1, warnings.size());
        assertEquals(sizeBefore, m.instructions.size());
        assertNull(findInsn(m.instructions, INVOKESTATIC));
    }

    // --- registration: @InitRewrite family ---------------------------------

    static class TestProvider extends VersionProvider {
        TestProvider() {
            super(Opcodes.V11, Opcodes.V1_8, 0);
        }
        @Override public void preInit() {
        }
        @Override public void init() {
        }
    }

    private static TestProvider provider(ClassDowngrader downgrader) {
        TestProvider provider = new TestProvider();
        provider.ensureInit(downgrader);
        return provider;
    }

    private static final String FR = "Ljava/io/FileReader;";
    private static final String ORIG = "(Ljava/lang/String;Ljava/nio/charset/Charset;)V";

    @InitRewrite(ref = @Ref(value = FR, desc = ORIG), to = "(Ljava/lang/String;)V", keep = {0})
    static class DropRewrite {
        @InitPost(target = ORIG)
        public static void capture(FileReader fr, Charset cs) {
        }
    }

    @InitRewrite(ref = @Ref(value = FR, desc = ORIG), to = "(Ljava/lang/String;Ljava/lang/String;)V", keep = {0, -1})
    static class ConvertRewrite {
        @InitArg(target = ORIG, slot = 1)
        public static String nameOf(Charset cs) {
            return cs.name();
        }
    }

    @InitRewrites({
        @InitRewrite(ref = @Ref(value = FR, desc = "(Ljava/lang/String;)V"), to = "()V"),
        @InitRewrite(ref = @Ref(value = "Ljava/io/File;", desc = "(Ljava/lang/String;)V"), to = "()V")
    })
    static class TwoRewrites {
    }

    static class OrphanArg {
        @InitArg(target = "(Ljava/lang/String;)V", slot = 0)
        public static String name(String s) {
            return s;
        }
    }

    @InitRewrite(ref = @Ref(value = FR, desc = "(Ljava/lang/String;Ljava/lang/String;)V"), to = "(Ljava/lang/String;)V")
    static class AmbiguousKeep {
    }

    @InitRewrite(ref = @Ref(value = FR, desc = ORIG), to = "(Ljava/lang/String;)V")
    static class ConflictWithFactory {
        @InitFactory(ref = @Ref(value = FR, member = "<init>", desc = ORIG))
        public static FileReader make(String s, Charset cs) {
            return null;
        }
    }

    private static Map<MemberNameAndDesc, CtorEntry> ctorMap(TestProvider p, String owner) {
        ClassMapping m = p.stubMappings.get(Type.getObjectType(owner));
        return m == null ? java.util.Collections.<MemberNameAndDesc, CtorEntry>emptyMap() : m.getCtorMap();
    }

    @Test
    public void rewriteRegistersWithBoundPost() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider p = provider(d);
            p.stub(DropRewrite.class);
            CtorEntry e = ctorMap(p, "java/io/FileReader")
                .get(new MemberNameAndDesc("<init>", Type.getMethodType(ORIG)));
            assertTrue(e instanceof CtorRewrite, "expected CtorRewrite, got " + e);
            CtorRewrite rw = (CtorRewrite) e;
            assertArrayEquals(Type.getArgumentTypes("(Ljava/lang/String;)V"), rw.toArgs);
            assertArrayEquals(new int[]{0}, rw.keep);
            assertNotNull(rw.post);
            assertArrayEquals(new int[]{1}, rw.postFrom);
        }
    }

    @Test
    public void rewriteBindsInitArgConverter() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider p = provider(d);
            p.stub(ConvertRewrite.class);
            CtorRewrite rw = (CtorRewrite) ctorMap(p, "java/io/FileReader")
                .get(new MemberNameAndDesc("<init>", Type.getMethodType(ORIG)));
            assertNotNull(rw);
            assertEquals(-1, rw.keep[1]);
            assertEquals("nameOf", rw.argMethods[1].member.getName());
            assertArrayEquals(new int[]{1}, rw.argFrom[1]);
        }
    }

    @Test
    public void containerRegistersEachRewrite() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider p = provider(d);
            p.stub(TwoRewrites.class);
            assertTrue(ctorMap(p, "java/io/FileReader")
                .containsKey(new MemberNameAndDesc("<init>", Type.getMethodType("(Ljava/lang/String;)V"))));
            assertTrue(ctorMap(p, "java/io/File")
                .containsKey(new MemberNameAndDesc("<init>", Type.getMethodType("(Ljava/lang/String;)V"))));
        }
    }

    @Test
    public void orphanHandlerFailsRegistration() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider p = provider(d);
            // logs a failure summary; registers nothing
            assertDoesNotThrow(() -> p.stub(OrphanArg.class));
            assertTrue(p.stubMappings.get(Type.getObjectType("java/io/FileReader")) == null
                || p.stubMappings.get(Type.getObjectType("java/io/FileReader")).getCtorMap().isEmpty());
        }
    }

    @Test
    public void ambiguousDeriveFailsRegistration() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider p = provider(d);
            assertDoesNotThrow(() -> p.stub(AmbiguousKeep.class));
            assertTrue(ctorMap(p, "java/io/FileReader").isEmpty(),
                "two identical String args can't derive keep[0] — registration must fail");
        }
    }

    @Test
    public void rewriteAndFactoryOnSameCtorConflict() throws Exception {
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(Opcodes.V1_8)) {
            TestProvider p = provider(d);
            assertDoesNotThrow(() -> p.stub(ConflictWithFactory.class));
            Map<MemberNameAndDesc, CtorEntry> m = ctorMap(p, "java/io/FileReader");
            // exactly one registration survives: first write wins, the other is a failure
            assertEquals(1, m.size());
        }
    }

    // --- registration: legacy @Stub forms ----------------------------------

    private static Ref ref(final String value, final String member, final String desc) {
        return new Ref() {
            @Override public Class<? extends Annotation> annotationType() { return Ref.class; }
            @Override public String value() { return value; }
            @Override public String member() { return member; }
            @Override public String desc() { return desc; }
        };
    }

    private static Stub stub() {
        return new Stub() {
            @Override public Class<? extends Annotation> annotationType() { return Stub.class; }
            @Override public Ref ref() { return null; }
            @Override public boolean downgradeVersion() { return false; }
            @Override public boolean requiresRuntime() { return false; }
            @Override public boolean noSpecial() { return false; }
            @Override public String[] excludeChild() { return new String[0]; }
        };
    }

    @Test
    public void factoryRegistersAgainstCtorDesc() throws Exception {
        FullyQualifiedMemberNameAndDesc t = InitFactoryHandler.resolveFactoryTarget(
            method("create", String.class, Charset.class), ref("", "<init>", ""));
        assertEquals("java/io/FileReader", t.getOwner().getInternalName());
        assertEquals("<init>", t.getName());
        assertEquals("(Ljava/lang/String;Ljava/nio/charset/Charset;)V", t.getDesc().getDescriptor());
    }

    @Test
    public void factoryMustReturnObject() throws Exception {
        assertThrows(IllegalArgumentException.class, () ->
            InitFactoryHandler.resolveFactoryTarget(
                method("createVoid", String.class, Charset.class), ref("", "<init>", "")));
    }

    @Test
    public void factoryMustBeStatic() {
        assertThrows(IllegalArgumentException.class, () ->
            InitFactoryHandler.resolveFactoryTarget(
                method("createInstance", String.class, Charset.class), ref("", "<init>", "")));
    }

    @Test
    public void factoryMemberMustBeInit() {
        assertThrows(IllegalArgumentException.class, () ->
            InitFactoryHandler.resolveFactoryTarget(
                method("create", String.class, Charset.class),
                ref("Ljava/io/File;", "notInit", "(Ljava/lang/String;)V")));
    }

    @Test
    public void plainCtorStubPointsAtInitFactory() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
            StubHandler.resolveStubTarget(
                method("create", String.class, Charset.class),
                ref("", "<init>", ""), stub()));
        assertTrue(e.getMessage().contains("@InitFactory"), e.getMessage());
    }

    @Test
    public void ctorStubWithExplicitRefStillRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
            StubHandler.resolveStubTarget(
                method("tail", FileReader.class, Charset.class),
                ref(FR, "<init>", "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"), stub()));
        assertTrue(e.getMessage().contains("@InitFactory"), e.getMessage());
    }
}
