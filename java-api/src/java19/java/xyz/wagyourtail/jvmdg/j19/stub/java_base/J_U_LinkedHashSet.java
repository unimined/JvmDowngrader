package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.LinkedHashSet;

public class J_U_LinkedHashSet {

    @Stub(ref = @Ref("java/util/LinkedHashSet"))
    public static <E> LinkedHashSet<E> newLinkedHashSet(int count) {
        return new LinkedHashSet<>(J_U_HashMap.calculateHashMapCapacity(count), .75f);
    }

}
