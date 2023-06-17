package xyz.wagyourtail.jvmdg.j8.intl.collections;

import java.util.*;

public class UnmodifiableNavigableMap<K, V> extends BackingMap<K, V> implements NavigableMap<K, V> {
    public static final UnmodifiableNavigableMap EMPTY = new UnmodifiableNavigableMap(new TreeMap());
    private final NavigableMap<K, V> backing;

    public UnmodifiableNavigableMap(NavigableMap<K, V> backing) {
        super(Collections.unmodifiableMap(backing));
        this.backing = backing;
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        return backing.lowerEntry(key);
    }

    @Override
    public K lowerKey(K key) {
        return backing.lowerKey(key);
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        return backing.floorEntry(key);
    }

    @Override
    public K floorKey(K key) {
        return backing.floorKey(key);
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        return backing.ceilingEntry(key);
    }

    @Override
    public K ceilingKey(K key) {
        return backing.ceilingKey(key);
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        return backing.higherEntry(key);
    }

    @Override
    public K higherKey(K key) {
        return backing.higherKey(key);
    }

    @Override
    public Entry<K, V> firstEntry() {
        return backing.firstEntry();
    }

    @Override
    public Entry<K, V> lastEntry() {
        return backing.lastEntry();
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        return new UnmodifiableNavigableMap<>(backing.descendingMap());
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return new UnmodifiableNavigableCollection<>(backing.navigableKeySet());
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return new UnmodifiableNavigableCollection<>(backing.descendingKeySet());
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return new UnmodifiableNavigableMap<>(backing.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return new UnmodifiableNavigableMap<>(backing.headMap(toKey, inclusive));
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return new UnmodifiableNavigableMap<>(backing.tailMap(fromKey, inclusive));
    }

    @Override
    public Comparator<? super K> comparator() {
        return backing.comparator();
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        return new UnmodifiableNavigableMap<>((NavigableMap<K, V>) backing.subMap(fromKey, toKey));
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
        return new UnmodifiableNavigableMap<>((NavigableMap<K, V>) backing.headMap(toKey));
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        return new UnmodifiableNavigableMap<>((NavigableMap<K, V>) backing.tailMap(fromKey));
    }

    @Override
    public K firstKey() {
        return backing.firstKey();
    }

    @Override
    public K lastKey() {
        return backing.lastKey();
    }
}
