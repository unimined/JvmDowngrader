package xyz.wagyourtail.jvmdg.version;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.cli.Flags;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Crafted-bytecode unit tests for the {@code --noIndy} post-chain sweep
 * ({@code java-api}'s {@code NoIndyContributor}/{@code NoIndyTransform},
 * appended at {@code AFTER_VERSION_CHAIN}).
 *
 * <p>Inputs are authored at V12 targeting V11: the j8 (lambda), j9 (concat)
 * and j11 (ldcCondyToIndy) provider boundaries are all below the chain, so
 * indy/condy sites survive the version chain untouched and only the sweep
 * removes them — this exercises the union of <em>all</em> providers'
 * {@code @IndyDesugar} registries, the generic {@code jvmdg$indy$}
 * fallback for unregistered bootstrap methods, and {@code jvmdg$condy$}
 * field lowering for {@code ldc <ConstantDynamic>}.</p>
 */
public class TestNoIndy {

    private static final Handle METAFACTORY = new Handle(Opcodes.H_INVOKESTATIC,
        "java/lang/invoke/LambdaMetafactory", "metafactory",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;" +
            "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;" +
            "Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)" +
            "Ljava/lang/invoke/CallSite;", false);
    private static final Handle ALT_METAFACTORY = new Handle(Opcodes.H_INVOKESTATIC,
        "java/lang/invoke/LambdaMetafactory", "altMetafactory",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;" +
            "Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)" +
            "Ljava/lang/invoke/CallSite;", false);
    private static final Handle MAKE_CONCAT = new Handle(Opcodes.H_INVOKESTATIC,
        "java/lang/invoke/StringConcatFactory", "makeConcat",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;" +
            "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
    private static final Handle USER_BSM = new Handle(Opcodes.H_INVOKESTATIC,
        "t/Boot", "bootstrap",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;" +
            "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
    private static final Handle CONDY_BSM = new Handle(Opcodes.H_INVOKESTATIC,
        "t/Boot", "condy",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)" +
            "Ljava/lang/Object;", false);
    private static final Handle GAINED_BSM = new Handle(Opcodes.H_INVOKESTATIC,
        "xyz/wagyourtail/jvmdg/stub/runtime/GainedRuntime", "bootstrap",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;" +
            "Ljava/lang/invoke/MethodType;Ljava/lang/String;Ljava/lang/String;I)" +
            "Ljava/lang/invoke/CallSite;", false);

    private static Flags flags(int version) {
        Flags f = new Flags();
        f.classVersion = version;
        f.noIndy = true;
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

    private static Map<String, ClassNode> downgrade(int target, ClassNode input) throws Exception {
        Map<String, byte[]> raw = new HashMap<>();
        raw.put(input.name, ASMUtils.classNodeToBytes(input, 0));
        Flags f = flags(target);
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(f)) {
            Map<String, byte[]> out = d.downgrade(new AtomicReference<>(), raw.get(input.name), false,
                name -> raw.get(name.replace(".", "/")));
            Map<String, ClassNode> nodes = new HashMap<>();
            for (Map.Entry<String, byte[]> e : (out == null ? raw : out).entrySet()) {
                ClassNode n = new ClassNode();
                new org.objectweb.asm.ClassReader(e.getValue()).accept(n, 0);
                nodes.put(n.name, n);
            }
            return nodes;
        }
    }

    private static void assertNoIndyOrCondy(Map<String, ClassNode> out) {
        for (ClassNode n : out.values()) {
            for (MethodNode m : n.methods) {
                for (AbstractInsnNode i : m.instructions) {
                    assertFalse(i instanceof InvokeDynamicInsnNode,
                        n.name + "." + m.name + " retains invokedynamic");
                    if (i instanceof LdcInsnNode) {
                        assertFalse(((LdcInsnNode) i).cst instanceof ConstantDynamic,
                            n.name + "." + m.name + " retains a ConstantDynamic ldc");
                    }
                }
            }
        }
    }

    private static FieldNode field(ClassNode n, String prefix) {
        for (FieldNode f : n.fields) {
            if (f.name.startsWith(prefix)) return f;
        }
        return null;
    }

    private static MethodNode find(ClassNode n, String prefix) {
        for (MethodNode m : n.methods) {
            if (m.name.startsWith(prefix)) return m;
        }
        return null;
    }

    @Test
    public void lambdaIndyDesugarsViaUnionRegistry() throws Exception {
        ClassNode caller = node(Opcodes.V12, Opcodes.ACC_PUBLIC, "t/Caller");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "get", "()Lt/Getter;");
        m.instructions.add(new InvokeDynamicInsnNode("get", "()Lt/Getter;", METAFACTORY,
            Type.getType("()Ljava/lang/Object;"),
            new Handle(Opcodes.H_INVOKESTATIC, "t/Impl", "impl", "()Ljava/lang/Object;", false),
            Type.getType("()Ljava/lang/Object;")));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        caller.methods.add(m);

        Map<String, ClassNode> out = downgrade(Opcodes.V11, caller);
        assertNoIndyOrCondy(out);
        ClassNode n = out.get("t/Caller");
        assertNotNull(find(n, "jvmdowngrader$lambda$"),
            "caller-side lambda factory method must be emitted");
        assertTrue(out.keySet().stream().anyMatch(k -> k.startsWith("t/Caller$")),
            "lambda impl class must be emitted via ctx.extras: " + out.keySet());
    }

