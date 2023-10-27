package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

public class J_L_Iterable {

    @Stub
    public static void forEach(Iterable<?> iterable, J_U_F_Consumer<?> action) {
        Objects.requireNonNull(action);
        for (Object o : iterable) {
            action.accept(o);
        }
    }

    @Stub
    public static J_U_Spliterator<?> spliterator(Iterable<?> iterable) {
        return J_U_Spliterators.spliteratorUnknownSize(iterable.iterator(), 0);
    }

}
