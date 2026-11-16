package xyz.wagyourtail.jvmdg.version.map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the {@code <init>} NEW/DUP pairing verifier in
 * {@link ClassMapping}. The old implementation scanned backwards guessing at
 * the matching NEW: it crashed with IllegalStateException on this()/super()
 * delegation and could remove an unrelated NEW in non-javac shapes.
 */
public class TestClassMappingInit implements Opcodes {

    private static MethodNode method(InsnList insns) {
        MethodNode m = new MethodNode(ACC_PUBLIC, "m", "()V", null, null);
        m.instructions = insns;
        return m;
    }

    private static InsnList list(AbstractInsnNode... insns) {
        InsnList l = new InsnList();
        for (AbstractInsnNode i : insns) {
            l.add(i);
        }
        return l;
    }

    private static int indexOf(InsnList insns, AbstractInsnNode node) {
        return insns.indexOf(node);
    }

    @Test
    public void simpleNewDupPairs() {
        TypeInsnNode n = new TypeInsnNode(NEW, "foo/Bar");
        MethodInsnNode init = new MethodInsnNode(INVOKESPECIAL, "foo/Bar", "<init>", "()V", false);
        MethodNode m = method(list(
            n,
            new InsnNode(DUP),
            init,
            new InsnNode(POP),
            new InsnNode(RETURN)
        ));
        assertEquals(indexOf(m.instructions, n),
            ClassMapping.findPairedNew(m, indexOf(m.instructions, init), "foo/Bar"));
    }

    @Test
    public void nestedSameOwnerCtorsPairCorrectly() {
        // new Foo(new Foo()): outer <init> must pair with the OUTER new, not
        // the nearer inner one (the inner <init> consumes the inner ref).
        TypeInsnNode outerNew = new TypeInsnNode(NEW, "foo/Bar");
        TypeInsnNode innerNew = new TypeInsnNode(NEW, "foo/Bar");
        MethodInsnNode innerInit = new MethodInsnNode(INVOKESPECIAL, "foo/Bar", "<init>", "()V", false);
        MethodInsnNode outerInit = new MethodInsnNode(INVOKESPECIAL, "foo/Bar", "<init>", "(Lfoo/Bar;)V", false);
        MethodNode m = method(list(
            outerNew,
            new InsnNode(DUP),
            innerNew,
            new InsnNode(DUP),
            innerInit,
            outerInit,
            new InsnNode(POP),
            new InsnNode(RETURN)
        ));
        assertEquals(indexOf(m.instructions, innerNew),
            ClassMapping.findPairedNew(m, indexOf(m.instructions, innerInit), "foo/Bar"));
        assertEquals(indexOf(m.instructions, outerNew),
            ClassMapping.findPairedNew(m, indexOf(m.instructions, outerInit), "foo/Bar"));
    }

    @Test
    public void thisDelegationDoesNotPair() {
        // this(5): invokespecial <init> on uninitialized this, no NEW anywhere
        MethodInsnNode init = new MethodInsnNode(INVOKESPECIAL, "foo/Bar", "<init>", "(I)V", false);
        MethodNode m = method(list(
            new VarInsnNode(ALOAD, 0),
            new InsnNode(ICONST_5),
            init,
            new InsnNode(RETURN)
        ));
        assertEquals(-1, ClassMapping.findPairedNew(m, indexOf(m.instructions, init), "foo/Bar"),
            "constructor delegation must not pair — previously threw IllegalStateException");
    }

    @Test
    public void newWithoutDupDoesNotPair() {
        TypeInsnNode n = new TypeInsnNode(NEW, "foo/Bar");
        MethodInsnNode init = new MethodInsnNode(INVOKESPECIAL, "foo/Bar", "<init>", "()V", false);
        MethodNode m = method(list(
            n,
            init, // no DUP — the rewrite contract (remove NEW+DUP) can't be honored
            new InsnNode(RETURN)
        ));
        assertEquals(-1, ClassMapping.findPairedNew(m, indexOf(m.instructions, init), "foo/Bar"),
            "NEW not followed by DUP must not be removed — previously threw 'expected DUP'");
    }

