package xyz.wagyourtail.jvmdg.j21.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.jvmdg.j21.stub.java_base.J_U_SequencedCollection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ReverseMap<K, V> implements Map<K, V> {
    public final Map<K, V> original;

    public ReverseMap(Map<K, V> original) {
        this.original = original;
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return new ReverseSet<>(original.entrySet());
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
        return new ReverseSet<>(original.keySet());
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return J_U_SequencedCollection.reversed(original.values());
    }

}
