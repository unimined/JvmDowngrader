package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_L_InternalError {

//    @Stub(ref = @Ref(value = "java/lang/InternalError", member = "<init>"))
//    public static InternalError init(String message, Throwable cause) {
//        InternalError ie = new InternalError(message);
//        ie.initCause(cause);
//        return ie;
//    }

    @Modify(ref = @Ref(value = "java/lang/InternalError", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: InternalError, String, Throwable
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Throwable, InternalError, String, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, InternalError, String
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: Throwable, InternalError, String, InternalError, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/InternalError", "<init>", "(Ljava/lang/String;)V", false));
        // stack: Throwable, InternalError, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, InternalError
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: InternalError, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/InternalError", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: InternalError
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

//    @Stub(ref = @Ref(value = "java/lang/InternalError", member = "<init>"))
//    public static InternalError init(Throwable cause) {
//        InternalError ie = new InternalError();
//        ie.initCause(cause);
//        return ie;
//    }

    @Modify(ref = @Ref(value = "java/lang/InternalError", member = "<init>", desc = "(Ljava/lang/Throwable;)V"))
    public static void init2(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: InternalError, Throwable, InternalError, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: InternalError, Throwable, InternalError
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/InternalError", "<init>", "()V", false));
        // stack: InternalError, Throwable
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/InternalError", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: InternalError
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}