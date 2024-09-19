package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Iterator;
import java.util.Objects;

public class J_U_Iterator {

    @Stub(ref = @Ref("java/util/Iterator"))
    public static <T> void forEachRemaining(Iterator<T> iterator, J_U_F_Consumer<? super T> action) {
        Objects.requireNonNull(action);
        while (iterator.hasNext()) {
            action.accept(iterator.next());
        }
    }

}
