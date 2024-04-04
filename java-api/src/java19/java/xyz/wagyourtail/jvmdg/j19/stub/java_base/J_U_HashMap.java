package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.HashMap;

public class J_U_HashMap {

    static int calculateHashMapCapacity(int numMappings) {
        return (int) Math.ceil(numMappings / .75);
    }

    @Stub(ref = @Ref("java/util/HashMap"))
    public static <K, V> HashMap<K, V> newHashMap(int count) {
        return new HashMap<>(calculateHashMapCapacity(count), .75f);
    }

}
