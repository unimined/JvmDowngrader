package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_I_InvalidObjectException {

    @Modify(ref = @Ref(value = "Ljava/io/InvalidObjectException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V;"))
    public static void init(MethodNode mnode, int i) {
        AbstractInsnNode node = mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: InvalidObjectException, String, Throwable
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Throwable, InvalidObjectException, String, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, InvalidObjectException, String
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: Throwable, InvalidObjectException, String, InvalidObjectException, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/InvalidObjectException", "<init>", "(Ljava/lang/String;)V", false));
        // stack: Throwable, InvalidObjectException, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, InvalidObjectException
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: InvalidObjectException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/InvalidObjectException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: InvalidObjectException
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
