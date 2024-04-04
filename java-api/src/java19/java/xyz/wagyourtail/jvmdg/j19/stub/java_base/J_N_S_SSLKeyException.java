package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_N_S_SSLKeyException {

    @Modify(javaVersion = Opcodes.V19, ref = @Ref(value = "Ljavax/net/ssl/SSLKeyException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V;"))
    public static void init(MethodNode mnode, int i) {
        AbstractInsnNode node = mnode.instructions.get(i);
        InsnList list = new InsnList();

        // stack: SSLKeyException, String (reason), Throwable
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Throwable, SSLKeyException, String (reason), Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, SSLKeyException, String (reason)
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: Throwable, SSLKeyException, String (reason), Throwable, SSLKeyException, String (reason)
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "javax/net/ssl/SSLKeyException", "<init>", "(Ljava/lang/String;)V", false));
        // stack: Throwable, SSLKeyException, String (reason)
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, SSLKeyException
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: SSLKeyException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "javax/net/ssl/SSLKeyException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: SSLKeyException
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
