package xyz.wagyourtail.jvmdg.j8.intl.collections;

import java.util.Collection;
import java.util.Iterator;

public class SynchronizedBackingCollection<E> implements Collection<E> {
    private final Collection<E> backing;
    protected final Object mutex;

    public SynchronizedBackingCollection(Collection<E> backing) {
        this.backing = backing;
        this.mutex = this;
    }

    public SynchronizedBackingCollection(Collection<E> backing, Object mutex) {
        this.backing = backing;
        this.mutex = mutex;
    }


    @Override
    public int size() {
        synchronized (mutex) {
            return backing.size();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (mutex) {
            return backing.isEmpty();
        }
    }

    @Override
    public boolean contains(Object o) {
        synchronized (mutex) {
            return backing.contains(o);
        }
    }

    @Override
    public Iterator<E> iterator() {
        return backing.iterator();
    }

    @Override
    public Object[] toArray() {
        synchronized (mutex) {
            return backing.toArray();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        synchronized (mutex) {
            return backing.toArray(a);
        }
    }

    @Override
    public boolean add(E e) {
        synchronized (mutex) {
            return backing.add(e);
        }
    }

    @Override
    public boolean remove(Object o) {
        synchronized (mutex) {
            return backing.remove(o);
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        synchronized (mutex) {
            return backing.containsAll(c);
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        synchronized (mutex) {
            return backing.addAll(c);
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        synchronized (mutex) {
            return backing.removeAll(c);
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        synchronized (mutex) {
            return backing.retainAll(c);
        }
    }

    @Override
    public void clear() {
        synchronized (mutex) {
            backing.clear();
        }
    }
}
