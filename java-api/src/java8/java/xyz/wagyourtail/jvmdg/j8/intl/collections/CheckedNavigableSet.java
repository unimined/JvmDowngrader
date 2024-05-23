package xyz.wagyourtail.jvmdg.j8.intl.collections;

import java.util.*;

public class CheckedNavigableSet<E> extends BackingCollection<E> implements NavigableSet<E> {
    private final NavigableSet<E> backing;
    private final Class<E> type;

    public CheckedNavigableSet(NavigableSet<E> backing, Class<E> type) {
        super(backing);
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
        return backing.pollFirst();
    }

    @Override
    public E pollLast() {
        return backing.pollLast();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new CheckedNavigableSet<>(backing.descendingSet(), type);
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return new CheckedNavigableSet<>(backing.subSet(fromElement, fromInclusive, toElement, toInclusive), type);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new CheckedNavigableSet<>(backing.headSet(toElement, inclusive), type);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new CheckedNavigableSet<>(backing.tailSet(fromElement, inclusive), type);
    }

    @Override
    public Comparator<? super E> comparator() {
        return backing.comparator();
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return Collections.checkedSortedSet(backing.subSet(fromElement, toElement), type);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return Collections.checkedSortedSet(backing.headSet(toElement), type);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return Collections.checkedSortedSet(backing.tailSet(fromElement), type);
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
