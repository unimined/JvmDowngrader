package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.ReverseDeque;
import xyz.wagyourtail.jvmdg.j21.impl.ReverseList;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Deque;
import java.util.List;

public class J_U_Deque {

    @Stub
    public static <E> Deque<E> reversed(Deque<E> self) {
        if (self instanceof ReverseDeque<E> rl) {
            return rl.original;
        }
        return new ReverseDeque<>(self);
    }

}
