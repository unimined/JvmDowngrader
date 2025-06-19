package xyz.wagyourtail.jvmdg.j9.intl;

import java.util.*;

import static xyz.wagyourtail.jvmdg.j9.intl.ImmutableCollections.*;

/**
 * bridge class between {@link ImmutableCollections} and {@link List} / {@link java.util.Set} / {@link java.util.Map}
 * static factory method so that modifications to {@link ImmutableCollections} can be minimal
 * @author ZZZank
 */
public interface ImmutableColAccess {
    static <E> List<E> list0() {
        return (List<E>) EMPTY_LIST;
    }

    static <E> List<E> list1(E e1) {
        return new List12<>(e1);
    }

    static <E> List<E> list2(E e1, E e2) {
        return new List12<>(e1, e2);
    }

    static <E> List<E> listN(E... elements) {
        return listFromArray(elements);
    }

    static <E> List<E> listNTrusted(Object... elements) {
        return listFromTrustedArray(elements);
    }

    static <E> List<E> listNTrustedNullable(Object... elements) {
        return listFromTrustedArrayNullsAllowed(elements);
    }

    static <E> List<E> listNCopy(Collection<? extends E> coll) {
        return listCopy(coll);
    }

    static <E> Set<E> set0() {
        return (Set<E>) EMPTY_SET;
    }

    static <E> Set<E> set1(E e1) {
        return new Set12<>(e1);
    }

    static <E> Set<E> set2(E e1, E e2) {
        return new Set12<>(e1, e2);
    }

    /// no "trusted" variant
    @SafeVarargs
    static <E> Set<E> setN(E... elements) {
        return new SetN<>(elements);
    }

    static <K, V> Map<K, V> map0() {
        return (Map<K, V>) EMPTY_MAP;
    }

    static <K, V> Map<K, V> map1(K k, V v) {
        return new Map1<>(k, v);
    }

    /// no "trusted" variant
    static <K, V> Map<K, V> mapN(Object... input) {
        return new MapN<>(input);
    }

    static <K, V> Map.Entry<K, V> mapEntry(K k, V v) {
        // KeyValueHolder checks for nulls
        return new KeyValueHolder<>(k, v);
    }

    @SafeVarargs
    static <K, V> Map<K, V> mapNEntries(Map.Entry<? extends K, ? extends V>... entries) {
        if (entries.length == 0) { // implicit null check of entries array
            return map0();
        } else if (entries.length == 1) {
            // implicit null check of the array slot
            Map.Entry<? extends K, ? extends V> entry = entries[0];
            return map1(entry.getKey(), entry.getValue());
        } else {
            Object[] kva = new Object[entries.length << 1];
            int a = 0;
            for (Map.Entry<? extends K, ? extends V> entry : entries) {
                // implicit null checks of each array slot
                kva[a++] = entry.getKey();
                kva[a++] = entry.getValue();
            }
            return mapN(kva);
        }
    }

    static <K, V> Map<K, V> mapNCopy(Map<? extends K, ? extends V> map) {
        if (map instanceof ImmutableCollections.AbstractImmutableMap) {
            return (Map<K,V>)map;
        } else if (map.isEmpty()) { // Implicit nullcheck of map
            return map0();
        } else {
            return mapNEntries(map.entrySet().toArray(new Map.Entry[0]));
        }
    }
}
