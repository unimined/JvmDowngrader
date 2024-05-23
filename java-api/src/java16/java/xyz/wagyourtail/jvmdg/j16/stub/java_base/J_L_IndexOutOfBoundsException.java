package xyz.wagyourtail.jvmdg.j16.stub.java_base;


import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;

public class J_L_IndexOutOfBoundsException {

    @Modify(ref = @Ref(value = "java/lang/IndexOutOfBoundsException", member = "<init>", desc = "(J)V"))
    public static void init(MethodNode mNode, int i) {
        MethodInsnNode node = (MethodInsnNode) mNode.instructions.get(i);
        InsnList list = new InsnList();
        // string concat factory, "Index out of range: \u0001"
        list.add(new InvokeDynamicInsnNode(
                "makeConcatWithConstants",
                "(J)Ljava/lang/String;",
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        "java/lang/invoke/StringConcatFactory",
                        "makeConcatWithConstants",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
                        false
                ),
                "Index out of range: \u0001"
        ));
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/IndexOutOfBoundsException", "<init>", "(Ljava/lang/String;)V", false));

        mNode.instructions.insert(node, list);
        mNode.instructions.remove(node);
    }

}
  