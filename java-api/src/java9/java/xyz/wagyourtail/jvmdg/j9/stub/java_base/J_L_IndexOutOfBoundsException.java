package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_L_IndexOutOfBoundsException {

    @Modify(ref = @Ref(value = "java/lang/IndexOutOfBoundsException", member = "<init>", desc = "(I)V"))
    public static void init(MethodNode mnode, int i) {
        MethodInsnNode node = (MethodInsnNode) mnode.instructions.get(i);
        InsnList list = new InsnList();

        // stack: IndexOutOfBoundsException, int
        // new StringBuilder("Index out of range: ").append(index)
        list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new LdcInsnNode("Index out of range: "));
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false));
        // stack: IndexOutOfBoundsException, int, StringBuilder
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: IndexOutOfBoundsException, StringBuilder, int
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false));
        // stack: IndexOutOfBoundsException, StringBuilder
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
        // stack: IndexOutOfBoundsException, String
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/IndexOutOfBoundsException", "<init>", "(Ljava/lang/String;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

}
