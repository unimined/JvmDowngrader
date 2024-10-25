package xyz.wagyourtail.jvmdg.j8.intl.collections;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;

public class SynchronizedNavigableMap<K, V> extends SynchronizedBackingMap<K, V> implements NavigableMap<K, V> {
    private final NavigableMap<K, V> backing;

    public SynchronizedNavigableMap(NavigableMap<K, V> backing) {
        super(backing);
        this.backing = backing;
    }

    public SynchronizedNavigableMap(NavigableMap<K, V> backing, Object mutex) {
        super(backing, mutex);
        this.backing = backing;
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        synchronized (mutex) {
            return backing.lowerEntry(key);
        }
    }

    @Override
    public K lowerKey(K key) {
        synchronized (mutex) {
            return backing.lowerKey(key);
        }
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        synchronized (mutex) {
            return backing.floorEntry(key);
        }
    }

    @Override
    public K floorKey(K key) {
        synchronized (mutex) {
            return backing.floorKey(key);
        }
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        synchronized (mutex) {
            return backing.ceilingEntry(key);
        }
    }

    @Override
    public K ceilingKey(K key) {
        synchronized (mutex) {
            return backing.ceilingKey(key);
        }
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        synchronized (mutex) {
            return backing.higherEntry(key);
        }
    }

    @Override
    public K higherKey(K key) {
        synchronized (mutex) {
            return backing.higherKey(key);
        }
    }

    @Override
    public Entry<K, V> firstEntry() {
        synchronized (mutex) {
            return backing.firstEntry();
        }
    }

    @Override
    public Entry<K, V> lastEntry() {
        synchronized (mutex) {
            return backing.lastEntry();
        }
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
        synchronized (mutex) {
            return backing.pollFirstEntry();
        }
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        synchronized (mutex) {
            return backing.pollLastEntry();
        }
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        synchronized (mutex) {
            return new SynchronizedNavigableMap<>(backing.descendingMap(), mutex);
        }
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        synchronized (mutex) {
            return new SynchronizedNavigableSet<>(backing.navigableKeySet(), mutex);
        }
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        synchronized (mutex) {
            return new SynchronizedNavigableSet<>(backing.descendingKeySet(), mutex);
        }
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return null;
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return null;
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return null;
    }

    @Override
    public Comparator<? super K> comparator() {
        return null;
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        return null;
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
        return null;
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        return null;
    }

    @Override
    public K firstKey() {
        return null;
    }

    @Override
    public K lastKey() {
        return null;
    }

}
