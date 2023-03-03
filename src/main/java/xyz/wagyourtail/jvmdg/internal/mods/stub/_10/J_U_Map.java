package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Map;

public class J_U_Map {

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/Map;")
    public static <K, V> Map<K, V> copyOf(Map<? extends K, ? extends V> m) {
        return Map.ofEntries(m.entrySet().toArray(new Map.Entry[0]));
    }

}
