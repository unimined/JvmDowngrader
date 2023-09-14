package xyz.wagyourtail.jvmdg.j10.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collection;
import java.util.List;

public class J_U_List {

    @Stub(opcVers = Opcodes.V10, ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> copyOf(Collection<? extends E> coll) {
        return (List) List.of(coll.toArray());
    }

}