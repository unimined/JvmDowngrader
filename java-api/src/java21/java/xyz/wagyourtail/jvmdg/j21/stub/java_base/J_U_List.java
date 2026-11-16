package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.ReverseList;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.List;
import java.util.NoSuchElementException;

public class J_U_List {

    @Stub
    public static <E> List<E> reversed(List<E> self) {
        if (self instanceof ReverseList<E> rl) {
            return rl.original;
        }
        return new ReverseList<>(self);
    }

    @Stub
    public static <E> void addFirst(List<E> self, E e) {
        self.add(0, e);
    }

    @Stub
    public static <E> void addLast(List<E> self, E e) {
        self.add(e);
    }

    @Stub
    public static <E> E getFirst(List<E> self) {
        if (self.isEmpty()) {
            throw new NoSuchElementException();
        }
        return self.get(0);
    }

    @Stub
    public static <E> E getLast(List<E> self) {
        if (self.isEmpty()) {
            throw new NoSuchElementException();
        }
        return self.get(self.size() - 1);
    }

    @Stub
    public static <E> E removeFirst(List<E> self) {
        J_U_SequencedCollection.checkEmptyRemovable(self);
        return self.remove(0);
    }

    @Stub
    public static <E> E removeLast(List<E> self) {
        J_U_SequencedCollection.checkEmptyRemovable(self);
        return self.remove(self.size() - 1);
    }

}
