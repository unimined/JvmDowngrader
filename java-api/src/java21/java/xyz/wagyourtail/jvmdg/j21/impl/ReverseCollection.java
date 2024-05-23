package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

public class ReverseCollection<E> implements Collection<E> {
    final Collection<E> original;

    public ReverseCollection(Collection<E> original) {
        this.original = original;
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        Object[] obj = original.toArray();
        return new Iterator<>() {
            int pos = original.size();

            @Override
            public boolean hasNext() {
                return pos >= 1;
            }

            @Override
            public E next() {
                return (E) obj[--pos];
            }
        };
    }

    @NotNull
    @Override
    public Object[] toArray() {
        Object[] obj = original.toArray();
        for (int i = 0, j = obj.length - 1; i < j; i++, j--) {
            Object tmp = obj[i];
            obj[i] = obj[j];
            obj[j] = tmp;
        }
        return obj;
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        T[] obj = original.toArray(a);
        for (int i = 0, j = original.size() - 1; i < j; i++, j--) {
            T tmp = obj[i];
            obj[i] = obj[j];
            obj[j] = tmp;
        }
        return obj;
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("JVMDG.ReverseCollection cannot call add currently.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("JVMDG.ReverseCollection cannot call remove currently.");
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return original.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException("JVMDG.ReverseCollection cannot call addAll currently.");
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return original.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return original.retainAll(c);
    }

    @Override
    public void clear() {
        original.clear();
    }

    @Override
    public int size() {
        return original.size();
    }

    @Override
    public boolean isEmpty() {
        return original.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return original.contains(o);
    }
}
