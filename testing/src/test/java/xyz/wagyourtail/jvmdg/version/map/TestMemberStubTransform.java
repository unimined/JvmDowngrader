package xyz.wagyourtail.jvmdg.version.map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MemberStubTransform} emission details the corpus can't inspect
 * directly — the {@code callerClass}/{@code downgradeVersion} trailing-arg
 * ordering on a rewritten callsite.
 */
public class TestMemberStubTransform implements Opcodes {

    /** {@code forName(String, Class, int)} — the J_L_Class handler shape */
    private static final StubEntry FOR_NAME = new StubEntry(
        new MethodRef(
            new FullyQualifiedMemberNameAndDesc(
                Type.getObjectType("jvmdg/stub/J_L_Class"), "forName",
                Type.getMethodType("(Ljava/lang/String;Ljava/lang/Class;I)Ljava/lang/Class;")),
            ACC_PUBLIC | ACC_STATIC, false, new int[0]),
        true, true, false, false, false, new String[0]);

    private static MemberResolver resolver(final StubEntry entry, final VersionProvider vp) {
        return new MemberResolver() {
            @Override
            public StubEntry getStubFor(MemberNameAndDesc member, boolean runtimeAvailable, int opcode, Set<String> warnings) {
                return member.getName().equals("forName") ? entry : null;
            }

            @Override
            public CtorEntry getCtorFor(MemberNameAndDesc member, boolean runtimeAvailable, Set<String> warnings) {
                return null;
            }

            @Override
            public MethodRef getModifyFor(MemberNameAndDesc member, int opcode, Set<String> warnings) {
                return null;
            }

            @Override
            public MethodRef rawModify(MemberNameAndDesc member) {
                return null;
            }

            @Override
            public MethodRef rawIndyDesugar(MemberNameAndDesc member) {
                return null;
            }

            @Override
            public MemberNameAndDesc unstubMember(String name, Type desc) {
                return new MemberNameAndDesc(name, desc);
            }

            @Override
            public VersionProvider provider() {
                return vp;
            }
        };
    }

    private static class TestProvider extends VersionProvider {
        TestProvider() {
            super(Opcodes.V21, Opcodes.V1_8, 0);
        }

        @Override
        public void preInit() {
        }

        @Override
        public void init() {
        }
    }

