package xyz.wagyourtail.jvmdg.j8.intl.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public class BackingCollection<E> implements Collection<E> {
    private final Collection<E> backing;

    public BackingCollection(Collection<E> backing) {
        this.backing = Objects.requireNonNull(backing);
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return backing.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return backing.iterator();
    }

    @Override
    public Object[] toArray() {
        return backing.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return backing.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return backing.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return backing.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return backing.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return backing.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return backing.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return backing.removeAll(c);
    }

    @Override
    public void clear() {
        backing.clear();
    }
}
