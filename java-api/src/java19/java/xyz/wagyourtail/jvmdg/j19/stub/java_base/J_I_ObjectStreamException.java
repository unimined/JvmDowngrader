package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_I_ObjectStreamException {

    @Modify(ref = @Ref(value = "Ljava/io/ObjectStreamException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V;"))
    public static void init(MethodNode mnode, int i) {
        AbstractInsnNode node = mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: ObjectStreamException, String, Throwable
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Throwable, ObjectStreamException, String, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, ObjectStreamException, String
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: Throwable, ObjectStreamException, String, ObjectStreamException, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/ObjectStreamException", "<init>", "(Ljava/lang/String;)V", false));
        // stack: Throwable, ObjectStreamException, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, ObjectStreamException
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: ObjectStreamException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/ObjectStreamException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: ObjectStreamException
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "Ljava/io/ObjectStreamException;", member = "<init>", desc = "(Ljava/lang/Throwable;)V;"))
    public static void init2(MethodNode mnode, int i) {
        AbstractInsnNode node = mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: ObjectStreamException, Throwable
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: ObjectStreamException, Throwable, ObjectStreamException, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: ObjectStreamException, Throwable, ObjectStreamException
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/ObjectStreamException", "<init>", "()V", false));
        // stack: ObjectStreamException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/ObjectStreamException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: ObjectStreamException
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
