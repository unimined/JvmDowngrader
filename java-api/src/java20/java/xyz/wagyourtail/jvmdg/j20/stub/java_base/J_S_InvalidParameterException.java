package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_S_InvalidParameterException {

    @Modify(javaVersion = Opcodes.V19, ref = @Ref(value = "Ljava/security/InvalidParameterException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V;"))
    public static void init(MethodNode mnode, int i) {
        AbstractInsnNode node = mnode.instructions.get(i);
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

    @Modify(javaVersion = Opcodes.V19, ref = @Ref(value = "Ljava/security/InvalidParameterException;", member = "<init>", desc = "(Ljava/lang/Throwable;)V;"))
    public static void init1(MethodNode mnode, int i) {
        AbstractInsnNode node = mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: InvalidParameterException, Throwable
        list.add(new InsnNode(Opcodes.DUP_X1));
        // stack: Throwable, InvalidParameterException, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, InvalidParameterException
        list.add(new InsnNode(Opcodes.DUP));
        // stack: Throwable, InvalidParameterException, InvalidParameterException
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/security/InvalidParameterException", "<init>", "()V", false));
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

}
