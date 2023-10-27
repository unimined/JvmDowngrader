package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_L_VirtualMachineError {

    @Modify(javaVersion = Opcodes.V1_8, ref = @Ref(value = "java/lang/VirtualMachineError", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    public static void init(MethodNode mnode, int i) {
        // stack: VirtualMachineError, String, Throwable
        AbstractInsnNode node = mnode.instructions.get(i);
        InsnList list = new InsnList();
        list.add(new InsnNode(Opcodes.DUP2_X1));
        // stack: VirtualMachineError, String, Throwable, VirtualMachineError, String, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: VirtualMachineError, String, Throwable, VirtualMachineError, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/VirtualMachineError", "<init>", "(Ljava/lang/String;)V", false));
        // stack: VirtualMachineError, String, Throwable
        // remove the string
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: VirtualMachineError, Throwable, String
        list.add(new InsnNode(Opcodes.POP));
        // stack: VirtualMachineError, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/VirtualMachineError", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(javaVersion = Opcodes.V1_8, ref = @Ref(value = "java/lang/VirtualMachineError", member = "<init>", desc = "(Ljava/lang/Throwable;)V"))
    public static void init2(MethodNode mnode, int i) {
        // stack:  VirtualMachineError, Throwable
        AbstractInsnNode node = mnode.instructions.get(i);
        InsnList list = new InsnList();
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: VirtualMachineError, Throwable, VirtualMachineError, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: VirtualMachineError, Throwable, VirtualMachineError
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/VirtualMachineError", "<init>", "()V", false));
        // stack: VirtualMachineError, Throwable
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/VirtualMachineError", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
