package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_N_S_SSLProtocolException {

    @Modify(ref = @Ref(value = "javax/net/ssl/SSLProtocolException", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: SSLProtocolException, String, Throwable
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Throwable, SSLProtocolException, String, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, SSLProtocolException, String
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: Throwable, SSLProtocolException, String, SSLProtocolException, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "javax/net/ssl/SSLProtocolException", "<init>", "(Ljava/lang/String;)V", false));
        // stack: Throwable, SSLProtocolException, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, SSLProtocolException
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: SSLProtocolException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "javax/net/ssl/SSLProtocolException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: SSLProtocolException
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
