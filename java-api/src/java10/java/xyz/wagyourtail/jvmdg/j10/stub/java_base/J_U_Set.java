package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class J_U_Set {

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> copyOf(Collection<? extends E> coll) {
        if (coll.isEmpty()) { // implicit null check
            return Set.of();
        }
        // unlike Set.of, copyOf drops duplicates silently (unspecified which
        // instance is retained) but still rejects nulls
        Set<E> set = new HashSet<>(coll.size() * 2);
        for (E e : coll) {
            set.add(Objects.requireNonNull(e));
        }
        return Collections.unmodifiableSet(set);
    }

}
