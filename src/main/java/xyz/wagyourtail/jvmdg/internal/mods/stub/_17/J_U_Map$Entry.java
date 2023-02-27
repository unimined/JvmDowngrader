package xyz.wagyourtail.jvmdg.internal.mods.stub._17;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.AbstractMap;
import java.util.Map;

public class J_U_Map$Entry {

    @Stub(JavaVersion.VERSION_17)
    public static <K, V> Map.Entry<K, V> copyOf(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }
}
