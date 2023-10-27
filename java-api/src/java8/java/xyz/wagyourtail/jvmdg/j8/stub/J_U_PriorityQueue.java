package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Comparator;
import java.util.PriorityQueue;

public class J_U_PriorityQueue {


    @Stub(ref = @Ref(value = "java/util/PriorityQueue", member = "<init>"))
    public static <E> PriorityQueue<E> init(Comparator<? super E> comparator) {
        return new PriorityQueue<>(11, comparator);
    }

    @Stub
    public static <E> J_U_Spliterator<E> spliterator(PriorityQueue<E> queue) {
        return J_U_Spliterators.spliterator(queue, 0);
    }
}
