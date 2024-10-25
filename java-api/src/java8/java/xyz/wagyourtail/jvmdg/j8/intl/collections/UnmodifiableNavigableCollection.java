package xyz.wagyourtail.jvmdg.j8.intl.collections;

import java.util.*;

public class UnmodifiableNavigableCollection<E> extends BackingCollection<E> implements NavigableSet<E> {
    public static final UnmodifiableNavigableCollection EMPTY = new UnmodifiableNavigableCollection(new TreeSet());
    private final NavigableSet<E> backing;

    public UnmodifiableNavigableCollection(NavigableSet<E> backing) {
        super(Collections.unmodifiableSet(backing));
        this.backing = backing;
    }

    @Override
    public E lower(E e) {
        return backing.lower(e);
    }

    @Override
    public E floor(E e) {
        return backing.floor(e);
    }

    @Override
    public E ceiling(E e) {
        return backing.ceiling(e);
    }

    @Override
    public E higher(E e) {
        return backing.higher(e);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new UnmodifiableNavigableCollection<>(backing.descendingSet());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return new UnmodifiableNavigableCollection<>(backing.subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new UnmodifiableNavigableCollection<>(backing.headSet(toElement, inclusive));
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new UnmodifiableNavigableCollection<>(backing.tailSet(fromElement, inclusive));
    }

    @Override
    public Comparator<? super E> comparator() {
        return backing.comparator();
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return Collections.unmodifiableSortedSet(backing.subSet(fromElement, toElement));
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return Collections.unmodifiableSortedSet(backing.headSet(toElement));
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return Collections.unmodifiableSortedSet(backing.tailSet(fromElement));
    }

    @Override
    public E first() {
        return backing.first();
    }

    @Override
    public E last() {
        return backing.last();
    }

}
