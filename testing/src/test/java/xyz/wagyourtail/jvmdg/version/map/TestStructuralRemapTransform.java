package xyz.wagyourtail.jvmdg.version.map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Adapter;

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
        final Map<Type, Pair<Type, Pair<Class<?>, Adapter>>> stubs = new HashMap<>();
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
                    Pair<Type, Pair<Class<?>, Adapter>> es = stubFor(element);
                    if (es != null) {
                        element = es.getFirst();
                    }
                    return Type.getType(type.getDescriptor().substring(0, type.getDimensions()) + element.getDescriptor());
                case Type.OBJECT:
                    Pair<Type, Pair<Class<?>, Adapter>> s = stubFor(type);
                    return s == null ? type : s.getFirst();
                default:
                    return type;
            }
        }

        @Override
        public Pair<Type, Pair<Class<?>, Adapter>> stubFor(Type type) {
            return stubs.get(type);
        }

        @Override
        public void warnUnstubbed(Type type, Set<String> warnings) {
            warned.add(type.getInternalName());
        }
    }

    private static FakeRemap remapWithAdapter() {
        FakeRemap r = new FakeRemap();
        r.stubs.put(REAL, new Pair<Type, Pair<Class<?>, Adapter>>(FAKE, new Pair<Class<?>, Adapter>(FakeAdapter.class, null)));
        return r;
    }

    private static FakeRemap remapWithoutAdapter() {
        FakeRemap r = new FakeRemap();
        r.stubs.put(REAL, new Pair<Type, Pair<Class<?>, Adapter>>(FAKE, new Pair<Class<?>, Adapter>(Object.class, null)));
        return r;
    }

    @Test
    public void methodInsnRemapsOwnerAndDescriptor() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        MethodInsnNode min = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "of", "(Ljava/lang/Object;)Ljava/util/Optional;");
        assertNull(t.remapInsn(min, new HashSet<String>()));
        assertEquals("jvmdg/fake/J_U_Optional", min.owner);
        assertEquals("(Ljava/lang/Object;)Ljvmdg/fake/J_U_Optional;", min.desc);
    }

    @Test
    public void fieldInsnRemapsOwnerAndDescriptor() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        FieldInsnNode fin = new FieldInsnNode(Opcodes.GETFIELD, "java/util/Optional", "value", "Ljava/util/Optional;");
        t.remapInsn(fin, new HashSet<String>());
        assertEquals("jvmdg/fake/J_U_Optional", fin.owner);
        assertEquals("Ljvmdg/fake/J_U_Optional;", fin.desc);
    }

    @Test
    public void indyRemapsDescAndBsmSlotsOnly() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        Handle bsm = new Handle(Opcodes.H_INVOKESTATIC, "java/util/Optional", "bsm", "(I)Ljava/util/Optional;", true);
        InvokeDynamicInsnNode indy = new InvokeDynamicInsnNode("run", "(Ljava/util/Optional;)V", bsm);
        t.remapInsn(indy, new HashSet<String>());
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
        t.remapInsn(mana, new HashSet<String>());
        assertEquals("[[Ljvmdg/fake/J_U_Optional;", mana.desc);
    }

    @Test
    public void ldcTypeConstantRemapped() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        LdcInsnNode ldc = new LdcInsnNode(REAL);
        t.remapInsn(ldc, new HashSet<String>());
        assertEquals(FAKE, ldc.cst);
    }

    @Test
    public void ldcNonTypeConstantUntouched() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        LdcInsnNode ldc = new LdcInsnNode("not a type");
        t.remapInsn(ldc, new HashSet<String>());
        assertEquals("not a type", ldc.cst);
    }

    @Test
    public void frameStringSlotsRemappedOthersLeft() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        Label l = new Label();
        FrameNode fn = new FrameNode(Opcodes.F_FULL, 3,
            new Object[]{"java/util/Optional", Opcodes.INTEGER, l},
            2, new Object[]{"java/util/Optional", Opcodes.TOP});
        t.remapInsn(fn, new HashSet<String>());
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
        assertNull(t.remapInsn(tin, new HashSet<String>()));
        assertEquals("jvmdg/fake/J_U_Optional", tin.desc);
    }

    @Test
    public void anewarrayOnStubbedElementType() {
        // ANEWARRAY's operand is the element type — OBJECT sort, same path as NEW
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.ANEWARRAY, "java/util/Optional");
        assertNull(t.remapInsn(tin, new HashSet<String>()));
        assertEquals("jvmdg/fake/J_U_Optional", tin.desc);
    }

    @Test
    public void anewarrayWithArrayOperandRemapsElement() {
        // operand "[Ljava/util/Optional;" builds Optional[][] — the ARRAY path
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.ANEWARRAY, "[Ljava/util/Optional;");
        assertNull(t.remapInsn(tin, new HashSet<String>()));
        assertEquals("[Ljvmdg/fake/J_U_Optional;", tin.desc);
    }

    @Test
    public void instanceofRewritesToAdapterHook() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithAdapter());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.INSTANCEOF, "java/util/Optional");
        AbstractInsnNode replacement = t.remapInsn(tin, new HashSet<String>());
        assertTrue(replacement instanceof MethodInsnNode);
        MethodInsnNode min = (MethodInsnNode) replacement;
        assertEquals(Opcodes.INVOKESTATIC, min.getOpcode());
        assertEquals(Type.getInternalName(FakeAdapter.class), min.owner);
        assertEquals("jvmdg$instanceof", min.name);
        assertFalse(min.itf);
    }

    @Test
    public void checkcastRewritesToAdapterHook() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithAdapter());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.CHECKCAST, "java/util/Optional");
        AbstractInsnNode replacement = t.remapInsn(tin, new HashSet<String>());
        MethodInsnNode min = (MethodInsnNode) replacement;
        assertEquals("jvmdg$checkcast", min.name);
        assertEquals(Type.getInternalName(FakeAdapter.class), min.owner);
    }

    @Test
    public void checkcastWithoutHookFallsBackToOwnerRewrite() {
        // adapter class lacking jvmdg$checkcast: insn keeps CHECKCAST on the fake type
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        TypeInsnNode tin = new TypeInsnNode(Opcodes.CHECKCAST, "java/util/Optional");
        assertNull(t.remapInsn(tin, new HashSet<String>()));
        assertEquals("jvmdg/fake/J_U_Optional", tin.desc);
    }

    @Test
    public void unstubbedTypeWarnsAndPassesThrough() {
        FakeRemap r = remapWithoutAdapter();
        StructuralRemapTransform t = new StructuralRemapTransform(r);
        TypeInsnNode tin = new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/String");
        assertNull(t.remapInsn(tin, new HashSet<String>()));
        assertEquals("java/lang/String", tin.desc);
        assertTrue(r.warned.contains("java/lang/String"));
    }

    @Test
    public void unstubbedArrayElementWarns() {
        FakeRemap r = remapWithoutAdapter();
        StructuralRemapTransform t = new StructuralRemapTransform(r);
        TypeInsnNode tin = new TypeInsnNode(Opcodes.ANEWARRAY, "[Ljava/lang/String;");
        t.remapInsn(tin, new HashSet<String>());
        assertTrue(r.warned.contains("java/lang/String"));
    }

    @Test
    public void nonTypeInsnReturnsNull() {
        StructuralRemapTransform t = new StructuralRemapTransform(remapWithoutAdapter());
        assertNull(t.remapInsn(new InsnNode(Opcodes.NOP), new HashSet<String>()));
        assertNull(t.remapInsn(new VarInsnNode(Opcodes.ALOAD, 0), new HashSet<String>()));
    }

}
