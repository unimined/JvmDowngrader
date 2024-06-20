package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ReverseList<E> implements List<E> {
    public final List<E> original;

    public ReverseList(List<E> original) {
        this.original = original;
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

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            int pos = original.size();

            @Override
            public boolean hasNext() {
                return pos >= 1;
            }

            @Override
            public E next() {
                return original.get(--pos);
            }

            @Override
            public void remove() {
                original.remove(pos);
            }

        };
    }

    @NotNull
    @Override
    public Object[] toArray() {
        List<?> list = new ArrayList<>(original);
        Collections.reverse(list);
        return list.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        List<T> list = new ArrayList<>(Arrays.asList(a));
        Collections.reverse(list);
        return list.toArray(a);
    }

    @Override
    public boolean add(E e) {
        int size = original.size();
        original.add(0, e);
        return original.size() > size;
    }

    @Override
    public boolean remove(Object o) {
        int i = original.lastIndexOf(o);
        if (i == -1) {
            return false;
        }
        E el = original.remove(i);
        return el != null;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return new HashSet<>(original).containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        int size = original.size();
        for (E e : c) {
            add(e);
        }
        return original.size() > size;
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        int size = original.size();
        for (E e : c) {
            add(index++, e);
        }
        return original.size() > size;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        int size = original.size();
        for (Object o : c) {
            remove(o);
        }
        return original.size() < size;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        int size = original.size();
        for (int i = original.size() - 1; i >= 0; i--) {
            if (!c.contains(original.get(i))) {
                original.remove(i);
            }
        }
        return original.size() < size;
    }

    @Override
    public void clear() {
        original.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ReverseList rl) {
            return original.equals(rl.original);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (E e : this)
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        return hashCode;
    }

    @Override
    public E get(int index) {
        return original.get(original.size() - index - 1);
    }

    @Override
    public E set(int index, E element) {
        return original.set(original.size() - index - 1, element);
    }

    @Override
    public void add(int index, E element) {
        original.add(original.size() - index, element);
    }

    @Override
    public E remove(int index) {
        return original.remove(original.size() - index - 1);
    }

    @Override
    public int indexOf(Object o) {
        return original.lastIndexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return original.indexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator(int index) {
        return new ListIterator<>() {
            int pos = original.size() - index;

            @Override
            public boolean hasNext() {
                return pos >= 1;
            }

            @Override
            public E next() {
                return original.get(--pos);
            }

            @Override
            public boolean hasPrevious() {
                return pos < original.size();
            }

            @Override
            public E previous() {
                return original.get(pos++);
            }

            @Override
            public int nextIndex() {
                return original.size() - pos;
            }

            @Override
            public int previousIndex() {
                return original.size() - pos - 1;
            }

            @Override
            public void remove() {
                original.remove(pos);
            }

            @Override
            public void set(E e) {
                original.set(pos, e);
            }

            @Override
            public void add(E e) {
                original.add(pos, e);
            }
        };
    }

    @NotNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return new ReverseList<>(original.subList(original.size() - toIndex, original.size() - fromIndex));
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }
}
