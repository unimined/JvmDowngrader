package xyz.wagyourtail.jvmdg.j8.intl.collections;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BackingMap<K, V> implements Map<K, V> {
    private final Map<K, V> backing;

    public BackingMap(Map<K, V> backing) {
        this.backing = Objects.requireNonNull(backing);
    }
    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return backing.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return backing.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return backing.get(key);
    }

    @Override
    public V put(K key, V value) {
        return backing.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return backing.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        backing.putAll(m);
    }

    @Override
    public void clear() {
        backing.clear();
    }

    @Override
    public Set<K> keySet() {
        return backing.keySet();
    }

    @Override
    public Collection<V> values() {
        return backing.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return backing.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return backing.equals(o);
    }

    @Override
    public int hashCode() {
        return backing.hashCode();
    }
}
