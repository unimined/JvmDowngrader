package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import xyz.wagyourtail.jvmdg.version.Modify;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.PriorityQueue;

public class J_U_PriorityQueue {

    @Modify(ref = @Ref(value = "java/util/PriorityQueue", member = "<init>", desc = "(Ljava/util/Comparator;)V"))
    public static void init(MethodNode mnode, int i) {
        AbstractInsnNode node = mnode.instructions.get(i);
        InsnList list = new InsnList();
        // stack: PriorityQueue, Comparator
        list.add(new LdcInsnNode(11));
        // stack: PriorityQueue, Comparator, 11
        list.add(new InsnNode(Opcodes.SWAP));
        // stack: PriorityQueue, 11, Comparator
        // call init
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/PriorityQueue", "<init>", "(ILjava/util/Comparator;)V", false));

        mnode.instructions.insert(node, list);
        mnode.instructions.remove(node);
    }

    @Stub
    public static <E> J_U_Spliterator<E> spliterator(PriorityQueue<E> queue) {
        return J_U_Spliterators.spliterator(queue, 0);
    }

}
