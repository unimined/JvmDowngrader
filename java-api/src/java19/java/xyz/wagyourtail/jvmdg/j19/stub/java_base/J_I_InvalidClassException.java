package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_I_InvalidClassException {

    @Modify(ref = @Ref(value = "Ljava/io/InvalidClassException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V;"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: InvalidClassException, String (cname), String (reason), Throwable
        list.add(new InsnNode(Opcodes.DUP2_X2));
        // stack: String (reason), Throwable, InvalidClassException, String (cname), String (reason), Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: String (reason), Throwable, InvalidClassException, String (cname), String (reason)
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: String (reason), Throwable, InvalidClassException, String (reason), String (cname)
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: String (reason), Throwable, String (cname), InvalidClassException, String (reason), String (cname)
        list.add(new InsnNode(Opcodes.POP));
        // stack: String (reason), Throwable, String (cname), InvalidClassException, String (reason)
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: String (reason), Throwable, String (cname), InvalidClassException, String (reason), InvalidClassException, String (reason)
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/InvalidClassException", "<init>", "(Ljava/lang/String;)V", false));
        // stack: String (reason), Throwable, String (cname), InvalidClassException, String (reason)
        list.add(new InsnNode(Opcodes.POP));
        // stack: String (reason), Throwable, String (cname), InvalidClassException
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: String (reason), Throwable, InvalidClassException, String (cname)
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: String (reason), Throwable, InvalidClassException, String (cname), InvalidClassException, String (cname)
        // set field classname
        list.add(new FieldInsnNode(Opcodes.PUTFIELD, "java/io/InvalidClassException", "classname", "Ljava/lang/String;"));
        // stack: String (reason), Throwable, InvalidClassException, String (cname)
        list.add(new InsnNode(Opcodes.POP));
        // stack: String (reason), Throwable, InvalidClassException
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: String (reason), InvalidClassException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/InvalidClassException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: String (reason), InvalidClassException
        list.add(new InsnNode(Opcodes.POP2));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "Ljava/io/InvalidClassException;", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V;"))
    public static void init2(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: InvalidClassException, String (reason), Throwable
        list.add(new InsnNode(Opcodes.DUP_X2));
        // stack: Throwable, InvalidClassException, String (reason), Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, InvalidClassException, String (reason)
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: Throwable, InvalidClassException, String (reason), InvalidClassException, String (reason)
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/InvalidClassException", "<init>", "(Ljava/lang/String;)V", false));
        // stack: Throwable, InvalidClassException, String (reason)
        list.add(new InsnNode(Opcodes.POP));
        // stack: Throwable, InvalidClassException
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: InvalidClassException, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/InvalidClassException", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: InvalidClassException
        list.add(new InsnNode(Opcodes.POP));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
