package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_N_SocketException {

    @Modify(ref = @Ref(value = "Ljava/net/SocketException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V;"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: SocketException, String, Throwable
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Throwable, SocketException, String, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, SocketException, String
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: Throwable, SocketException, String, SocketException, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/net/SocketException", "<init>", "(Ljava/lang/String;)V", false));
        // stack: Throwable, SocketException, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, SocketException
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: SocketException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/net/SocketException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: SocketException
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "Ljava/net/SocketException;", member = "<init>", desc = "(Ljava/lang/Throwable;)V;"))
    public static void init2(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: SocketException, Throwable
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: SocketException, Throwable, SocketException, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: SocketException, Throwable, SocketException
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/net/SocketException", "<init>", "()V", false));
        // stack: SocketException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/net/SocketException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: SocketException
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
