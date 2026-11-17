package xyz.wagyourtail.jvmdg.version.map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.map.ClassStubEntry;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Slot-coverage for {@link StructuralRemapTransform} against a fake
 * {@link TypeRemap}: {@code java/util/Optional} stubs to
 * {@code jvmdg/fake/J_U_Optional} whose backing class is {@link FakeAdapter}.
 */
public class TestStructuralRemapTransform {

    private static final Type REAL = Type.getObjectType("java/util/Optional");
    private static final Type FAKE = Type.getObjectType("jvmdg/fake/J_U_Optional");
    private static final Type OTHER = Type.getObjectType("java/lang/String");

    /** Stand-in adapter class — provides the jvmdg$ hooks the rewrite looks up reflectively. */
    public static class FakeAdapter {
        @SuppressWarnings("unused")
        public static boolean jvmdg$instanceof(Object o) {
            return o instanceof FakeAdapter;
        }

        @SuppressWarnings("unused")
        public static Object jvmdg$checkcast(Object o) {
            return o;
        }
    }

    /** Recursive map-backed remap mirroring {@code VersionProvider.stubClass}. */
    private static class FakeRemap implements TypeRemap {
        final Map<Type, ClassStubEntry> stubs = new HashMap<>();
        final Set<String> warned = new HashSet<>();

        @Override
        public Type remap(Type type, Set<String> warnings) {
            switch (type.getSort()) {
                case Type.METHOD:
                    Type[] args = type.getArgumentTypes();
                    for (int i = 0; i < args.length; i++) {
                        args[i] = remap(args[i], warnings);
                    }
                    return Type.getMethodType(remap(type.getReturnType(), warnings), args);
                case Type.ARRAY:
                    Type element = type.getElementType();
                    ClassStubEntry es = stubFor(element);
                    if (es != null) {
                        element = es.target;
                    }
                    return Type.getType(type.getDescriptor().substring(0, type.getDimensions()) + element.getDescriptor());
                case Type.OBJECT:
                    ClassStubEntry s = stubFor(type);
                    return s == null ? type : s.target;
                default:
                    return type;
            }
        }

        @Override
        public ClassStubEntry stubFor(Type type) {
            return stubs.get(type);
        }

        @Override
        public void warnUnstubbed(Type type, Set<String> warnings) {
            warned.add(type.getInternalName());
        }
    }

    private static FakeRemap remapWithAdapter() {
        FakeRemap r = new FakeRemap();
        r.stubs.put(REAL, new ClassStubEntry(FAKE, FakeAdapter.class, false,
            xyz.wagyourtail.jvmdg.version.Adapter.Mode.AUTO, null, null, null));
        return r;
    }

    private static FakeRemap remapWithoutAdapter() {
        FakeRemap r = new FakeRemap();
        r.stubs.put(REAL, new ClassStubEntry(FAKE, Object.class, false,
            xyz.wagyourtail.jvmdg.version.Adapter.Mode.AUTO, null, null, null));
        return r;
    }

