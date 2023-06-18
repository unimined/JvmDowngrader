package xyz.wagyourtail.jvmdg.j10.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.Map;

public class J_U_Map {

    @Stub(opcVers = Opcodes.V10, ref = @Ref("Ljava/util/Map;"))
    public static <K, V> Map<K, V> copyOf(Map<? extends K, ? extends V> m) {
        return Map.ofEntries(m.entrySet().toArray(new Map.Entry[0]));
    }

}
