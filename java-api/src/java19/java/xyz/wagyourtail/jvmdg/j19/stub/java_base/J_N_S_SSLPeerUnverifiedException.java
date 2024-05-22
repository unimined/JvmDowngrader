package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_N_S_SSLPeerUnverifiedException {

    @Modify(ref = @Ref(value = "javax/net/ssl/SSLPeerUnverifiedException", member = "<init>", desc = "(Ljava/lang/String;java/lang/Throwable;)V"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: SSLPeerUnverifiedException, String, Throwable
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Throwable, SSLPeerUnverifiedException, String, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, SSLPeerUnverifiedException, String
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: Throwable, SSLPeerUnverifiedException, String, SSLPeerUnverifiedException, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "javax/net/ssl/SSLPeerUnverifiedException", "<init>", "(Ljava/lang/String;)V", false));
        // stack: Throwable, SSLPeerUnverifiedException, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, SSLPeerUnverifiedException
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: SSLPeerUnverifiedException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "javax/net/ssl/SSLPeerUnverifiedException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: SSLPeerUnverifiedException
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
