package xyz.wagyourtail.jvmdg.j15.stub.java_base;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_U_NoSuchElementException {

    @Modify(ref = @Ref(value = "Ljava/util/NoSuchElementException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: NoSuchElementException, String, Throwable
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Throwable, NoSuchElementException, String, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, NoSuchElementException, String
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: Throwable, NoSuchElementException, String, NoSuchElementException, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/NoSuchElementException", "<init>", "(Ljava/lang/String;)V", false));
        // stack: Throwable, NoSuchElementException, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, NoSuchElementException
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: NoSuchElementException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/NoSuchElementException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: NoSuchElementException
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "java/util/NoSuchElementException", member = "<init>", desc = "(Ljava/lang/Throwable;)V"))
    public static void init2(MethodNode mNode, int i) {
        MethodInsnNode node = (MethodInsnNode) mNode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: NoSuchElementException, Throwable
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: NoSuchElementException, Throwable, NoSuchElementException, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: NoSuchElementException, Throwable, NoSuchElementException
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/NoSuchElementException", "<init>", "()V", false));
        // stack: NoSuchElementException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/NoSuchElementException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: NoSuchElementException
        list.add(new InsnNode(Opcodes.POP));

        mNode.instructions.insert(node, list);
        mNode.instructions.remove(node);
    }

}
