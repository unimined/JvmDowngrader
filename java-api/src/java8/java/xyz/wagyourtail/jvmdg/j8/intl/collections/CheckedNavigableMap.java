package xyz.wagyourtail.jvmdg.j8.intl.collections;

import java.util.*;

public class CheckedNavigableMap<K, V> extends BackingMap<K, V> implements NavigableMap<K, V> {
    private final NavigableMap<K, V> backing;
    private final Class<K> keyType;
    private final Class<V> valueType;

    public CheckedNavigableMap(Map<K, V> backing, Class<K> keyType, Class<V> valueType) {
        super(Collections.checkedMap(backing, keyType, valueType));
        this.backing = (NavigableMap<K, V>) backing;
        this.keyType = keyType;
        this.valueType = valueType;
    }


    private void typeCheck(Object key, Object value) {
        if (key != null && !keyType.isInstance(key))
            throw new ClassCastException(badKeyMsg(key));

        if (value != null && !valueType.isInstance(value))
            throw new ClassCastException(badValueMsg(value));
    }

    private String badKeyMsg(Object key) {
        return "Attempt to insert " + key.getClass() +
            " key into map with key type " + keyType;
    }

    private String badValueMsg(Object value) {
        return "Attempt to insert " + value.getClass() +
            " value into map with value type " + valueType;
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        Entry<K, V> lower = backing.lowerEntry(key);
        return (null != lower) ? new CheckedEntry<>(lower, valueType) : null;
    }

    @Override
    public K lowerKey(K key) {
        return backing.lowerKey(key);
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        Entry<K, V> floor = backing.floorEntry(key);
        return (null != floor) ? new CheckedEntry<>(floor, valueType) : null;
    }

    @Override
    public K floorKey(K key) {
        return backing.floorKey(key);
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        Entry<K, V> ceiling = backing.ceilingEntry(key);
        return (null != ceiling) ? new CheckedEntry<>(ceiling, valueType) : null;
    }

    @Override
    public K ceilingKey(K key) {
        return backing.ceilingKey(key);
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        Entry<K, V> higher = backing.higherEntry(key);
        return (null != higher) ? new CheckedEntry<>(higher, valueType) : null;
    }

    @Override
    public K higherKey(K key) {
        return backing.higherKey(key);
    }

    @Override
    public Entry<K, V> firstEntry() {
        Entry<K, V> first = backing.firstEntry();
        return (null != first) ? new CheckedEntry<>(first, valueType) : null;
    }

    @Override
    public Entry<K, V> lastEntry() {
        Entry<K, V> last = backing.lastEntry();
        return (null != last) ? new CheckedEntry<>(last, valueType) : null;
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
        Entry<K, V> first = backing.pollFirstEntry();
        return (null != first) ? new CheckedEntry<>(first, valueType) : null;
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        Entry<K, V> last = backing.pollLastEntry();
        return (null != last) ? new CheckedEntry<>(last, valueType) : null;
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        return new CheckedNavigableMap<>(backing.descendingMap(), keyType, valueType);
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return new CheckedNavigableSet<>(backing.navigableKeySet(), keyType);
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return new CheckedNavigableSet<>(backing.descendingKeySet(), keyType);
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return new CheckedNavigableMap<>(backing.subMap(fromKey, fromInclusive, toKey, toInclusive), keyType, valueType);
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return new CheckedNavigableMap<>(backing.headMap(toKey, inclusive), keyType, valueType);
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return new CheckedNavigableMap<>(backing.tailMap(fromKey, inclusive), keyType, valueType);
    }

    @Override
    public Comparator<? super K> comparator() {
        return backing.comparator();
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        return Collections.checkedSortedMap(backing.subMap(fromKey, toKey), keyType, valueType);
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
        return Collections.checkedSortedMap(backing.headMap(toKey), keyType, valueType);
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        return Collections.checkedSortedMap(backing.tailMap(fromKey), keyType, valueType);
    }

    @Override
    public K firstKey() {
        return backing.firstKey();
    }

    @Override
    public K lastKey() {
        return backing.lastKey();
    }

    private static class CheckedEntry<K, V, T> implements Map.Entry<K, V> {
        private final Map.Entry<K, V> e;
        private final Class<T> valueType;

        CheckedEntry(Map.Entry<K, V> e, Class<T> valueType) {
            this.e = Objects.requireNonNull(e);
            this.valueType = Objects.requireNonNull(valueType);
        }

        public K getKey() {
            return e.getKey();
        }

        public V getValue() {
            return e.getValue();
        }

        public int hashCode() {
            return e.hashCode();
        }

        public String toString() {
            return e.toString();
        }

        public V setValue(V value) {
            if (value != null && !valueType.isInstance(value))
                throw new ClassCastException(badValueMsg(value));
            return e.setValue(value);
        }

        private String badValueMsg(Object value) {
            return "Attempt to insert " + value.getClass() + " value into map with value type " + valueType;
        }

        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Map.Entry))
                return false;
            return e.equals(new AbstractMap.SimpleImmutableEntry<>((Map.Entry<?, ?>) o));
        }

    }

}
