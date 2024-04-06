package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.jvmdg.j21.stub.java_base.J_U_SequencedCollection;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SequencedListAdapter<E> implements J_U_SequencedCollection<E> {
    final List<E> collection;

    public SequencedListAdapter(List<E> collection) {
        this.collection = collection;
    }

    @Override
    public J_U_SequencedCollection<E> reversed() {
        return new SequencedListAdapter<>(new AbstractList<>() {
            @Override
            public E get(int index) {
                return collection.get(collection.size() - 1 - index);
            }

            @Override
            public boolean add(E e) {
                collection.add(0, e);
                return true;
            }

            @Override
            public void add(int index, E element) {
                super.add(collection.size() - 1 - index, element);
            }

            @Override
            public int size() {
                return collection.size();
            }
        });
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
        collection.add(0, e);
    }

    @Override
    public void addLast(E e) {
        collection.add(e);
    }

    @Override
    public E getFirst() {
        return collection.get(0);
    }

    @Override
    public E getLast() {
        return collection.get(collection.size() - 1);
    }

    @Override
    public E removeFirst() {
        return collection.remove(0);
    }

    @Override
    public E removeLast() {
        return collection.remove(collection.size() - 1);
    }
}