    @Test
    public void altLambdaIndyDesugarsWithMarkersAndBridges() throws Exception {
        ClassNode caller = node(Opcodes.V12, Opcodes.ACC_PUBLIC, "t/AltCaller");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "get", "()Lt/Func;");
        // samMethodType (erased) differs from instantiatedMethodType → implicit bridge
        m.instructions.add(new InvokeDynamicInsnNode("f", "()Lt/Func;", ALT_METAFACTORY,
            Type.getType("()Ljava/lang/Object;"),
            new Handle(Opcodes.H_INVOKESTATIC, "t/Impl", "impl", "()Ljava/lang/String;", false),
            Type.getType("()Ljava/lang/String;"),
            2 | 4, // FLAG_MARKERS | FLAG_BRIDGES
            1, Type.getType("Lt/Marker;"),
            1, Type.getType("()I")));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        caller.methods.add(m);

        Map<String, ClassNode> out = downgrade(Opcodes.V11, caller);
        assertNoIndyOrCondy(out);
        ClassNode impl = out.get("t/AltCaller$1");
        assertNotNull(impl, "alt lambda impl class must be emitted via ctx.extras: " + out.keySet());
        assertTrue((impl.access & Opcodes.ACC_FINAL) != 0, "impl class must be final like JDK lambda impls");
        assertTrue(impl.interfaces.contains("t/Func"), "impl must implement the SAM: " + impl.interfaces);
        assertTrue(impl.interfaces.contains("t/Marker"), "impl must implement marker interfaces: " + impl.interfaces);
        assertTrue(hasMethod(impl, "f", "()Ljava/lang/String;"), "instantiated SAM method missing");
        assertTrue(hasMethod(impl, "f", "()Ljava/lang/Object;"), "implicit erased-sam bridge missing");
        assertTrue(hasMethod(impl, "f", "()I"), "explicit FLAG_BRIDGES method missing");
        // no FLAG_SERIALIZABLE → hostile methods instead of writeReplace
        assertTrue(hasMethod(impl, "readObject", "(Ljava/io/ObjectInputStream;)V"), "hostile readObject missing");
        assertTrue(hasMethod(impl, "writeObject", "(Ljava/io/ObjectOutputStream;)V"), "hostile writeObject missing");
        assertTrue(hasMethod(impl, "readObjectNoData", "()V"), "hostile readObjectNoData missing");
        assertNull(find(impl, "writeReplace"), "non-serializable impl must not get writeReplace");
        assertNull(find(out.get("t/AltCaller"), "$deserializeLambda$"), "non-serializable caller must not get $deserializeLambda$");
    }

    @Test
    public void altLambdaIndyDesugarsSerializable() throws Exception {
        ClassNode caller = node(Opcodes.V12, Opcodes.ACC_PUBLIC, "t/SerCaller");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "get", "()Lt/Func;");
        m.instructions.add(new InvokeDynamicInsnNode("f", "()Lt/Func;", ALT_METAFACTORY,
            Type.getType("()Ljava/lang/Object;"),
            new Handle(Opcodes.H_INVOKESTATIC, "t/SerCaller", "impl", "()Ljava/lang/String;", false),
            Type.getType("()Ljava/lang/Object;"),
            1 | 4, // FLAG_SERIALIZABLE | FLAG_BRIDGES
            0));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        caller.methods.add(m);

        Map<String, ClassNode> out = downgrade(Opcodes.V11, caller);
        assertNoIndyOrCondy(out);
        ClassNode impl = out.get("t/SerCaller$1");
        assertNotNull(impl, "serializable impl class must be emitted: " + out.keySet());
        assertTrue(impl.interfaces.contains("java/io/Serializable"), "impl must implement Serializable: " + impl.interfaces);
        assertTrue(impl.fields.stream().anyMatch(f -> f.name.equals("instantiatedMethodType")),
            "instantiatedMethodType field missing");
        assertTrue(hasMethod(impl, "writeReplace", "()Ljava/lang/Object;"), "writeReplace missing");
        assertNull(find(impl, "readObject"), "serializable impl must not get hostile readObject");
        ClassNode callerOut = out.get("t/SerCaller");
        MethodNode dser = null;
        for (MethodNode mn : callerOut.methods) {
            if (mn.name.equals("$deserializeLambda$")
                && mn.desc.equals("(Ljava/lang/invoke/SerializedLambda;)Ljava/lang/Object;")) {
                dser = mn;
                break;
            }
        }
        assertNotNull(dser, "caller must get $deserializeLambda$");
        boolean callsFactory = false;
        for (AbstractInsnNode i : dser.instructions) {
            if (i instanceof MethodInsnNode && ((MethodInsnNode) i).name.startsWith("jvmdowngrader$lambda$")) {
                callsFactory = true;
            }
        }
        assertTrue(callsFactory, "$deserializeLambda$ must dispatch to the lambda factory");
    }

    private static boolean hasMethod(ClassNode n, String name, String desc) {
        for (MethodNode mn : n.methods) {
            if (mn.name.equals(name) && mn.desc.equals(desc)) return true;
        }
        return false;
    }

    @Test
    public void concatIndyDesugars() throws Exception {
        ClassNode caller = node(Opcodes.V12, Opcodes.ACC_PUBLIC, "t/Concat");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "cat",
            "(Ljava/lang/String;)Ljava/lang/String;");
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new InvokeDynamicInsnNode("makeConcat",
            "(Ljava/lang/String;)Ljava/lang/String;", MAKE_CONCAT));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        caller.methods.add(m);

        Map<String, ClassNode> out = downgrade(Opcodes.V11, caller);
        assertNoIndyOrCondy(out);
    }

    @Test
    public void unknownBsmFallsBackToGenericLowering() throws Exception {
        ClassNode caller = node(Opcodes.V12, Opcodes.ACC_PUBLIC, "t/User");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "site", "()Ljava/lang/String;");
        m.instructions.add(new InvokeDynamicInsnNode("site", "()Ljava/lang/String;", USER_BSM));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        caller.methods.add(m);

        Map<String, ClassNode> out = downgrade(Opcodes.V11, caller);
        assertNoIndyOrCondy(out);
        ClassNode n = out.get("t/User");
        assertNotNull(field(n, "jvmdg$indy$"), "generic CallSite field must be emitted");
        assertNotNull(find(n, "jvmdg$indy$"), "generic dispatch method must be emitted");
        assertNotNull(find(n, "<clinit>"), "clinit must resolve the callsite once");
    }

    @Test
    public void ldcCondyLowersToSyntheticField() throws Exception {
        ClassNode caller = node(Opcodes.V12, Opcodes.ACC_PUBLIC, "t/Condy");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "cst", "()Ljava/lang/Object;");
        m.instructions.add(new LdcInsnNode(new ConstantDynamic("c", "Ljava/lang/Object;",
            CONDY_BSM)));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        caller.methods.add(m);

        Map<String, ClassNode> out = downgrade(Opcodes.V11, caller);
        assertNoIndyOrCondy(out);
        ClassNode n = out.get("t/Condy");
        FieldNode f = field(n, "jvmdg$condy$");
        assertNotNull(f, "condy must resolve to a synthetic field");
        assertEquals("Ljava/lang/Object;", f.desc);
    }

    @Test
    public void combinedSweepLeavesNoDynamicSites() throws Exception {
        ClassNode caller = node(Opcodes.V12, Opcodes.ACC_PUBLIC, "t/All");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "all", "()Ljava/lang/Object;");
        m.instructions.add(new LdcInsnNode(new ConstantDynamic("c", "Ljava/lang/Object;",
            CONDY_BSM)));
        m.instructions.add(new InsnNode(Opcodes.POP));
        m.instructions.add(new InvokeDynamicInsnNode("site", "()Ljava/lang/String;", USER_BSM));
        m.instructions.add(new InvokeDynamicInsnNode("makeConcat",
            "(Ljava/lang/String;)Ljava/lang/String;", MAKE_CONCAT));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        caller.methods.add(m);

        Map<String, ClassNode> out = downgrade(Opcodes.V11, caller);
        assertNoIndyOrCondy(out);
    }

    @Test
    public void gainedIndyDesugarsToGainedCall() throws Exception {
        ClassNode caller = node(Opcodes.V12, Opcodes.ACC_PUBLIC, "t/Gained");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "idx",
            "(Ljava/lang/Object;Ljava/lang/Object;)I");
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        // gained-site shape: receiver + ref args erased to Object,
        // primitive return kept; iName/member/ceiling ride bsmArgs
        m.instructions.add(new InvokeDynamicInsnNode("indexOf",
            "(Ljava/lang/Object;Ljava/lang/Object;)I", GAINED_BSM,
            "java/util/List", "indexOf (Ljava/lang/Object;)I", 52));
        m.instructions.add(new InsnNode(Opcodes.IRETURN));
        caller.methods.add(m);

        Map<String, ClassNode> out = downgrade(Opcodes.V11, caller);
        assertNoIndyOrCondy(out);
        MethodNode idx = null;
        for (MethodNode mn : out.get("t/Gained").methods) {
            if (mn.name.equals("idx")) idx = mn;
        }
        assertNotNull(idx);
        boolean callsGained = false;
        boolean sawIName = false;
        boolean sawMember = false;
        boolean sawUnbox = false;
        for (AbstractInsnNode i : idx.instructions) {
            if (i instanceof LdcInsnNode) {
                if ("java/util/List".equals(((LdcInsnNode) i).cst)) sawIName = true;
                if ("indexOf (Ljava/lang/Object;)I".equals(((LdcInsnNode) i).cst)) sawMember = true;
            }
            if (i instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) i;
                if (mi.owner.equals("xyz/wagyourtail/jvmdg/stub/runtime/RetroTypes")
                    && mi.name.equals("gainedCall")) callsGained = true;
                if (mi.name.equals("intValue")) sawUnbox = true;
            }
        }
        assertTrue(callsGained, "gained indy must lower to RetroTypes.gainedCall");
        assertTrue(sawIName && sawMember, "bsmArg constants must ride the emitted sequence");
        assertTrue(sawUnbox, "primitive return must unbox the Object result");
    }

    @Test
    public void sweepIsInertWithoutFlag() throws Exception {
        ClassNode caller = node(Opcodes.V12, Opcodes.ACC_PUBLIC, "t/Keep");
        MethodNode m = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "cat",
            "(Ljava/lang/String;)Ljava/lang/String;");
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new InvokeDynamicInsnNode("makeConcat",
            "(Ljava/lang/String;)Ljava/lang/String;", MAKE_CONCAT));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        caller.methods.add(m);

        Map<String, byte[]> raw = new HashMap<>();
        raw.put(caller.name, ASMUtils.classNodeToBytes(caller, 0));
        Flags f = new Flags();
        f.classVersion = Opcodes.V11;
        f.quiet = true;
        try (ClassDowngrader d = ClassDowngrader.downgradeTo(f)) {
            Map<String, byte[]> out = d.downgrade(new AtomicReference<>(), raw.get(caller.name),
                false, name -> raw.get(name.replace(".", "/")));
            ClassNode n = new ClassNode();
            new org.objectweb.asm.ClassReader(out.get("t/Keep")).accept(n, 0);
            boolean foundIndy = false;
            for (MethodNode mn : n.methods) {
                for (AbstractInsnNode i : mn.instructions) {
                    if (i instanceof InvokeDynamicInsnNode) foundIndy = true;
                }
            }
            assertTrue(foundIndy, "indy must pass through untouched without --noIndy");
        }
    }
}
