package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.*;

@JEP(269)
public class J_U_Map {

    private static <K, V> void put(Map<K, V> map, K k, V v) {
        if (map.put(Objects.requireNonNull(k), Objects.requireNonNull(v)) != null) {
            throw new IllegalArgumentException("duplicate key: " + k);
        }
    }

    private static <K, V> Map<K, V> mapOf(Object... kvs) {
        Map<K, V> map = new HashMap<>(toExpectedSize(kvs.length / 2));
        for (int i = 0; i < kvs.length; i += 2) {
            put(map, (K) kvs[i], (V) kvs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of() {
        return Collections.emptyMap();
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1) {
        return Collections.singletonMap(Objects.requireNonNull(k1), Objects.requireNonNull(v1));
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        return J_U_Map.<K, V>mapOf(k1, v1, k2, v2);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return J_U_Map.<K, V>mapOf(k1, v1, k2, v2, k3, v3);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return J_U_Map.<K, V>mapOf(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return J_U_Map.<K, V>mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        return J_U_Map.<K, V>mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        return J_U_Map.<K, V>mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        return J_U_Map.<K, V>mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        return J_U_Map.<K, V>mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        return J_U_Map.<K, V>mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
    }

    @SafeVarargs
    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> ofEntries(Map.Entry<? extends K, ? extends V>... entries) {
        if (entries.length == 0) {
            return Collections.emptyMap();
        }
        if (entries.length == 1) {
            return Collections.singletonMap(entries[0].getKey(), entries[0].getValue());
        }
        Map<K, V> map = new HashMap<>(toExpectedSize(entries.length));
        for (Map.Entry<? extends K, ? extends V> entry : entries) {
            put(map, entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(map);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map.Entry<K, V> entry(K k, V v) {
        return new AbstractMap.SimpleImmutableEntry<>(Objects.requireNonNull(k), Objects.requireNonNull(v));
    }

    /**
     * @see HashMap#DEFAULT_LOAD_FACTOR
     */
    private static int toExpectedSize(int elementCount) {
        assert elementCount >= 0;
        return (int) (elementCount / 0.75f) + 1;
    }
}
