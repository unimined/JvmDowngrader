package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class J_U_LinkedHashMap {

    @Stub(ref = @Ref("java/util/LinkedHashMap"))
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int count) {
        return new LinkedHashMap<>(J_U_HashMap.calculateHashMapCapacity(count), .75f);
    }

}
