package xyz.wagyourtail.jvmdg.j8.intl.collections;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;

public class SynchronizedNavigableSet<E> extends SynchronizedBackingSet<E> implements NavigableSet<E> {
    public final NavigableSet<E> backing;

    public SynchronizedNavigableSet(NavigableSet<E> backing) {
        super(backing);
        this.backing = backing;
    }

    public SynchronizedNavigableSet(NavigableSet<E> backing, Object mutex) {
        super(backing, mutex);
        this.backing = backing;
    }


    @Override
    public E lower(E e) {
        synchronized (mutex) {
            return backing.lower(e);
        }
    }

    @Override
    public E floor(E e) {
        synchronized (mutex) {
            return backing.floor(e);
        }
    }

    @Override
    public E ceiling(E e) {
        synchronized (mutex) {
            return backing.ceiling(e);
        }
    }

    @Override
    public E higher(E e) {
        synchronized (mutex) {
            return backing.higher(e);
        }
    }

    @Override
    public E pollFirst() {
        synchronized (mutex) {
            return backing.pollFirst();
        }
    }

    @Override
    public E pollLast() {
        synchronized (mutex) {
            return backing.pollLast();
        }
    }

    @Override
    public NavigableSet<E> descendingSet() {
        synchronized (mutex) {
            return new SynchronizedNavigableSet<>(backing.descendingSet(), mutex);
        }
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        synchronized (mutex) {
            return new SynchronizedNavigableSet<>(backing.subSet(fromElement, fromInclusive, toElement, toInclusive), mutex);
        }
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        synchronized (mutex) {
            return new SynchronizedNavigableSet<>(backing.headSet(toElement, inclusive), mutex);
        }
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        synchronized (mutex) {
            return new SynchronizedNavigableSet<>(backing.tailSet(fromElement, inclusive), mutex);
        }
    }

    @Override
    public Comparator<? super E> comparator() {
        synchronized (mutex) {
            return backing.comparator();
        }
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        synchronized (mutex) {
            return new SynchronizedNavigableSet<>((NavigableSet<E>) backing.subSet(fromElement, toElement), mutex);
        }
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        synchronized (mutex) {
            return new SynchronizedNavigableSet<>((NavigableSet<E>) backing.headSet(toElement), mutex);
        }
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        synchronized (mutex) {
            return new SynchronizedNavigableSet<>((NavigableSet<E>) backing.tailSet(fromElement), mutex);
        }
    }

    @Override
    public E first() {
        synchronized (mutex) {
            return backing.first();
        }
    }

    @Override
    public E last() {
        synchronized (mutex) {
            return backing.last();
        }
    }

}
