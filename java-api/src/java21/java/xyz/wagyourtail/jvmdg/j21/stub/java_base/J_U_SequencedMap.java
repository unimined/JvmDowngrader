package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.j21.impl.ReverseMap;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.*;

@Adapter(value = "java/util/SequencedMap", target = "java/util/Map")
public class J_U_SequencedMap {

    private J_U_SequencedMap() {
    }

    public static boolean jvmdg$instanceof(Object obj) {
        return obj instanceof LinkedHashMap<?, ?> ||
            obj instanceof SortedMap<?, ?> ||
            obj instanceof ReverseMap<?, ?>;
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

    @Stub
    public static <K, V> Map<K, V> reversed(Map<K, V> self) {
        if (self instanceof ReverseMap<K, V> rs) {
            return rs.original;
        }
        return new ReverseMap<>(self);
    }

    @Stub
    public static <K, V> Map.Entry<K, V> firstEntry(Map<K, V> self) {
        return self.entrySet().iterator().next();
    }

    @Stub
    public static <K, V> Map.Entry<K, V> lastEntry(Map<K, V> self) {
        return reversed(self).entrySet().iterator().next();
    }

    @Stub
    public static <K, V> Map.Entry<K, V> pollFirstEntry(Map<K, V> self) {
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
        throw new UnsupportedOperationException();
    }

    @Stub
    public static <K, V> V putLast(Map<K, V> self, K key, V value) {
        throw new UnsupportedOperationException();
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
