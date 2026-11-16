package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_S_InvalidParameterException {

    @Modify(ref = @Ref(value = "Ljava/security/InvalidParameterException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V;"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: InvalidParameterException, String, Throwable
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Throwable, InvalidParameterException, String, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, InvalidParameterException, String
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: Throwable, InvalidParameterException, String, InvalidParameterException, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/security/InvalidParameterException", "<init>", "(Ljava/lang/String;)V", false));
        // stack: Throwable, InvalidParameterException, String
        // remove the string
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, InvalidParameterException
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: InvalidParameterException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/security/InvalidParameterException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: InvalidParameterException
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "Ljava/security/InvalidParameterException;", member = "<init>", desc = "(Ljava/lang/Throwable;)V;"))
    public static void init1(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: InvalidParameterException, Throwable
        // Throwable(Throwable) sets detailMessage = cause == null ? null : cause.toString()
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: InvalidParameterException, Throwable, InvalidParameterException, Throwable
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "xyz/wagyourtail/jvmdg/j8/intl/ExceptionHelper", "causeMessage", "(Ljava/lang/Throwable;)Ljava/lang/String;", false));
        // stack: InvalidParameterException, Throwable, InvalidParameterException, String
        // call init(String)
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/security/InvalidParameterException", "<init>", "(Ljava/lang/String;)V", false));
        // stack: InvalidParameterException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/security/InvalidParameterException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: InvalidParameterException, InvalidParameterException (initCause returns this)
        list.add(new InsnNode(Opcodes.POP));
        // stack: InvalidParameterException

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
