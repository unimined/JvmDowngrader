package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.SequencedDequeAdapter;
import xyz.wagyourtail.jvmdg.j21.impl.SequencedListAdapter;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.*;

@Adapter(value = "java/util/SequencedCollection")
public interface J_U_SequencedCollection<E> extends Collection<E> {

    static boolean jvmdg$instanceof(Object obj) {
        return obj instanceof J_U_SequencedCollection<?> ||
            obj instanceof List<?> ||
            obj instanceof LinkedHashSet<?> ||
            obj instanceof Deque<?> ||
            obj instanceof SortedSet<?>;
    }

    static <E> J_U_SequencedCollection<E> jvmdg$checkcast(Object obj) {
        if (!jvmdg$instanceof(obj)) {
            throw new ClassCastException();
        }
        if (obj instanceof J_U_SequencedCollection<?>) {
            return (J_U_SequencedCollection<E>) obj;
        }
        if (obj instanceof List<?>) {
            return new SequencedListAdapter<>((List<E>) obj);
        }
        if (obj instanceof Set<?>) {
            return J_U_SequencedSet.jvmdg$checkcast(obj);
        }
        if (obj instanceof Deque<?>) {
            return new SequencedDequeAdapter<>((Deque<E>) obj);
        }
        throw new AssertionError();
    }

    J_U_SequencedCollection<E> reversed();

    default void addFirst(E e) {
        throw new UnsupportedOperationException();
    }

    default void addLast(E e) {
        throw new UnsupportedOperationException();
    }

    default E getFirst() {
        return this.iterator().next();
    }

    default E getLast() {
        return this.reversed().iterator().next();
    }

    default E removeFirst() {
        Iterator<E> it = this.iterator();
        E e = it.next();
        it.remove();
        return e;
    }

    default E removeLast() {
        Iterator<E> it = this.reversed().iterator();
        E e = it.next();
        it.remove();
        return e;
    }


}
