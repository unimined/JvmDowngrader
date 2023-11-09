package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j8.intl.spliterator.IteratorSpliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Predicate;
import xyz.wagyourtail.jvmdg.j8.stub.stream.J_U_S_Stream;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collection;

public class J_U_Collection {

    @Stub
    public static <T> boolean removeIf(Collection<T> collection, J_U_F_Predicate<T> filter) {
        throw MissingStubError.create();
    }

    @Stub
    public static <T> J_U_Spliterator<T> spliterator(Collection<T> collection) {
        return J_U_Spliterators.spliterator(collection, 0);
    }

    @Stub
    public static <T> J_U_S_Stream<T> stream(Collection<T> collection) {
        throw MissingStubError.create();
    }

    @Stub
    public static <T> J_U_S_Stream<T> parallelStream(Collection<T> collection) {
        throw MissingStubError.create();
    }

}