    @Test
    public void methodInsnRemapsOwnerAndDescriptor() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        MethodInsnNode min = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "of", "(Ljava/lang/Object;)Ljava/util/Optional;");
        assertNull(t.remapInsn(min, new HashSet<String>(), 0, "test/Owner"));
        assertEquals("jvmdg/fake/J_U_Optional", min.owner);
        assertEquals("(Ljava/lang/Object;)Ljvmdg/fake/J_U_Optional;", min.desc);
    }

    @Test
    public void fieldInsnRemapsOwnerAndDescriptor() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        FieldInsnNode fin = new FieldInsnNode(Opcodes.GETFIELD, "java/util/Optional", "value", "Ljava/util/Optional;");
        t.remapInsn(fin, new HashSet<String>(), 0, "test/Owner");
        assertEquals("jvmdg/fake/J_U_Optional", fin.owner);
        assertEquals("Ljvmdg/fake/J_U_Optional;", fin.desc);
    }

    @Test
    public void indyRemapsDescAndBsmSlotsOnly() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        Handle bsm = new Handle(Opcodes.H_INVOKESTATIC, "java/util/Optional", "bsm", "(I)Ljava/util/Optional;", true);
        InvokeDynamicInsnNode indy = new InvokeDynamicInsnNode("run", "(Ljava/util/Optional;)V", bsm);
        t.remapInsn(indy, new HashSet<String>(), 0, "test/Owner");
        assertEquals("(Ljvmdg/fake/J_U_Optional;)V", indy.desc);
        assertEquals("jvmdg/fake/J_U_Optional", indy.bsm.getOwner());
        assertEquals("(I)Ljvmdg/fake/J_U_Optional;", indy.bsm.getDesc());
        assertEquals("bsm", indy.bsm.getName());
        assertEquals(Opcodes.H_INVOKESTATIC, indy.bsm.getTag());
        assertTrue(indy.bsm.isInterface());
    }

    @Test
    public void multiANewArrayRemapsElementType() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        MultiANewArrayInsnNode mana = new MultiANewArrayInsnNode("[[Ljava/util/Optional;", 2);
        t.remapInsn(mana, new HashSet<String>(), 0, "test/Owner");
        assertEquals("[[Ljvmdg/fake/J_U_Optional;", mana.desc);
    }

    @Test
    public void ldcTypeConstantRemapped() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        LdcInsnNode ldc = new LdcInsnNode(REAL);
        t.remapInsn(ldc, new HashSet<String>(), 0, "test/Owner");
        assertEquals(FAKE, ldc.cst);
    }

    @Test
    public void ldcNonTypeConstantUntouched() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        LdcInsnNode ldc = new LdcInsnNode("not a type");
        t.remapInsn(ldc, new HashSet<String>(), 0, "test/Owner");
        assertEquals("not a type", ldc.cst);
    }

    @Test
    public void frameStringSlotsRemappedOthersLeft() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        Label l = new Label();
        FrameNode fn = new FrameNode(Opcodes.F_FULL, 3,
            new Object[]{"java/util/Optional", Opcodes.INTEGER, l},
            2, new Object[]{"java/util/Optional", Opcodes.TOP});
        t.remapInsn(fn, new HashSet<String>(), 0, "test/Owner");
        assertEquals("jvmdg/fake/J_U_Optional", fn.local.get(0));
        assertEquals(Opcodes.INTEGER, fn.local.get(1));
        assertSame(l, fn.local.get(2));
        assertEquals("jvmdg/fake/J_U_Optional", fn.stack.get(0));
        assertEquals(Opcodes.TOP, fn.stack.get(1));
    }

    @Test
    public void newInsnOnStubbedTypeGetsAdapterOwner() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.NEW, "java/util/Optional");
        assertNull(t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner"));
        assertEquals("jvmdg/fake/J_U_Optional", tin.desc);
    }

    @Test
    public void anewarrayOnStubbedElementType() {
        // ANEWARRAY's operand is the element type — OBJECT sort, same path as NEW
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.ANEWARRAY, "java/util/Optional");
        assertNull(t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner"));
        assertEquals("jvmdg/fake/J_U_Optional", tin.desc);
    }

    @Test
    public void anewarrayWithArrayOperandRemapsElement() {
        // operand "[Ljava/util/Optional;" builds Optional[][] — the ARRAY path
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.ANEWARRAY, "[Ljava/util/Optional;");
        assertNull(t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner"));
        assertEquals("[Ljvmdg/fake/J_U_Optional;", tin.desc);
    }

    @Test
    public void instanceofRewritesToAdapterHook() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithAdapter());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.INSTANCEOF, "java/util/Optional");
        InsnList replacement = t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner");
        MethodInsnNode min = (MethodInsnNode) replacement.getFirst();
        assertEquals(Opcodes.INVOKESTATIC, min.getOpcode());
        assertEquals(Type.getInternalName(FakeAdapter.class), min.owner);
        assertEquals("jvmdg$instanceof", min.name);
        assertFalse(min.itf);
    }

    @Test
    public void checkcastRewritesToAdapterHook() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithAdapter());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.CHECKCAST, "java/util/Optional");
        InsnList replacement = t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner");
        MethodInsnNode min = (MethodInsnNode) replacement.getFirst();
        assertEquals("jvmdg$checkcast", min.name);
        assertEquals(Type.getInternalName(FakeAdapter.class), min.owner);
    }

    @Test
    public void checkcastOwnInterfaceEmitsGeneratedWrap() {
        // own-interface stub with no hook and no wrap factory: the generated
        // per-adapter dispatcher — real bytecode adaptation (impls pull up
        // to ancestors, wrapImpl is the table fallback) then the adapter
        // checkcast keeps the stack type. Uniform emission keeps scan/idx
        // parity: every OWN_INTERFACE adapter gets a dispatcher, impls
        // never gate the emitted shape.
        FakeRemap r = new FakeRemap();
        r.stubs.put(REAL, new ClassStubEntry(REAL, Optional.class, false,
            xyz.wagyourtail.jvmdg.version.Adapter.Mode.AUTO, null, null, null));
        StructuralRemapTransform t = new StructuralRemapTransform(r);
        TypeInsnNode tin = new TypeInsnNode(Opcodes.CHECKCAST, "java/util/Optional");
        InsnList out = t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner");
        MethodInsnNode call = (MethodInsnNode) out.get(0);
        assertEquals(Opcodes.INVOKESTATIC, call.getOpcode());
        assertEquals("java/util/Optional$jvmdg$retro", call.owner);
        assertEquals("wrap", call.name);
        assertEquals("(Ljava/lang/Object;)Ljava/lang/Object;", call.desc);
        TypeInsnNode verify = (TypeInsnNode) out.get(1);
        assertEquals(Opcodes.CHECKCAST, verify.getOpcode());
        assertEquals(Type.getInternalName(Optional.class), verify.desc);
        assertEquals(2, out.size());
    }

    @Test
    public void unstubbedTypeWarnsAndPassesThrough() {
        FakeRemap r = remapWithoutAdapter();
        StructuralRemapTransform t = new StructuralRemapTransform(r);
        TypeInsnNode tin = new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/String");
        assertNull(t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner"));
        assertEquals("java/lang/String", tin.desc);
        assertTrue(r.warned.contains("java/lang/String"));
    }

    @Test
    public void unstubbedArrayElementWarns() {
        FakeRemap r = remapWithoutAdapter();
        StructuralRemapTransform t = new StructuralRemapTransform(r);
        TypeInsnNode tin = new TypeInsnNode(Opcodes.ANEWARRAY, "[Ljava/lang/String;");
        t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner");
        assertTrue(r.warned.contains("java/lang/String"));
    }

    @Test
    public void nonTypeInsnReturnsNull() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        assertNull(t.remapInsn(new InsnNode(Opcodes.NOP), new HashSet<String>(), 0, "test/Owner"));
        assertNull(t.remapInsn(new VarInsnNode(Opcodes.ALOAD, 0), new HashSet<String>(), 0, "test/Owner"));
    }

    private static final Type SEQ = Type.getObjectType("jvmdg/retro/SeqCol");
    /** generated dispatcher hanging off the SUPERTYPE api adapter (stubType) */
    private static final String SEQ_DISP = "jvmdg/stub/SeqCol$jvmdg$retro";

    /** SUPERTYPE entry with membership data — emits the RetroTypes helpers. */
    private static FakeRemap remapWithSupertype() {
        FakeRemap r = new FakeRemap();
        r.stubs.put(SEQ, new ClassStubEntry(
            Type.getObjectType("java/util/Collection"), Type.getObjectType("jvmdg/stub/SeqCol"),
            false, true, null, null, new int[0],
            xyz.wagyourtail.jvmdg.version.Adapter.Mode.SUPERTYPE,
            Collections.singleton("java/util/List"), null, null, null));
        return r;
    }

    /** hooked SUPERTYPE entry — the explicit jvmdg$ hook must still win. */
    private static FakeRemap remapWithHookedSupertype() {
        FakeRemap r = new FakeRemap();
        r.stubs.put(SEQ, new ClassStubEntry(
            Type.getObjectType("java/util/Collection"), Type.getObjectType("jvmdg/stub/SeqCol"),
            false, true, FullyQualifiedMemberNameAndDesc.of("Ljvmdg/stub/SeqCol;jvmdg$instanceof;(Ljava/lang/Object;)Z"),
            null, new int[0],
            xyz.wagyourtail.jvmdg.version.Adapter.Mode.SUPERTYPE,
            Collections.singleton("java/util/List"), null, null, null));
        return r;
    }

    /** OWN_INTERFACE entry with a wrap factory for checkcast-by-adaptation. */
    private static FakeRemap remapWithWrap() {
        FakeRemap r = new FakeRemap();
        r.stubs.put(SEQ, new ClassStubEntry(
            Type.getObjectType("jvmdg/stub/OwnType"), Type.getObjectType("jvmdg/stub/OwnType"),
            false, true, null, null, new int[0],
            xyz.wagyourtail.jvmdg.version.Adapter.Mode.OWN_INTERFACE,
            Collections.singleton("java/util/List"),
            FullyQualifiedMemberNameAndDesc.of("Ljvmdg/stub/Wraps;adapt;(Ljava/lang/Object;)Ljvmdg/stub/OwnType;"), null, null));
        return r;
    }

    @Test
    public void instanceofSupertypeEmitsDispatcherCall() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithSupertype());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.INSTANCEOF, "jvmdg/retro/SeqCol");
        InsnList out = t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner");
        MethodInsnNode call = (MethodInsnNode) out.get(0);
        assertEquals(Opcodes.INVOKESTATIC, call.getOpcode());
        assertEquals(SEQ_DISP, call.owner);
        assertEquals("isInstance", call.name);
        assertEquals("(Ljava/lang/Object;)Z", call.desc);
        assertEquals(1, out.size());
    }

    @Test
    public void checkcastSupertypeEmitsCastThenTargetCheckcast() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithSupertype());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.CHECKCAST, "jvmdg/retro/SeqCol");
        InsnList out = t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner");
        MethodInsnNode call = (MethodInsnNode) out.get(0);
        assertEquals(SEQ_DISP, call.owner);
        assertEquals("cast", call.name);
        assertEquals("(Ljava/lang/Object;)Ljava/lang/Object;", call.desc);
        TypeInsnNode verify = (TypeInsnNode) out.get(1);
        assertEquals(Opcodes.CHECKCAST, verify.getOpcode());
        assertEquals("java/util/Collection", verify.desc);
        assertEquals(2, out.size());
    }

    @Test
    public void arrayInstanceofSupertypeEmitsArrayHelper() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithSupertype());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.INSTANCEOF, "[Ljvmdg/retro/SeqCol;");
        InsnList out = t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner");
        MethodInsnNode call = (MethodInsnNode) out.get(0);
        assertEquals("isArrayInstance", call.name);
        assertEquals(SEQ_DISP, call.owner);
        assertEquals(1, out.size());
    }

    @Test
    public void arrayCheckcastSupertypeEmitsHelperThenRemappedCheckcast() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithSupertype());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.CHECKCAST, "[[Ljvmdg/retro/SeqCol;");
        InsnList out = t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner");
        MethodInsnNode call = (MethodInsnNode) out.get(0);
        assertEquals("castArray", call.name);
        assertEquals(SEQ_DISP, call.owner);
        TypeInsnNode verify = (TypeInsnNode) out.get(1);
        assertEquals("[[Ljava/util/Collection;", verify.desc);
        assertEquals(2, out.size());
    }

    @Test
    public void instanceofWithoutDerivedMembershipStillEmitsRetro() {
        // parity invariant: implementedBy is idx-only data (the scan path
        // leaves it empty), so emission must gate on mode alone — the
        // runtime table answers membership, not the bytecode
        FakeRemap r = new FakeRemap();
        r.stubs.put(SEQ, new ClassStubEntry(
            Type.getObjectType("java/util/Collection"), Type.getObjectType("jvmdg/stub/SeqCol"),
            false, true, null, null, new int[0],
            xyz.wagyourtail.jvmdg.version.Adapter.Mode.SUPERTYPE, null, null, null, null));
        StructuralRemapTransform t = new StructuralRemapTransform(r);
        TypeInsnNode tin = new TypeInsnNode(Opcodes.INSTANCEOF, "jvmdg/retro/SeqCol");
        InsnList out = t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner");
        assertNotNull(out);
        assertEquals("isInstance", ((MethodInsnNode) out.get(0)).name);
    }

    @Test
    public void checkcastOwnInterfaceEmitsWrapFactory() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithWrap());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.CHECKCAST, "jvmdg/retro/SeqCol");
        InsnList out = t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner");
        MethodInsnNode call = (MethodInsnNode) out.getFirst();
        assertEquals(Opcodes.INVOKESTATIC, call.getOpcode());
        assertEquals("jvmdg/stub/Wraps", call.owner);
        assertEquals("adapt", call.name);
        assertEquals(1, out.size());
    }

    @Test
    public void instanceofOwnInterfaceUsesMembershipHelper() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithWrap());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.INSTANCEOF, "jvmdg/retro/SeqCol");
        InsnList out = t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner");
        assertEquals("isInstance", ((MethodInsnNode) out.get(0)).name);
        assertEquals("jvmdg/stub/OwnType$jvmdg$retro", ((MethodInsnNode) out.get(0)).owner);
    }

    @Test
    public void explicitHookStillOverridesRetroEmission() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithHookedSupertype());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.INSTANCEOF, "jvmdg/retro/SeqCol");
        InsnList out = t.remapInsn(tin, new HashSet<String>(), 0, "test/Owner");
        MethodInsnNode call = (MethodInsnNode) out.getFirst();
        assertEquals("jvmdg$instanceof", call.name);
        assertEquals("jvmdg/stub/SeqCol", call.owner);
        assertTrue(call.itf, "hook invoke must carry the adapter's interface bit");
        assertEquals(1, out.size());
    }

    /** {@code static boolean m(Object o) { return <insns> } } — analyzable shape. */
    private static MethodNode queryMethod(AbstractInsnNode... insns) {
        MethodNode m = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "m", "(Ljava/lang/Object;)Z", null, null);
        m.instructions = new InsnList();
        for (AbstractInsnNode i : insns) {
            m.instructions.add(i);
        }
        m.maxLocals = 1;
        m.maxStack = 4;
        return m;
    }

    private static MethodInsnNode classQuery(String name, String desc) {
        return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", name, desc, false);
    }

    private static InsnList routed(StructuralRemapTransform t, MethodNode m) {
        java.util.Map<AbstractInsnNode, InsnList> map = t.remapClassCalls("com/example/Owner", m, 0);
        MethodInsnNode call = (MethodInsnNode) m.instructions.get(m.instructions.size() - 2);
        return map.get(call);
    }

    @Test
    public void classIsInstanceOnRetroLdcRoutesToRetroTypes() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithSupertype());
        MethodNode m = queryMethod(
            new LdcInsnNode(SEQ),
            new VarInsnNode(Opcodes.ALOAD, 0),
            classQuery("isInstance", "(Ljava/lang/Object;)Z"),
            new InsnNode(Opcodes.IRETURN));
        InsnList out = routed(t, m);
        assertNotNull(out, "X.class.isInstance on a stubbed type must route through the dispatcher");
        assertEquals(Opcodes.SWAP, out.get(0).getOpcode());
        assertEquals(Opcodes.POP, out.get(1).getOpcode());
        MethodInsnNode call = (MethodInsnNode) out.get(2);
        assertEquals("isInstance", call.name);
        assertEquals(SEQ_DISP, call.owner);
        assertEquals("(Ljava/lang/Object;)Z", call.desc);
        assertEquals(3, out.size());
    }

    @Test
    public void classCastOnRetroLdcRoutesToRetroTypes() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithSupertype());
        MethodNode m = queryMethod(
            new LdcInsnNode(SEQ),
            new VarInsnNode(Opcodes.ALOAD, 0),
            classQuery("cast", "(Ljava/lang/Object;)Ljava/lang/Object;"),
            new InsnNode(Opcodes.POP),
            new InsnNode(Opcodes.ICONST_0),
            new InsnNode(Opcodes.IRETURN));
        java.util.Map<AbstractInsnNode, InsnList> map = t.remapClassCalls("com/example/Owner", m, 0);
        InsnList out = map.get(m.instructions.get(2));
        assertNotNull(out);
        assertEquals("cast", ((MethodInsnNode) out.get(2)).name);
        assertEquals(SEQ_DISP, ((MethodInsnNode) out.get(2)).owner);
    }

    @Test
    public void classCastOwnInterfaceRoutesToGeneratedWrap() {
        // OWN_INTERFACE receivers must adapt (cast() would return the raw
        // member) — emits the table-driven wrap with the adapter ldc as the
        // impl-resolution loader context, matching checkcast emission
        FakeRemap r = new FakeRemap();
        r.stubs.put(SEQ, new ClassStubEntry(
            Type.getObjectType("jvmdg/stub/OwnType"), Type.getObjectType("jvmdg/stub/OwnType"),
            false, true, null, null, new int[0],
            xyz.wagyourtail.jvmdg.version.Adapter.Mode.OWN_INTERFACE,
            Collections.singleton("java/util/List"), null, null, null));
        StructuralRemapTransform t = new StructuralRemapTransform(r);
        MethodNode m = queryMethod(
            new LdcInsnNode(SEQ),
            new VarInsnNode(Opcodes.ALOAD, 0),
            classQuery("cast", "(Ljava/lang/Object;)Ljava/lang/Object;"),
            new InsnNode(Opcodes.POP),
            new InsnNode(Opcodes.ICONST_0),
            new InsnNode(Opcodes.IRETURN));
        java.util.Map<AbstractInsnNode, InsnList> map = t.remapClassCalls("com/example/Owner", m, 0);
        InsnList out = map.get(m.instructions.get(2));
        assertNotNull(out);
        assertEquals(Opcodes.SWAP, out.get(0).getOpcode());
        assertEquals(Opcodes.POP, out.get(1).getOpcode());
        MethodInsnNode call = (MethodInsnNode) out.get(2);
        assertEquals("wrap", call.name);
        assertEquals("jvmdg/stub/OwnType$jvmdg$retro", call.owner);
        assertEquals("(Ljava/lang/Object;)Ljava/lang/Object;", call.desc);
        assertEquals(3, out.size());
    }

    @Test
    public void isAssignableFromOnRetroLdcRoutesWithDims() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithSupertype());
        MethodNode m = queryMethod(
            new LdcInsnNode(SEQ),
            new VarInsnNode(Opcodes.ALOAD, 0),
            classQuery("isAssignableFrom", "(Ljava/lang/Class;)Z"),
            new InsnNode(Opcodes.IRETURN));
        InsnList out = routed(t, m);
        assertNotNull(out);
        assertEquals(Opcodes.ICONST_0, out.get(2).getOpcode(), "scalar receiver carries dims=0");
        MethodInsnNode call = (MethodInsnNode) out.get(3);
        assertEquals("isAssignableFrom", call.name);
        assertEquals(SEQ_DISP, call.owner);
        assertEquals("(Ljava/lang/Class;I)Z", call.desc);
    }

    @Test
    public void arrayClassLdcRoutesToArrayForm() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithSupertype());
        MethodNode m = queryMethod(
            new LdcInsnNode(Type.getType("[Ljvmdg/retro/SeqCol;")),
            new VarInsnNode(Opcodes.ALOAD, 0),
            classQuery("isInstance", "(Ljava/lang/Object;)Z"),
            new InsnNode(Opcodes.IRETURN));
        InsnList out = routed(t, m);
        assertNotNull(out, "X[].class.isInstance must route to the element-membership form");
        MethodInsnNode call = (MethodInsnNode) out.get(2);
        assertEquals("isArrayInstance", call.name);
        assertEquals(SEQ_DISP, call.owner);
    }

    @Test
    public void nonRetroClassLdcLeftAlone() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithSupertype());
        MethodNode m = queryMethod(
            new LdcInsnNode(Type.getObjectType("java/lang/String")),
            new VarInsnNode(Opcodes.ALOAD, 0),
            classQuery("isInstance", "(Ljava/lang/Object;)Z"),
            new InsnNode(Opcodes.IRETURN));
        assertTrue(t.remapClassCalls("com/example/Owner", m, 0).isEmpty());
    }

    @Test
    public void opaqueReceiverLeftAlone() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithSupertype());
        MethodNode m = queryMethod(
            new VarInsnNode(Opcodes.ALOAD, 0),
            new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false),
            new VarInsnNode(Opcodes.ALOAD, 0),
            classQuery("isInstance", "(Ljava/lang/Object;)Z"),
            new InsnNode(Opcodes.IRETURN));
        assertTrue(t.remapClassCalls("com/example/Owner", m, 0).isEmpty(),
            "non-ldc receivers can't carry retro identity — remapped-native stays");
    }

    // --- gained-supertype routing ----------------------------------------
    //
    // A gained interface is real at the output — no stub entry — so
    // routing keys on TypeRemap.gainedFor and emits the interface's
    // generated dispatcher with the authored class-file version as the
    // trailing ceiling argument.

    private static final Type AC = Type.getObjectType("java/lang/AutoCloseable");
    private static final String AC_DISP = "xyz/wagyourtail/jvmdg/gen/gained/J_L_AutoCloseable";

    /** gained-capable remap — AutoCloseable reports its generated dispatcher. */
    private static FakeRemap remapWithGained() {
        return new FakeRemap() {
            @Override
            public String gainedFor(Type type) {
                return AC.equals(type) ? GainedSites.dispatchOwner(type.getInternalName()) : null;
            }
        };
    }

    /** the operand of an int-const insn — ICONST_* encode in the opcode, BIPUSH/SIPUSH in the operand */
    private static int intOperand(AbstractInsnNode insn) {
        if (insn instanceof IntInsnNode) {
            return ((IntInsnNode) insn).operand;
        }
        return insn.getOpcode() - Opcodes.ICONST_0;
    }

    @Test
    public void instanceofGainedRoutesToDispatcherWithCeiling() {
        FakeRemap r = remapWithGained();
        StructuralRemapTransform t = new StructuralRemapTransform(r);
        TypeInsnNode tin = new TypeInsnNode(Opcodes.INSTANCEOF, "java/lang/AutoCloseable");
        InsnList out = t.remapInsn(tin, new HashSet<String>(), 65, "test/Owner");
        assertNotNull(out, "instanceof on a gained interface must route to its dispatcher");
        assertEquals(2, out.size());
        assertEquals(65, intOperand(out.get(0)), "the authored version is the membership ceiling");
        MethodInsnNode call = (MethodInsnNode) out.get(1);
        assertEquals("jvmdg$isInstance", call.name);
        assertEquals(AC_DISP, call.owner);
        assertEquals("(Ljava/lang/Object;I)Z", call.desc);
        assertFalse(call.itf);
        assertTrue(r.warned.isEmpty(), "a routed gained interface is not an unstubbed type");
    }

    @Test
    public void checkcastGainedEmitsWrapProducingDispatcherCall() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithGained());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/AutoCloseable");
        InsnList out = t.remapInsn(tin, new HashSet<String>(), 65, "test/Owner");
        assertNotNull(out);
        assertEquals(2, out.size());
        assertEquals(65, intOperand(out.get(0)));
        MethodInsnNode call = (MethodInsnNode) out.get(1);
        assertEquals("jvmdg$cast", call.name);
        // the dispatcher returns the real I — what the verifier needs on stack
        assertEquals("(Ljava/lang/Object;I)Ljava/lang/AutoCloseable;", call.desc);
        assertEquals(AC_DISP, call.owner);
    }

    @Test
    public void arrayInstanceofGainedElementEmitsArrayHelper() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithGained());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.INSTANCEOF, "[Ljava/lang/AutoCloseable;");
        InsnList out = t.remapInsn(tin, new HashSet<String>(), 65, "test/Owner");
        assertNotNull(out, "instanceof on a gained-element array routes to the array helper");
        assertEquals(65, intOperand(out.get(0)));
        MethodInsnNode call = (MethodInsnNode) out.get(1);
        assertEquals("jvmdg$isArrayInstance", call.name);
        assertEquals(AC_DISP, call.owner);
    }

    @Test
    public void arrayCheckcastGainedElementStaysNative() {
        // member arrays can't be fabricated — the native checkcast's CCE
        // is the honest answer; the element type still isn't a warning
        FakeRemap r = remapWithGained();
        StructuralRemapTransform t = new StructuralRemapTransform(r);
        TypeInsnNode tin = new TypeInsnNode(Opcodes.CHECKCAST, "[Ljava/lang/AutoCloseable;");
        assertNull(t.remapInsn(tin, new HashSet<String>(), 65, "test/Owner"));
        assertTrue(r.warned.isEmpty());
    }

    @Test
    public void generatedDispatcherKeepsLiteralInstanceof() {
        // the api jar's own self-downgrade passes generated dispatchers
        // through this transform — routing their literal fast-path
        // `instanceof I` would emit a self-recursive jvmdg$isInstance call
        FakeRemap r = remapWithGained();
        StructuralRemapTransform t = new StructuralRemapTransform(r);
        TypeInsnNode tin = new TypeInsnNode(Opcodes.INSTANCEOF, "java/lang/AutoCloseable");
        assertNull(t.remapInsn(tin, new HashSet<String>(), 51,
            "xyz/wagyourtail/jvmdg/gen/gained/J_L_AutoCloseable"),
            "generated dispatcher internals must stay literal");
        assertNull(t.remapInsn(tin, new HashSet<String>(), 51,
            "xyz/wagyourtail/jvmdg/stub/java_base/J_L_AutoCloseable$jvmdg$retro"));
        assertTrue(r.warned.isEmpty(), "machinery sites are not coverage warnings");
    }

    @Test
    public void nonGainedInterfaceInstanceofWarnsAndStays() {
        FakeRemap r = remapWithGained();
        StructuralRemapTransform t = new StructuralRemapTransform(r);
        TypeInsnNode tin = new TypeInsnNode(Opcodes.INSTANCEOF, "java/lang/Appendable");
        assertNull(t.remapInsn(tin, new HashSet<String>(), 65, "test/Owner"));
        assertTrue(r.warned.contains("java/lang/Appendable"));
    }

    @Test
    public void classIsInstanceOnGainedLdcRoutesWithCeiling() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithGained());
        MethodNode m = queryMethod(
            new LdcInsnNode(AC),
            new VarInsnNode(Opcodes.ALOAD, 0),
            classQuery("isInstance", "(Ljava/lang/Object;)Z"),
            new InsnNode(Opcodes.IRETURN));
        java.util.Map<AbstractInsnNode, InsnList> map = t.remapClassCalls("com/example/Owner", m, 65);
        InsnList out = map.get(m.instructions.get(2));
        assertNotNull(out, "I.class.isInstance on a gained interface must route");
        assertEquals(Opcodes.SWAP, out.get(0).getOpcode());
        assertEquals(Opcodes.POP, out.get(1).getOpcode());
        assertEquals(65, intOperand(out.get(2)));
        MethodInsnNode call = (MethodInsnNode) out.get(3);
        assertEquals("jvmdg$isInstance", call.name);
        assertEquals(AC_DISP, call.owner);
    }

    @Test
    public void classCastOnGainedLdcEmitsRealTypedResult() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithGained());
        MethodNode m = queryMethod(
            new LdcInsnNode(AC),
            new VarInsnNode(Opcodes.ALOAD, 0),
            classQuery("cast", "(Ljava/lang/Object;)Ljava/lang/Object;"),
            new InsnNode(Opcodes.POP),
            new InsnNode(Opcodes.ICONST_0),
            new InsnNode(Opcodes.IRETURN));
        java.util.Map<AbstractInsnNode, InsnList> map = t.remapClassCalls("com/example/Owner", m, 65);
        InsnList out = map.get(m.instructions.get(2));
        assertNotNull(out);
        MethodInsnNode call = (MethodInsnNode) out.get(3);
        assertEquals("jvmdg$cast", call.name);
        assertEquals("(Ljava/lang/Object;I)Ljava/lang/AutoCloseable;", call.desc);
    }

    @Test
    public void arrayIsAssignableFromOnGainedLdcCarriesDims() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithGained());
        MethodNode m = queryMethod(
            new LdcInsnNode(Type.getType("[Ljava/lang/AutoCloseable;")),
            new VarInsnNode(Opcodes.ALOAD, 0),
            classQuery("isAssignableFrom", "(Ljava/lang/Class;)Z"),
            new InsnNode(Opcodes.IRETURN));
        java.util.Map<AbstractInsnNode, InsnList> map = t.remapClassCalls("com/example/Owner", m, 65);
        InsnList out = map.get(m.instructions.get(2));
        assertNotNull(out);
        assertEquals(1, intOperand(out.get(2)), "the array receiver's dims");
        assertEquals(65, intOperand(out.get(3)), "the authored ceiling");
        MethodInsnNode call = (MethodInsnNode) out.get(4);
        assertEquals("jvmdg$isArrayAssignableFrom", call.name);
        assertEquals("(Ljava/lang/Class;II)Z", call.desc);
    }

    @Test
    public void arrayCastOnGainedLdcIsNotRouted() {
        // member arrays can't be fabricated — cast stays native
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithGained());
        MethodNode m = queryMethod(
            new LdcInsnNode(Type.getType("[Ljava/lang/AutoCloseable;")),
            new VarInsnNode(Opcodes.ALOAD, 0),
            classQuery("cast", "(Ljava/lang/Object;)Ljava/lang/Object;"),
            new InsnNode(Opcodes.POP),
            new InsnNode(Opcodes.ICONST_0),
            new InsnNode(Opcodes.IRETURN));
        assertTrue(t.remapClassCalls("com/example/Owner", m, 65).isEmpty());
    }

    // --- gained-element array stores ---------------------------------------
    //
    // aastore into an I-component array is the one place the runtime DOES
    // check the gained edge — a raw member throws ArrayStoreException. The
    // Analyzer (BasicValue) types the array operand; a gained element type
    // routes the stored value through the dispatcher's jvmdg$aastore wrap.

    /** {@code static void m(AutoCloseable[] a, Object v) { a[0] = v; } } */
    private static MethodNode storeMethod(String arrayDesc) {
        MethodNode m = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "m",
            "(" + arrayDesc + "Ljava/lang/Object;)V", null, null);
        m.instructions = new InsnList();
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new InsnNode(Opcodes.ICONST_0));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        m.instructions.add(new InsnNode(Opcodes.AASTORE));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        m.maxLocals = 2;
        m.maxStack = 4;
        return m;
    }

    @Test
    public void aastoreIntoGainedElementArrayRoutesToWrapHelper() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithGained());
        MethodNode m = storeMethod("[Ljava/lang/AutoCloseable;");
        java.util.Map<AbstractInsnNode, InsnList> map = t.remapClassCalls("com/example/Owner", m, 65);
        InsnList out = map.get(m.instructions.get(3));
        assertNotNull(out, "aastore into a gained-element array must route the value");
        assertEquals(65, intOperand(out.get(0)), "the authored ceiling");
        MethodInsnNode call = (MethodInsnNode) out.get(1);
        assertEquals("jvmdg$aastore", call.name);
        assertEquals(AC_DISP, call.owner);
        assertEquals("(Ljava/lang/Object;I)Ljava/lang/AutoCloseable;", call.desc);
        assertFalse(call.itf);
        assertEquals(Opcodes.AASTORE, out.get(2).getOpcode(),
            "the native store runs after the value is wrapped");
    }

    @Test
    public void aastoreIntoPlainObjectArrayNotRouted() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithGained());
        MethodNode m = storeMethod("[Ljava/lang/Object;");
        assertTrue(t.remapClassCalls("com/example/Owner", m, 65).isEmpty(),
            "an Object[] store has no gained element check to emulate");
    }

    @Test
    public void aastoreIntoMultiDimGainedArrayNotRouted() {
        // AutoCloseable[][] — the stored value is an AutoCloseable[]; member
        // arrays can't be fabricated into I[] so there is no wrap to emit
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithGained());
        MethodNode m = storeMethod("[[Ljava/lang/AutoCloseable;");
        assertTrue(t.remapClassCalls("com/example/Owner", m, 65).isEmpty());
    }

    @Test
    public void aastoreBeneathCatchAllTopValueIsNotRouted() {
        // a catch-all handler makes Analyzer push a null-typed TOP value for
        // the caught exception (BasicValue.REFERENCE_VALUE semantics) — a
        // store+load spreads it to the aastore's array slot, where the
        // consumer must skip it rather than dereference a null frame value
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithGained());
        MethodNode m = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "m", "()V", null, null);
        LabelNode l0 = new LabelNode(), l1 = new LabelNode(), h = new LabelNode();
        m.instructions = new InsnList();
        m.instructions.add(l0);
        m.instructions.add(new InsnNode(Opcodes.NOP));
        m.instructions.add(l1);
        m.instructions.add(h);
        m.instructions.add(new VarInsnNode(Opcodes.ASTORE, 0));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new InsnNode(Opcodes.ICONST_0));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new InsnNode(Opcodes.AASTORE));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        m.tryCatchBlocks = Collections.singletonList(new TryCatchBlockNode(l0, l1, h, null));
        m.maxLocals = 1;
        m.maxStack = 3;
        assertTrue(t.remapClassCalls("com/example/Owner", m, 65).isEmpty(),
            "a TOP-valued store operand has no element type to route");
    }

}
