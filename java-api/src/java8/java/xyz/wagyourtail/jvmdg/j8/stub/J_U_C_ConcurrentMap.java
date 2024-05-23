package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BiConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BiFunction;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Function;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

public class J_U_C_ConcurrentMap {

    @Stub
    public static <K, V> V replace(ConcurrentMap<K, V> map, K key, V value) {
        return map.replace(key, value);
    }

    @Stub
    public static <K, V> boolean replace(ConcurrentMap<K, V> map, K key, V oldValue, V newValue) {
        return map.replace(key, oldValue, newValue);
    }

    @Stub
    public static <K, V> boolean remove(ConcurrentMap<K, V> map, K key, V value) {
        return map.remove(key, value);
    }

    @Stub
    public static <K, V> V getOrDefault(ConcurrentMap<K, V> map, Object key, V defaultValue) {
        V v;
        return (((v = map.get(key)) != null)) ? v : defaultValue;
    }

    @Stub
    public static <K, V> void forEach(ConcurrentMap<K, V> map, J_U_F_BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<K, V> entry : map.entrySet()) {
            K k;
            V v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch (IllegalStateException ise) {
                // this usually means the entry is no longer in the map.
                // ignore the entry.
                continue;
            }
            action.accept(k, v);
        }
    }

    @Stub
    public static <K, V> void replaceAll(final ConcurrentMap<K, V> map, final J_U_F_BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        forEach(map, new J_U_F_BiConsumer.BiConsumerAdapter<K, V>() {
            @Override
            public void accept(K k, V v) {
                while (!map.replace(k, v, function.apply(k, v))) {
                    if ((v = map.get(k)) == null) {
                        break;
                    }
                }
            }
        });
    }

    @Stub
    public static <K, V> V computeIfAbsent(ConcurrentMap<K, V> map, K key, J_U_F_Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V oldValue, newValue;
        return ((oldValue = map.get(key)) == null && (newValue = mappingFunction.apply(key)) != null && (oldValue = map.putIfAbsent(key, newValue)) == null) ? newValue : oldValue;
    }

    @Stub
    public static <K, V> V computeIfPresent(ConcurrentMap<K, V> map, K key, J_U_F_BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue;
        while ((oldValue = map.get(key)) != null) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue == null) {
                if (map.remove(key, oldValue)) {
                    return null;
                }
            } else if (map.replace(key, oldValue, newValue)) {
                return newValue;
            }
        }
        return null;
    }

    @Stub
    public static <K, V> V compute(ConcurrentMap<K, V> map, K key, J_U_F_BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        retry:
        for (; ; ) {
            V oldValue = map.get(key);
            // if putIfAbsent fails, opportunistically use its return value
            haveOldValue:
            for (; ; ) {
                V newValue = remappingFunction.apply(key, oldValue);
                if (newValue != null) {
                    if (oldValue != null) {
                        if (map.replace(key, oldValue, newValue))
                            return newValue;
                    } else if ((oldValue = map.putIfAbsent(key, newValue)) == null)
                        return newValue;
                    else continue haveOldValue;
                } else if (oldValue == null || map.remove(key, oldValue)) {
                    return null;
                }
                continue retry;
            }
        }
    }

    @Stub
    public static <K, V> V merge(ConcurrentMap<K, V> map, K key, V value, J_U_F_BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);
        retry:
        for (; ; ) {
            V oldValue = map.get(key);
            // if putIfAbsent fails, opportunistically use its return value
            haveOldValue:
            for (; ; ) {
                if (oldValue != null) {
                    V newValue = remappingFunction.apply(oldValue, value);
                    if (newValue != null) {
                        if (map.replace(key, oldValue, newValue))
                            return newValue;
                    } else if (map.remove(key, oldValue)) {
                        return null;
                    }
                    continue retry;
                } else {
                    if ((oldValue = map.putIfAbsent(key, value)) == null)
                        return value;
                }
            }
        }
    }

}
