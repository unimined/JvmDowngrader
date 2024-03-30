package xyz.wagyourtail.jvmdg.j17.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.AbstractMap;
import java.util.Map;

public class J_U_Map$Entry {

    @Stub(ref = @Ref("java/util/Map$Entry"))
    public static <K, V> Map.Entry<K, V> copyOf(Map.Entry<K, V> entry) {
        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue());
    }

}
