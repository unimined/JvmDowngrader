package xyz.wagyourtail.jvmdg.j8.intl.collections;

import java.util.Set;

public class SynchronizedBackingSet<E> extends SynchronizedBackingCollection<E> implements Set<E> {

    private final Set<E> backing;

    public SynchronizedBackingSet(Set<E> backing) {
        super(backing);
        this.backing = backing;
    }

    public SynchronizedBackingSet(Set<E> backing, Object mutex) {
        super(backing, mutex);
        this.backing = backing;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        synchronized (mutex) {
            return backing.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        synchronized (mutex) {
            return backing.hashCode();
        }
    }
}
