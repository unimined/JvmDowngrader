package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.*;

import static xyz.wagyourtail.jvmdg.j9.intl.ImmutableColAccess.*;

public class J_U_Map {

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of() {
        return map0();
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1) {
        return map1(k1, v1);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        return mapN(k1, v1, k2, v2);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return mapN(k1, v1, k2, v2, k3, v3);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return mapN(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return mapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        return mapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        return mapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        return mapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        return mapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        return mapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
    }

    @SafeVarargs
    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> ofEntries(Map.Entry<? extends K, ? extends V>... entries) {
        return mapNEntries(entries);
    }

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map.Entry<K, V> entry(K k, V v) {
        return mapEntry(k ,v);
    }

}
