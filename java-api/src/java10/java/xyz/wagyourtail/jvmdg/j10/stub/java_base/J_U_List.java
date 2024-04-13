package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collection;
import java.util.List;

public class J_U_List {

    @Stub(ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> copyOf(Collection<? extends E> coll) {
        return (List) List.of(coll.toArray());
    }

}
