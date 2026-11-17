package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.PriorityQueue;

@InitRewrite(ref = @Ref(value = "Ljava/util/PriorityQueue;", member = "<init>", desc = "(Ljava/util/Comparator;)V"), to = "(ILjava/util/Comparator;)V", keep = {-1, 0})
public class J_U_PriorityQueue {

    // PriorityQueue(Comparator) -> PriorityQueue(11, Comparator)
    @InitArg(target = "(Ljava/util/Comparator;)V", slot = 0)
    public static int defaultCapacity() {
        return 11;
    }

    @Stub
    public static <E> J_U_Spliterator<E> spliterator(PriorityQueue<E> queue) {
        return J_U_Spliterators.spliterator(queue, 0);
    }

}
