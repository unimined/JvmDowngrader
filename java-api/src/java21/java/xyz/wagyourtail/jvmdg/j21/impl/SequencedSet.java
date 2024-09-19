package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SequencedSet<E> implements Set<E> {
    final Map<E, Boolean> map;

    public SequencedSet(Map<E, Boolean> map) {
        this.map = map;
    }

    @Override
    public boolean add(E e) {
        return map.put(e, Boolean.TRUE) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return map.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        boolean changed = false;
        for (E e : c) {
            if (map.put(e, Boolean.TRUE) == null) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return map.keySet().retainAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return map.keySet().removeAll(c);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return map.keySet().toArray(a);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

}
