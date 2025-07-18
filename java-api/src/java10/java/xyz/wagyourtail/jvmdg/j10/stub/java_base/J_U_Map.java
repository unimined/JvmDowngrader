package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Map;

public class J_U_Map {

    @Stub(ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> copyOf(Map<? extends K, ? extends V> m) {
        if (m.isEmpty()) { // implicit null check
            return Map.of();
        }
        return Map.ofEntries(m.entrySet().toArray(new Map.Entry[0]));
    }

}
