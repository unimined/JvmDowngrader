package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collection;
import java.util.Set;

public class J_U_Set {

    @Stub(ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> copyOf(Collection<? extends E> coll) {
        // strictly speaking this is not the "standard" approach, but it works
        return (Set<E>) Set.of(coll.toArray());
    }

}
