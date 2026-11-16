package xyz.wagyourtail.jvmdg.version;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.cli.Flags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Crafted-bytecode unit tests for {@code VersionProvider#otherTransforms} —
 * the rewrites javac fixtures can't produce (private interface methods,
 * nest-private handle targets, compact mains) plus the {@code <=51} transform
 * chain that the {@code testTargetVersion=8} matrix never reaches:
 * interface statics/defaults ({@code $jvmdg$StaticDefaults}), invokedynamic /
 * ldc desugar ({@code jvmdg$indy$}/{@code jvmdg$handle$}/{@code jvmdg$lookup}),
 * stack-map frame stripping, and nest accessors.
 *
 * <p>Each test crafts {@link ClassNode}s with ASM, runs them through
 * {@link ClassDowngrader#downgrade}, re-parses the output and asserts on the
 * resulting structure — no JVMs, no fixtures.</p>
 */
public class TestOtherTransforms {

    private static Flags flags(int version) {
        Flags f = new Flags();
        f.classVersion = version;
        f.quiet = true;
        return f;
    }

    private static ClassNode node(int version, int access, String name) {
        ClassNode n = new ClassNode();
        n.version = version;
        n.access = access;
        n.name = name;
        n.superName = "java/lang/Object";
        return n;
    }

    private static MethodNode method(int access, String name, String desc) {
        MethodNode m = new MethodNode(access, name, desc, null, null);
        m.instructions = new InsnList();
        return m;
    }

    private static Map<String, byte[]> downgrade(int target, Map<String, ClassNode> inputs, String main) throws Exception {
        Map<String, byte[]> raw = new HashMap<>();
        for (Map.Entry<String, ClassNode> e : inputs.entrySet()) {
            raw.put(e.getKey(), ASMUtils.classNodeToBytes(e.getValue(), 0));
        }
        Flags f = flags(target);
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(f)) {
            Map<String, byte[]> out = d.downgrade(new AtomicReference<>(), raw.get(main), false,
                name -> raw.get(name.replace(".", "/")));
            return out == null ? new HashMap<>() : out;
        }
    }

    private static Map<String, ClassNode> toNodes(Map<String, byte[]> out) {
        Map<String, ClassNode> nodes = new HashMap<>();
        for (Map.Entry<String, byte[]> e : out.entrySet()) {
            if (e.getKey().endsWith(".class") || !e.getKey().contains("/META-INF/versions")) {
                ClassNode n = new ClassNode();
                new org.objectweb.asm.ClassReader(e.getValue()).accept(n, 0);
                nodes.put(n.name, n);
            }
        }
        return nodes;
    }

    private static MethodNode find(ClassNode n, String name) {
        for (MethodNode m : n.methods) {
            if (m.name.equals(name)) return m;
        }
        return null;
    }

    private static boolean hasInsn(ClassNode n, java.util.function.Predicate<AbstractInsnNode> p) {
        for (MethodNode m : n.methods) {
            for (AbstractInsnNode i : m.instructions) {
                if (p.test(i)) return true;
            }
        }
        return false;
    }

    // ---------- Java8Downgrader (target <= 51) ----------

    @Test
    public void interfaceStaticsMoveToStaticDefaults() throws Exception {
        ClassNode itf = node(Opcodes.V1_8, Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC, "t/Itf");
        MethodNode stat = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "stat", "()V");
        stat.instructions.add(new InsnNode(Opcodes.RETURN));
        MethodNode dflt = method(Opcodes.ACC_PUBLIC, "dflt", "()V");
        dflt.instructions.add(new InsnNode(Opcodes.RETURN));
        itf.methods.add(stat);
        itf.methods.add(dflt);

        Map<String, ClassNode> out = toNodes(downgrade(Opcodes.V1_7, Map.of("t/Itf", itf), "t/Itf"));

        ClassNode companion = out.get("t/Itf$jvmdg$StaticDefaults");
        assertNotNull(companion, "companion class must be emitted");
        MethodNode movedStat = find(companion, "stat");
        assertNotNull(movedStat);
        assertEquals("()V", movedStat.desc);
        MethodNode movedDflt = find(companion, "dflt");
        assertNotNull(movedDflt, "default impl must move to companion");
        assertEquals("(Lt/Itf;)V", movedDflt.desc, "default must gain a self parameter");
        assertEquals(Opcodes.ACC_STATIC, movedDflt.access & Opcodes.ACC_STATIC);

        ClassNode newItf = out.get("t/Itf");
        assertNull(find(newItf, "stat"), "static must leave the interface");
        MethodNode shell = find(newItf, "dflt");
        assertNotNull(shell, "abstract shell must be written back");
        assertEquals(Opcodes.ACC_ABSTRACT, shell.access & Opcodes.ACC_ABSTRACT);
    }

    @Test
    public void interfaceCallSitesRewritten() throws Exception {
        ClassNode caller = node(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "t/Caller");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "call", "()V");
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "t/Itf", "stat", "()V", true));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "t/Itf", "dflt", "()V", true));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        caller.methods.add(m);

        Map<String, ClassNode> out = toNodes(downgrade(Opcodes.V1_7, Map.of("t/Caller", caller), "t/Caller"));
        ClassNode nc = out.get("t/Caller");
        assertTrue(hasInsn(nc, i -> i instanceof MethodInsnNode min
            && min.getOpcode() == Opcodes.INVOKESTATIC
            && min.owner.equals("t/Itf$jvmdg$StaticDefaults")
            && min.name.equals("stat") && !min.itf), "invokestatic must redirect to companion");
        assertTrue(hasInsn(nc, i -> i instanceof MethodInsnNode min
            && min.owner.equals("t/Itf$jvmdg$StaticDefaults")
            && min.name.equals("dflt")
            && min.desc.equals("(Lt/Itf;)V")), "private/super itf call must gain self param");
    }

    // ---------- Java7Downgrader (target <= 50) ----------

    @Test
    public void invokedynamicDesugarsToSyntheticCall() throws Exception {
        ClassNode c = node(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "t/L");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()V");
        // LambdaMetafactory bootstraps are claimed by Java8's lambda desugar
        // (jvmdowngrader$lambda$ + an anonymous class); jvmdg$indy$ is the
        // fallback path for other bootstrap methods
        Handle bsm = new Handle(Opcodes.H_INVOKESTATIC,
            "t/Boot", "bootstrap",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;I)Ljava/lang/invoke/CallSite;", false);
        m.instructions.add(new InvokeDynamicInsnNode("dyn", "()Ljava/lang/Object;", bsm, 42));
        m.instructions.add(new InsnNode(Opcodes.POP));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        c.methods.add(m);

        // Java7Downgrader is the v51 -> v50 provider; indy desugar only runs
        // when the target is <= v50
        Map<String, ClassNode> out = toNodes(downgrade(Opcodes.V1_6, Map.of("t/L", c), "t/L"));
        ClassNode nc = out.get("t/L");
        assertFalse(hasInsn(nc, i -> i instanceof InvokeDynamicInsnNode), "indy must be desugared");
        assertTrue(nc.methods.stream().anyMatch(mn -> mn.name.startsWith("jvmdg$indy$")),
            "jvmdg$indy$ synthetic method must exist");
        assertTrue(nc.fields.stream().anyMatch(fd -> fd.name.startsWith("jvmdg$indy$")),
            "CallSite field must be cached");
        assertTrue(nc.fields.stream().anyMatch(fd -> fd.name.equals("jvmdg$lookup")),
            "jvmdg$lookup field must be added");
    }

    @Test
    public void ldcHandleAndMethodTypeDesugar() throws Exception {
        ClassNode c = node(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "t/Ldc");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()V");
        m.instructions.add(new LdcInsnNode(new Handle(Opcodes.H_INVOKESTATIC, "java/lang/Object", "toString", "()Ljava/lang/String;", false)));
        m.instructions.add(new InsnNode(Opcodes.POP));
        m.instructions.add(new LdcInsnNode(Type.getMethodType("()V")));
        m.instructions.add(new InsnNode(Opcodes.POP));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        c.methods.add(m);

        Map<String, ClassNode> out = toNodes(downgrade(Opcodes.V1_6, Map.of("t/Ldc", c), "t/Ldc"));
        ClassNode nc = out.get("t/Ldc");
        assertTrue(nc.fields.stream().anyMatch(fd -> fd.name.startsWith("jvmdg$handle$")),
            "ldc MethodHandle must become a cached field");
        assertFalse(hasInsn(nc, i -> i instanceof LdcInsnNode ldc && ldc.cst instanceof Handle),
            "ldc(handle) is illegal below v51");
        assertFalse(hasInsn(nc, i -> i instanceof LdcInsnNode ldc && ldc.cst instanceof Type mt && mt.getSort() == Type.METHOD),
            "ldc(MethodType) is illegal below v51");
    }

    // ---------- Java6Downgrader (target <= 49) ----------

    @Test
    public void stackMapFramesStripped() throws Exception {
        ClassNode c = node(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "t/F");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()V");
        LabelNode lbl = new LabelNode();
        m.instructions.add(lbl);
        m.instructions.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        c.methods.add(m);

        Map<String, ClassNode> out = toNodes(downgrade(Opcodes.V1_5, Map.of("t/F", c), "t/F"));
        assertFalse(hasInsn(out.get("t/F"), i -> i instanceof FrameNode),
            "stack map frames must be stripped below v50");
    }

    // ---------- Java11Downgrader (target <= 54) ----------

    @Test
    public void privateInterfaceInvokeRewritten() throws Exception {
        ClassNode itf = node(Opcodes.V11, Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC, "t/Itf");
        MethodNode priv = method(Opcodes.ACC_PRIVATE, "priv", "()V");
        priv.instructions.add(new InsnNode(Opcodes.RETURN));
        itf.methods.add(priv);
        MethodNode caller = method(0, "caller", "()V");
        caller.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        caller.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "t/Itf", "priv", "()V", true));
        caller.instructions.add(new InsnNode(Opcodes.RETURN));
        itf.methods.add(caller);

        Map<String, ClassNode> out = toNodes(downgrade(Opcodes.V1_8, Map.of("t/Itf", itf), "t/Itf"));
        assertTrue(hasInsn(out.get("t/Itf"), i -> i instanceof MethodInsnNode min
            && min.getOpcode() == Opcodes.INVOKESPECIAL
            && min.name.equals("priv")), "invokeinterface on private itf method must become invokespecial");
    }

    @Test
    public void nestAccessGeneratesAccessor() throws Exception {
        ClassNode outer = node(Opcodes.V11, Opcodes.ACC_PUBLIC, "t/Outer");
        outer.nestMembers = List.of("t/Outer$Inner");
        outer.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "secret", "I", null, 0));
        ClassNode inner = node(Opcodes.V11, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "t/Outer$Inner");
        inner.nestHostClass = "t/Outer";
        MethodNode read = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "read", "(Lt/Outer;)I");
        read.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        read.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "t/Outer", "secret", "I"));
        read.instructions.add(new InsnNode(Opcodes.IRETURN));
        inner.methods.add(read);

        Map<String, byte[]> raw = new HashMap<>();
        raw.put("t/Outer", ASMUtils.classNodeToBytes(outer, 0));
        raw.put("t/Outer$Inner", ASMUtils.classNodeToBytes(inner, 0));
        Flags f = flags(Opcodes.V1_8);
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(f)) {
            Map<String, byte[]> out = d.downgrade(new AtomicReference<>(), raw.get("t/Outer$Inner"), false,
                n -> raw.get(n.replace(".", "/")));
            ClassNode ni = toNodes(out).get("t/Outer$Inner");
            assertTrue(hasInsn(ni, i -> i instanceof MethodInsnNode min
                && min.getOpcode() == Opcodes.INVOKEVIRTUAL
                && min.owner.equals("t/Outer")
                && min.name.startsWith("jvmdowngrader$nest$")),
                "nestmate private field access must route through a generated accessor");
        }
    }

    // ---------- Java25Downgrader (target <= 68) ----------

    @Test
    public void instanceMainRenamedAndShimSynthesized() throws Exception {
        ClassNode c = node(Opcodes.V25, Opcodes.ACC_PUBLIC, "t/M");
        MethodNode main = method(0, "main", "([Ljava/lang/String;)V");
        main.instructions.add(new InsnNode(Opcodes.RETURN));
        c.methods.add(main);
        MethodNode ctor = method(Opcodes.ACC_PUBLIC, "<init>", "()V");
        ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ctor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V"));
        ctor.instructions.add(new InsnNode(Opcodes.RETURN));
        c.methods.add(ctor);

        Map<String, ClassNode> out = toNodes(downgrade(Opcodes.V24, Map.of("t/M", c), "t/M"));
        ClassNode nc = out.get("t/M");
        assertNotNull(find(nc, "jvmdg$instanceMain"), "instance main must be renamed");
        MethodNode shim = find(nc, "main");
        assertNotNull(shim, "static shim must be synthesized");
        assertEquals(Opcodes.ACC_STATIC, shim.access & Opcodes.ACC_STATIC);
        assertTrue(hasInsn(nc, i -> i instanceof MethodInsnNode min
            && min.getOpcode() == Opcodes.INVOKEVIRTUAL
            && min.name.equals("jvmdg$instanceMain")), "shim must invoke the renamed instance main");
    }

    @Test
    public void allInstanceMainCallSitesRenamed() throws Exception {
        ClassNode c = node(Opcodes.V25, Opcodes.ACC_PUBLIC, "t/D");
        MethodNode helper = method(0, "main", "([Ljava/lang/String;)V");
        helper.instructions.add(new InsnNode(Opcodes.RETURN));
        c.methods.add(helper);
        MethodNode twoCalls = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "twice", "()V");
        twoCalls.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        twoCalls.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "t/D", "main", "([Ljava/lang/String;)V"));
        twoCalls.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        twoCalls.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "t/D", "main", "([Ljava/lang/String;)V"));
        twoCalls.instructions.add(new InsnNode(Opcodes.RETURN));
        c.methods.add(twoCalls);

        Map<String, ClassNode> out = toNodes(downgrade(Opcodes.V24, Map.of("t/D", c), "t/D"));
        ClassNode nc = out.get("t/D");
        long renamed = 0;
        for (MethodNode mn : nc.methods) {
            for (AbstractInsnNode i : mn.instructions) {
                if (i instanceof MethodInsnNode min && min.name.equals("jvmdg$instanceMain")) renamed++;
            }
        }
        assertTrue(renamed >= 2, "every call site must be renamed, got " + renamed);
        assertFalse(hasInsn(nc, i -> i instanceof MethodInsnNode min
            && min.getOpcode() == Opcodes.INVOKEVIRTUAL
            && min.name.equals("main") && min.desc.equals("([Ljava/lang/String;)V")),
            "no un-renamed instance main call sites may remain");
    }
}
