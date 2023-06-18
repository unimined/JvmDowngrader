package xyz.wagyourtail.jvmdg.j17.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.AbstractMap;
import java.util.Map;

public class J_U_Map$Entry {

    @Stub(opcVers = Opcodes.V17)
    public static <K, V> Map.Entry<K, V> copyOf(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

}
