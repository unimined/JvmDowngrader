package xyz.wagyourtail.jvmdg.j10.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.Collection;
import java.util.Set;

public class J_U_Set {

    @Stub(javaVersion = Opcodes.V10, ref = @Ref("Ljava/util/Set;"))
    public static <E> Set<E> copyOf(Collection<? extends E> coll) {
        return (Set) Set.of(coll.toArray());
    }

}
