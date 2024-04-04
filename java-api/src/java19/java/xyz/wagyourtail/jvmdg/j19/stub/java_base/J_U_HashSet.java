package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.HashSet;

public class J_U_HashSet {

    @Stub(ref = @Ref("java/util/HashSet"))
    public static <E> HashSet<E> newHashSet(int count) {
        return new HashSet<>(J_U_HashMap.calculateHashMapCapacity(count), .75f);
    }

}
