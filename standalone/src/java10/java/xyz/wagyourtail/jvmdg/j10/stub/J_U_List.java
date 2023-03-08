package xyz.wagyourtail.jvmdg.j10.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.Collection;
import java.util.List;

public class J_U_List {

    @Stub(javaVersion = Opcodes.V10, ref = @Ref("Ljava/util/List;"))
    public static <E> List<E> copyOf(Collection<? extends E> coll) {
        return (List) List.of(coll.toArray());
    }

}
