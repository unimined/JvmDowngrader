package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.Comparator;
import java.util.PriorityQueue;

public class J_U_PriorityQueue {


    @Stub(opcVers = Opcodes.V1_8, ref = @Ref(value = "java/util/PriorityQueue", member = "<init>"))
    public static <E> PriorityQueue<E> init(Comparator<? super E> comparator) {
        return new PriorityQueue<>(11, comparator);
    }

    @Stub(opcVers = Opcodes.V1_8)
    public static <E> J_U_Spliterator<E> spliterator(PriorityQueue<E> queue) {
        //TODO
    }
}
