package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ReverseDeque<E> implements Deque<E> {
    public final Deque<E> original;

    public ReverseDeque(Deque<E> original) {
        this.original = original;
    }

    @Override
    public void addFirst(E e) {
        original.addLast(e);
    }

    @Override
    public void addLast(E e) {
        original.addFirst(e);
    }

    @Override
    public boolean offerFirst(E e) {
        return original.offerLast(e);
    }

    @Override
    public boolean offerLast(E e) {
        return original.offerFirst(e);
    }

    @Override
    public E removeFirst() {
        return original.removeLast();
    }

    @Override
    public E removeLast() {
        return original.removeFirst();
    }

    @Override
    public E pollFirst() {
        return original.pollLast();
    }

    @Override
    public E pollLast() {
        return original.pollFirst();
    }

    @Override
    public E getFirst() {
        return original.getLast();
    }

    @Override
    public E getLast() {
        return original.getFirst();
    }

    @Override
    public E peekFirst() {
        return original.peekLast();
    }

    @Override
    public E peekLast() {
        return original.peekFirst();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return original.removeLastOccurrence(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return original.removeFirstOccurrence(o);
    }

    @Override
    public boolean add(E e) {
        original.addFirst(e);
        return true;
    }

    @Override
    public boolean offer(E e) {
        return original.offerFirst(e);
    }

    @Override
    public E remove() {
        return original.removeFirst();
    }

    @Override
    public E poll() {
        return original.pollLast();
    }

    @Override
    public E element() {
        return original.peekLast();
    }

    @Override
    public E peek() {
        return original.peekLast();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        List<E> list = List.copyOf(c);
        for (int i = list.size() - 1; i >= 0; i--) {
            original.addFirst(list.get(i));
        }
        return true;
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
    public void push(E e) {
        original.addLast(e);
    }

    @Override
    public E pop() {
        return original.removeLast();
    }

    @Override
    public boolean remove(Object o) {
        return original.removeLastOccurrence(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return original.containsAll(c);
    }

    @Override
    public boolean contains(Object o) {
        return original.contains(o);
    }

    @Override
    public int size() {
        return original.size();
    }

    @Override
    public boolean isEmpty() {
        return original.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return original.descendingIterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        List<?> list = new ArrayList<>(Arrays.asList(original.toArray()));
        Collections.reverse(list);
        return list.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        List<T> list = new ArrayList<>(Arrays.asList(original.toArray(a)));
        Collections.reverse(list);
        return list.toArray(a);
    }

    @NotNull
    @Override
    public Iterator<E> descendingIterator() {
        return original.iterator();
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }
}
