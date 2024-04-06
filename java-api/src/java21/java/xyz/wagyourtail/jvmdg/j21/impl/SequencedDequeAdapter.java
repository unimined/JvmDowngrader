package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.jvmdg.j21.stub.java_base.J_U_SequencedCollection;

import java.util.*;

public class SequencedDequeAdapter<E> implements J_U_SequencedCollection<E> {
    final Deque<E> collection;

    public SequencedDequeAdapter(Deque<E> collection) {
        this.collection = collection;
    }

    @Override
    public J_U_SequencedCollection<E> reversed() {
        if (collection instanceof ReverseDeque<E> c) {
            return new SequencedDequeAdapter<>(c.original);
        }
        return new SequencedDequeAdapter<>(new ReverseDeque<>(collection));
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return collection.contains(o);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return collection.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return collection.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return collection.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return collection.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return collection.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return collection.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        return collection.addAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return collection.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return collection.retainAll(c);
    }

    @Override
    public void clear() {
        collection.clear();
    }

    @Override
    public void addFirst(E e) {
        collection.addFirst(e);
    }

    @Override
    public void addLast(E e) {
        collection.addLast(e);
    }

    @Override
    public E getFirst() {
        return collection.getFirst();
    }

    @Override
    public E getLast() {
        return collection.getLast();
    }

    @Override
    public E removeFirst() {
        return collection.removeFirst();
    }

    @Override
    public E removeLast() {
        return collection.removeLast();
    }
}
