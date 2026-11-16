package xyz.wagyourtail.jvmdg.j15.stub.java_base;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
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
        // Throwable(Throwable) sets detailMessage = cause == null ? null : cause.toString()
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: NoSuchElementException, Throwable, NoSuchElementException, Throwable
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "xyz/wagyourtail/jvmdg/j8/intl/ExceptionHelper", "causeMessage", "(Ljava/lang/Throwable;)Ljava/lang/String;", false));
        // stack: NoSuchElementException, Throwable, NoSuchElementException, String
        // call init(String)
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/NoSuchElementException", "<init>", "(Ljava/lang/String;)V", false));
        // stack: NoSuchElementException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/NoSuchElementException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: NoSuchElementException, NoSuchElementException (initCause returns this)
        list.add(new InsnNode(Opcodes.POP));
        // stack: NoSuchElementException

        mNode.instructions.insert(node, list);
        mNode.instructions.remove(node);
    }

}
