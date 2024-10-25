package xyz.wagyourtail.jvmdg.j8.intl.collections;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class SynchronizedBackingMap<K, V> implements Map<K, V> {
    protected final Object mutex;
    private final Map<K, V> backing;

    public SynchronizedBackingMap(Map<K, V> backing) {
        this.backing = backing;
        this.mutex = this;
    }

    public SynchronizedBackingMap(Map<K, V> backing, Object mutex) {
        this.backing = backing;
        this.mutex = mutex;
    }


    @Override
    public int size() {
        synchronized (mutex) {
            return backing.size();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (mutex) {
            return backing.isEmpty();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        synchronized (mutex) {
            return backing.containsKey(key);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        synchronized (mutex) {
            return backing.containsValue(value);
        }
    }

    @Override
    public V get(Object key) {
        synchronized (mutex) {
            return backing.get(key);
        }
    }

    @Override
    public V put(K key, V value) {
        synchronized (mutex) {
            return backing.put(key, value);
        }
    }

    @Override
    public V remove(Object key) {
        synchronized (mutex) {
            return backing.remove(key);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        synchronized (mutex) {
            backing.putAll(m);
        }
    }

    @Override
    public void clear() {
        synchronized (mutex) {
            backing.clear();
        }
    }

    @Override
    public Set<K> keySet() {
        synchronized (mutex) {
            return new SynchronizedBackingSet<>(backing.keySet(), mutex);
        }
    }

    @Override
    public Collection<V> values() {
        synchronized (mutex) {
            return new SynchronizedBackingCollection<>(backing.values(), mutex);
        }
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        synchronized (mutex) {
            return new SynchronizedBackingSet<>(backing.entrySet(), mutex);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        synchronized (mutex) {
            return backing.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        synchronized (mutex) {
            return backing.hashCode();
        }
    }

}
