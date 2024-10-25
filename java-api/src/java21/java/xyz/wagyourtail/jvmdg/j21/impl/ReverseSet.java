package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public class ReverseSet<E> extends AbstractSet<E> {
    public final Set<E> original;

    public ReverseSet(Set<E> original) {
        this.original = original;
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("JVMDG.ReverseSet cannot call add currently.");
    }

    @Override
    public boolean remove(Object o) {
        return original.remove(o);
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
            public void remove() {
                original.remove(obj[pos]);
            }

            @Override
            public E next() {
                return (E) obj[--pos];
            }
        };
    }

    @Override
    public int size() {
        return original.size();
    }

}
