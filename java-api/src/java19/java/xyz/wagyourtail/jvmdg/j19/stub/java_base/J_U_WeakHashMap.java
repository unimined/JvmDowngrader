package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.WeakHashMap;

public class J_U_WeakHashMap {

    @Stub(ref = @Ref("java/util/WeakHashMap"))
    public static <K, V> WeakHashMap<K, V> newWeakHashMap(int count) {
        return new WeakHashMap<>(J_U_HashMap.calculateHashMapCapacity(count), .75f);
    }

}