    @Test
    public void callerClassAndVersionEmitInOrder() {
        // Class.forName("x.Y") → ldc name, ldc <containing>, ldc inputVersion, invokestatic
        MethodInsnNode site = new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName",
            "(Ljava/lang/String;)Ljava/lang/Class;", false);
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "m", "()V", null, null);
        m.instructions = new InsnList();
        m.instructions.add(new LdcInsnNode("x.Y"));
        m.instructions.add(site);
        m.instructions.add(new InsnNode(POP));
        m.instructions.add(new InsnNode(RETURN));
        ClassNode c = new ClassNode();
        c.name = "foo/Caller";
        c.superName = "java/lang/Object";
        c.methods.add(m);

        Set<String> warnings = new HashSet<>();
        new MemberStubTransform().transformInsn(resolver(FOR_NAME, new TestProvider()), m,
            m.instructions.indexOf(site), c, new HashSet<ClassNode>(), true, warnings, null);

        assertTrue(warnings.isEmpty(), "no warnings expected: " + warnings);
        // ldc "x.Y" stays; ldc Caller.class + ldc inputVersion precede the call
        LdcInsnNode caller = (LdcInsnNode) m.instructions.get(1);
        assertEquals(Type.getObjectType("foo/Caller"), caller.cst,
            "callerClass pushes an ldc of the containing class");
        LdcInsnNode version = (LdcInsnNode) m.instructions.get(2);
        assertEquals(Opcodes.V21, version.cst,
            "downgradeVersion pushes the provider's input version");
        MethodInsnNode call = (MethodInsnNode) m.instructions.get(3);
        assertEquals(INVOKESTATIC, call.getOpcode());
        assertEquals("jvmdg/stub/J_L_Class", call.owner);
        assertEquals("(Ljava/lang/String;Ljava/lang/Class;I)Ljava/lang/Class;", call.desc,
            "the handler desc carries both extra args in signature order");
    }

    /**
     * VarHandle-shaped signature-polymorphic site: {@code get(LFoo;)LBar;} on
     * the poly owner emits an invokedynamic naming the adapter's
     * {@code jvmdg$bootstrap} with the callsite name+desc and a stub-typed
     * receiver prepended — the bsm derives the impl signature at link time.
     */
    @Test
    public void signaturePolymorphicSiteEmitsIndy() {
        TestProvider vp = new TestProvider();
        // mirror the real entry shape: @Adapter w/o target → target = the
        // stub class; the callsite reaching transformInsn already carries the
        // remapped (stub) owner, so lookup goes through unstubClass
        vp.classStubs.put(Type.getObjectType("java/lang/invoke/VarHandle"), new ClassStubEntry(
            Type.getObjectType("jvmdg/stub/J_L_I_VarHandle"),
            Type.getObjectType("jvmdg/stub/J_L_I_VarHandle"),
            false, false, null, null, new int[0],
            xyz.wagyourtail.jvmdg.version.Adapter.Mode.SUPERTYPE, null, null,
            new FullyQualifiedMemberNameAndDesc(
                Type.getObjectType("jvmdg/stub/J_L_I_VarHandle"), "jvmdg$bootstrap",
                Type.getMethodType("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;" +
                    "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;")),
            new java.util.LinkedHashSet<>(java.util.Arrays.asList("get", "set"))));

        MethodInsnNode site = new MethodInsnNode(INVOKEVIRTUAL, "jvmdg/stub/J_L_I_VarHandle",
            "get", "(Lfoo/Owner;)Lfoo/Ret;", false);
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "m", "()V", null, null);
        m.instructions = new InsnList();
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new VarInsnNode(ALOAD, 1));
        m.instructions.add(site);
        m.instructions.add(new InsnNode(POP));
        m.instructions.add(new InsnNode(RETURN));
        ClassNode c = new ClassNode();
        c.name = "foo/Caller";
        c.superName = "java/lang/Object";
        c.methods.add(m);

        Set<String> warnings = new HashSet<>();
        new MemberStubTransform().transformInsn(resolver(null, vp), m,
            m.instructions.indexOf(site), c, new HashSet<ClassNode>(), true, warnings, null);

        AbstractInsnNode out = m.instructions.get(2);
        assertTrue(out instanceof InvokeDynamicInsnNode,
            "polymorphic member miss emits invokedynamic, got " + out.getClass().getSimpleName());
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) out;
        assertEquals("get", indy.name, "callsite name carries the access mode to the bsm");
        assertEquals("(Ljvmdg/stub/J_L_I_VarHandle;Lfoo/Owner;)Lfoo/Ret;", indy.desc,
            "stub receiver prepended, original member signature preserved");
        assertEquals("jvmdg/stub/J_L_I_VarHandle", indy.bsm.getOwner());
        assertEquals("jvmdg$bootstrap", indy.bsm.getName());
        assertEquals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;" +
            "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", indy.bsm.getDesc());
        assertEquals(0, indy.bsmArgs.length, "no bsmArgs — name+desc are the whole contract");
    }

    /** names not in {@code signaturePolymorphic} stay on the native path */
    @Test
    public void signaturePolymorphicUnlistedNameUntouched() {
        TestProvider vp = new TestProvider();
        vp.classStubs.put(Type.getObjectType("java/lang/invoke/VarHandle"), new ClassStubEntry(
            Type.getObjectType("jvmdg/stub/J_L_I_VarHandle"),
            Type.getObjectType("jvmdg/stub/J_L_I_VarHandle"),
            false, false, null, null, new int[0],
            xyz.wagyourtail.jvmdg.version.Adapter.Mode.SUPERTYPE, null, null,
            new FullyQualifiedMemberNameAndDesc(
                Type.getObjectType("jvmdg/stub/J_L_I_VarHandle"), "jvmdg$bootstrap",
                Type.getMethodType("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;" +
                    "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;")),
            new java.util.LinkedHashSet<>(java.util.Arrays.asList("get", "set"))));

        MethodInsnNode site = new MethodInsnNode(INVOKEVIRTUAL, "jvmdg/stub/J_L_I_VarHandle",
            "equals", "(Ljava/lang/Object;)Z", false);
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "m", "()V", null, null);
        m.instructions = new InsnList();
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new VarInsnNode(ALOAD, 1));
        m.instructions.add(site);
        m.instructions.add(new InsnNode(POP));
        m.instructions.add(new InsnNode(RETURN));
        ClassNode c = new ClassNode();
        c.name = "foo/Caller";
        c.superName = "java/lang/Object";
        c.methods.add(m);

        Set<String> warnings = new HashSet<>();
        new MemberStubTransform().transformInsn(resolver(null, vp), m,
            m.instructions.indexOf(site), c, new HashSet<ClassNode>(), true, warnings, null);

        AbstractInsnNode out = m.instructions.get(2);
        assertSame(site, out, "unlisted member keeps the original callsite");
    }

    @Test
    public void reflectionRefDescCarriesStandInRemap() throws Exception {
        // dispatch-site @Ref descs must match the api index's member keys —
        // the post-remap (stand-in) vocabulary the api jar's own members are
        // stored under — or shade pruning can't match the member and drops it
        try (ClassDowngrader cd = ClassDowngrader.downgradeTo(V1_8)) {
            MethodRef impl = new MethodRef(
                new FullyQualifiedMemberNameAndDesc(
                    Type.getObjectType("xyz/wagyourtail/jvmdg/j21/stub/java_net_http/J_N_H_HttpClient"),
                    "close", Type.getMethodType("(Ljava/net/http/HttpClient;)V")),
                ACC_PUBLIC | ACC_STATIC, false, new int[0]);
            MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "site", "()V", null, null);
            MemberStubTransform.addReflectionRef(m, impl, cd, V21, new HashSet<String>());

            assertNotNull(m.invisibleAnnotations);
            AnnotationNode rr = m.invisibleAnnotations.get(0);
            assertEquals("Lxyz/wagyourtail/jvmdg/version/ReflectionReferences;", rr.desc);
            @SuppressWarnings("unchecked")
            List<Object> refs = (List<Object>) rr.values.get(1);
            AnnotationNode ref = (AnnotationNode) refs.get(0);
            assertEquals("Lxyz/wagyourtail/jvmdg/version/Ref;", ref.desc);
            assertEquals("Lxyz/wagyourtail/jvmdg/j21/stub/java_net_http/J_N_H_HttpClient;",
                ref.values.get(1), "impl owner stays the api class");
            assertEquals("close", ref.values.get(3));
            assertEquals("(Lxyz/wagyourtail/jvmdg/j11/stub/java_net_http/J_N_H_HttpClient;)V",
                ref.values.get(5),
                "java/net/http/HttpClient remaps to the j11 stand-in through the chain");
        }
    }
}