    @Test
    public void staleSameOwnerNewIsNotStolen() {
        // two sequential `new Foo()`; second call must pair with second NEW
        TypeInsnNode firstNew = new TypeInsnNode(NEW, "foo/Bar");
        TypeInsnNode secondNew = new TypeInsnNode(NEW, "foo/Bar");
        MethodInsnNode firstInit = new MethodInsnNode(INVOKESPECIAL, "foo/Bar", "<init>", "()V", false);
        MethodInsnNode secondInit = new MethodInsnNode(INVOKESPECIAL, "foo/Bar", "<init>", "()V", false);
        MethodNode m = method(list(
            firstNew,
            new InsnNode(DUP),
            firstInit,
            new VarInsnNode(ASTORE, 1),
            secondNew,
            new InsnNode(DUP),
            secondInit,
            new VarInsnNode(ASTORE, 2),
            new InsnNode(RETURN)
        ));
        assertEquals(indexOf(m.instructions, firstNew),
            ClassMapping.findPairedNew(m, indexOf(m.instructions, firstInit), "foo/Bar"));
        assertEquals(indexOf(m.instructions, secondNew),
            ClassMapping.findPairedNew(m, indexOf(m.instructions, secondInit), "foo/Bar"));
    }

    @Test
    public void consumedUninitRefDoesNotPair() {
        // NEW DUP POP <init>: the dup'd ref was destroyed before the call
        MethodInsnNode init = new MethodInsnNode(INVOKESPECIAL, "foo/Bar", "<init>", "()V", false);
        MethodNode m = method(list(
            new TypeInsnNode(NEW, "foo/Bar"),
            new InsnNode(DUP),
            new InsnNode(POP),
            init,
            new InsnNode(RETURN)
        ));
        assertEquals(-1, ClassMapping.findPairedNew(m, indexOf(m.instructions, init), "foo/Bar"));
    }

    @Test
    public void uninitRefUsedAsArgDoesNotPair() {
        // new Foo(<itself>): marker in arg position — unprovable
        MethodInsnNode init = new MethodInsnNode(INVOKESPECIAL, "foo/Bar", "<init>", "(Lfoo/Bar;)V", false);
        MethodNode m = method(list(
            new TypeInsnNode(NEW, "foo/Bar"),
            new InsnNode(DUP),
            new InsnNode(DUP),
            init,
            new InsnNode(RETURN)
        ));
        assertEquals(-1, ClassMapping.findPairedNew(m, indexOf(m.instructions, init), "foo/Bar"));
    }

    @Test
    public void ternaryArgRegionPairs() {
        // new Foo(cond ? a : b): branch inside the ctor-arg region must verify
        // via stack-merge, not bail on the first branch seen.
        LabelNode elseLabel = new LabelNode();
        LabelNode endLabel = new LabelNode();
        TypeInsnNode n = new TypeInsnNode(NEW, "foo/Bar");
        MethodInsnNode init = new MethodInsnNode(INVOKESPECIAL, "foo/Bar", "<init>", "(Ljava/lang/String;)V", false);
        MethodNode m = method(list(
            n,
            new InsnNode(DUP),
            new VarInsnNode(ILOAD, 1),
            new JumpInsnNode(IFEQ, elseLabel),
            new LdcInsnNode("a"),
            new JumpInsnNode(GOTO, endLabel),
            elseLabel,
            new LdcInsnNode("b"),
            endLabel,
            init,
            new InsnNode(POP),
            new InsnNode(RETURN)
        ));
        assertEquals(indexOf(m.instructions, n),
            ClassMapping.findPairedNew(m, indexOf(m.instructions, init), "foo/Bar"));
    }

    @Test
    public void differentOwnerNewIsIgnored() {
        // candidate scan must skip NEW instructions of other types
        TypeInsnNode barNew = new TypeInsnNode(NEW, "foo/Bar");
        MethodInsnNode init = new MethodInsnNode(INVOKESPECIAL, "foo/Bar", "<init>", "()V", false);
        MethodNode m = method(list(
            new TypeInsnNode(NEW, "foo/Baz"), // unrelated allocation
            new InsnNode(DUP),
            new MethodInsnNode(INVOKESPECIAL, "foo/Baz", "<init>", "()V", false),
            new InsnNode(POP),
            barNew,
            new InsnNode(DUP),
            init,
            new InsnNode(RETURN)
        ));
        assertEquals(indexOf(m.instructions, barNew),
            ClassMapping.findPairedNew(m, indexOf(m.instructions, init), "foo/Bar"));
    }
}
