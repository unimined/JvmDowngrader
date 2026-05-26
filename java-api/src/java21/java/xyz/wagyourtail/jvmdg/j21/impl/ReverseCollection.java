package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

public class ReverseCollection<E, T extends Collection<E>> extends AbstractCollection<E> implements Collection<E> {
    public final T original;

    public ReverseCollection(T original) {
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
        return original.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return original.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return original.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        return original.addAll(c);
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
