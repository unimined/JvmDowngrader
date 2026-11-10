package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.ReverseMap;
import xyz.wagyourtail.jvmdg.j21.impl.UnmodifiableSequencedMap;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.*;

@JEP(431)
@Adapter(value = "java/util/SequencedMap", target = "java/util/Map")
public class J_U_SequencedMap {

    private J_U_SequencedMap() {
    }

    public static boolean jvmdg$instanceof(Object obj) {
        return obj instanceof LinkedHashMap<?, ?> ||
            obj instanceof SortedMap<?, ?> ||
            obj instanceof ReverseMap<?, ?, ?> ||
            obj instanceof UnmodifiableSequencedMap<?, ?, ?>;
    }

    public static Map<?, ?> jvmdg$checkcast(Object obj) {
        if (!jvmdg$instanceof(obj)) {
            throw new ClassCastException();
        }
        if (obj instanceof Map<?, ?>) {
            return (Map<?, ?>) obj;
        }
        throw new ClassCastException();
    }

    /**
     * unmodifiable map wrappers throw {@link UnsupportedOperationException} from
     * the mutating sequenced operations unconditionally (even when empty), so
     * they must be detected before the empty checks below.
     */
    private static boolean isUnmodifiable(Map<?, ?> self) {
        if (self instanceof UnmodifiableSequencedMap<?, ?, ?>) {
            return true;
        }
        String name = self.getClass().getName();
        return name.startsWith("java.util.Collections$Unmodifiable") ||
            name.startsWith("java.util.Collections$Empty") ||
            name.startsWith("java.util.Collections$Singleton") ||
            name.startsWith("java.util.ImmutableCollections$");
    }

    @Stub
    public static <K, V> Map<K, V> reversed(Map<K, V> self) {
        if (self instanceof UnmodifiableSequencedMap<K, V, ?> unmodifiable) {
            return new UnmodifiableSequencedMap<>((Map<K, V>) reversed(unmodifiable.original));
        }
        if (self instanceof NavigableMap<K,V> navigableMap) {
            return navigableMap.descendingMap();
        }
        if (self instanceof ReverseMap<K, V, ?> rs) {
            return rs.original;
        }
        return new ReverseMap<>(self);
    }

    @Stub
    public static <K, V> Map.Entry<K, V> firstEntry(Map<K, V> self) {
        Iterator<Map.Entry<K, V>> it = self.entrySet().iterator();
        return it.hasNext() ? it.next() : null;
    }

    @Stub
    public static <K, V> Map.Entry<K, V> lastEntry(Map<K, V> self) {
        Iterator<Map.Entry<K, V>> it = reversed(self).entrySet().iterator();
        return it.hasNext() ? it.next() : null;
    }

    @Stub
    public static <K, V> Map.Entry<K, V> pollFirstEntry(Map<K, V> self) {
        if (isUnmodifiable(self)) {
            throw new UnsupportedOperationException();
        }
        var it = self.entrySet().iterator();
        if (!it.hasNext()) {
            return null;
        }
        var entry = it.next();
        it.remove();
        return entry;
    }

    @Stub
    public static <K, V> Map.Entry<K, V> pollLastEntry(Map<K, V> self) {
        return pollFirstEntry(reversed(self));
    }

    @Stub
    public static <K, V> V putFirst(Map<K, V> self, K key, V value) {
        if (self instanceof ReverseMap<K, V, ?> reverseMap) {
            // first in the reversed view is last in the original
            return putLast(reverseMap.original, key, value);
        }
        if (self instanceof LinkedHashMap<K, V> lhm) {
            return putFirstLinked(lhm, key, value);
        }
        throw new UnsupportedOperationException();
    }

    @Stub
    public static <K, V> V putLast(Map<K, V> self, K key, V value) {
        if (self instanceof ReverseMap<K, V, ?> reverseMap) {
            // last in the reversed view is first in the original
            return putFirst(reverseMap.original, key, value);
        }
        if (self instanceof LinkedHashMap<K, V> lhm) {
            return putLastLinked(lhm, key, value);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * {@code LinkedHashMap.putFirst}: inserts or moves the entry to the head of
     * the encounter order, returning the previous value.
     */
    private static <K, V> V putFirstLinked(LinkedHashMap<K, V> lhm, K key, V value) {
        V old = lhm.get(key);
        LinkedHashMap<K, V> rest = new LinkedHashMap<>();
        for (Map.Entry<K, V> e : lhm.entrySet()) {
            if (!e.getKey().equals(key)) {
                rest.put(e.getKey(), e.getValue());
            }
        }
        lhm.clear();
        lhm.put(key, value);
        lhm.putAll(rest);
        return old;
    }

    /**
     * {@code LinkedHashMap.putLast}: inserts or moves the entry to the tail of
     * the encounter order, returning the previous value.
     */
    private static <K, V> V putLastLinked(LinkedHashMap<K, V> lhm, K key, V value) {
        if (!lhm.containsKey(key)) {
            return lhm.put(key, value);
        }
        V old = lhm.remove(key);
        lhm.put(key, value);
        return old;
    }

    @Stub
    public static <K> Set<K> sequencedKeySet(Map<K, ?> self) {
        return self.keySet();
    }

    @Stub
    public static <V> Collection<V> sequencedValues(Map<?, V> self) {
        return self.values();
    }

    @Stub
    public static <K, V> Set<Map.Entry<K, V>> sequencedEntrySet(Map<K, V> self) {
        return self.entrySet();
    }

}
