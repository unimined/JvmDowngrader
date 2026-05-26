package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;

import java.util.*;

public class ReverseSet<E, T extends Set<E>> extends ReverseCollection<E, T> implements Set<E> {

    private ReverseSet(T original) {
        super(original);
    }

    public static <E> ReverseSet<E, ?> create(Set<E> set) {
        if (set instanceof SortedSet<E>) {
            return new ReverseSortedSet<>((SortedSet<E>) set);
        }
        return new ReverseSet<>(set);
    }

    public static class ReverseSortedSet<E, T extends SortedSet<E>> extends ReverseSet<E, T> implements SortedSet<E> {

        private ReverseSortedSet(T original) {
            super(original);
        }

        @Nullable
        @Override
        public Comparator<? super E> comparator() {
            return Collections.reverseOrder(original.comparator());
        }

        @NotNull
        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return this.tailSet(fromElement).headSet(toElement);
        }

        @NotNull
        @Override
        public SortedSet<E> headSet(E toElement) {
            throw MissingStubError.create();
        }

        @NotNull
        @Override
        public SortedSet<E> tailSet(E fromElement) {
            throw MissingStubError.create();
        }

        @Override
        public E first() {
            return original.last();
        }

        @Override
        public E last() {
            return original.first();
        }

    }

}
