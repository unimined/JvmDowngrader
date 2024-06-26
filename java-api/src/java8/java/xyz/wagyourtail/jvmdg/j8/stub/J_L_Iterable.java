package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

public class J_L_Iterable {

    @Stub
    public static <T> void forEach(Iterable<T> iterable, J_U_F_Consumer<T> action) {
        Objects.requireNonNull(action);
        for (T o : iterable) {
            action.accept(o);
        }
    }

    @Stub
    public static <T> J_U_Spliterator<T> spliterator(Iterable<T> iterable) {
        return J_U_Spliterators.spliteratorUnknownSize(iterable.iterator(), 0);
    }

}
