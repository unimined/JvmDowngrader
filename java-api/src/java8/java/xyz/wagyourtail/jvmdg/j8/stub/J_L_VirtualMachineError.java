package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_L_VirtualMachineError {

    @Modify(ref = @Ref(value = "java/lang/VirtualMachineError", member = "<init>", desc = "(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    public static void init(MethodNode mnode, int i) {
        // stack: VirtualMachineError, String, Throwable
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();
        list.add(new InsnNode(Opcodes.DUP2_X1));
        // stack: String, Throwable, VirtualMachineError, String, Throwable
        list.add(new InsnNode(Opcodes.POP));
        // stack: String, Throwable, VirtualMachineError, String
        list.add(new InsnNode(Opcodes.DUP2));
        // stack: String, Throwable, VirtualMachineError, String, VirtualMachineError, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/VirtualMachineError", "<init>", "(Ljava/lang/String;)V", false));
        // stack: String, Throwable, VirtualMachineError, String
        // remove the string
        list.add(new InsnNode(Opcodes.POP));
        // stack: String, Throwable, VirtualMachineError
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: String, VirtualMachineError, Throwable
        // call initCause
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/VirtualMachineError", "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false));
        // stack: String
        list.add(new InsnNode(Opcodes.POP));
        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Modify(ref = @Ref(value = "java/lang/VirtualMachineError", member = "<init>", desc = "(Ljava/lang/Throwable;)V"))
    public static void init2(MethodNode mnode, int i) {
        // stack:  VirtualMachineError, Throwable
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
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
