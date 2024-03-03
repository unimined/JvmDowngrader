package xyz.wagyourtail.jvmdg.j17.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.AbstractMap;
import java.util.Map;

public class J_U_Map$Entry {

    @Stub
    public static <K, V> Map.Entry<K, V> copyOf(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

}
