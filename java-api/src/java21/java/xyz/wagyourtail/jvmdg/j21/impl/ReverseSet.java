package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ReverseSet<E> extends AbstractSet<E> {
    public final Set<E> original;

    public ReverseSet(Set<E> original) {
        this.original = original;
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("JVMDG.ReverseSet cannot call add currently.");
    }

    /**
     * TODO: somehow have concurrent modification checks
     */
    @NotNull
    @Override
    public Iterator<E> iterator() {
        List<E> elements = new ArrayList<>(original);
        Collections.reverse(elements);
        return elements.iterator();
    }

    @Override
    public int size() {
        return original.size();
    }
}
