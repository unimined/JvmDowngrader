package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j21.stub.java_base.J_U_SequencedCollection;

import java.util.*;

public class ReverseMap<K, V, T extends Map<K, V>> implements Map<K, V> {
    public final T original;

    public ReverseMap(T original) {
        this.original = original;
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return ReverseSet.create(original.entrySet());
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
    public boolean containsKey(Object key) {
        return original.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return original.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return original.get(key);
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        return original.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return original.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        original.putAll(m);
    }

    @Override
    public void clear() {
        original.clear();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return ReverseSet.create(original.keySet());
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return J_U_SequencedCollection.reversed(original.values());
    }

    public static <K, V> ReverseMap<K, V, ?> create(Map<K, V> map) {
        if (map instanceof SortedMap) {
            return new ReverseSortedMap<>((SortedMap<K, V>) map);
        }
        return new ReverseMap<>(map);
    }

    public static class ReverseSortedMap<K, V, T extends SortedMap<K, V>> extends ReverseMap<K, V, T> implements SortedMap<K, V> {
        public ReverseSortedMap(T original) {
            super(original);
        }

        @Override
        public Comparator<? super K> comparator() {
            return Collections.reverseOrder(original.comparator());
        }

        @NotNull
        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return this.tailMap(fromKey).headMap(toKey);
        }

        @NotNull
        @Override
        public SortedMap<K, V> headMap(K toKey) {
            throw MissingStubError.create();
        }

        @NotNull
        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            throw MissingStubError.create();
        }

        @Override
        public K firstKey() {
            return original.lastKey();
        }

        @Override
        public K lastKey() {
            return original.firstKey();
        }

    }

}
