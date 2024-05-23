package xyz.wagyourtail.jvmdg.j8.intl.collections;

import java.util.Collections;
import java.util.Queue;

public class CheckedQueue<E> extends BackingCollection<E> implements Queue<E> {
    private final Queue<E> backing;
    private final Class<E> type;

    public CheckedQueue(Queue<E> backing, Class<E> type) {
        super(Collections.checkedCollection(backing, type));
        this.backing = backing;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    E typeCheck(Object o) {
        if (o != null && !type.isInstance(o))
            throw new ClassCastException(badElementMsg(o));
        return (E) o;
    }

    private String badElementMsg(Object o) {
        return "Attempt to insert " + o.getClass() +
                " element into collection with element type " + type;
    }


    @Override
    public boolean offer(E e) {
        return backing.offer(typeCheck(e));
    }

    @Override
    public E remove() {
        return backing.remove();
    }

    @Override
    public E poll() {
        return backing.poll();
    }

    @Override
    public E element() {
        return backing.element();
    }

    @Override
    public E peek() {
        return backing.peek();
    }
}
