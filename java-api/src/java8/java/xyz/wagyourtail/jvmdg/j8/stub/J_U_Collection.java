package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Predicate;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collection;

public class J_U_Collection {

    @Stub
    public static <T> boolean removeIf(Collection<T> collection, J_U_F_Predicate<T> filter) {
        //TODO
        return false;
    }

    @Stub
    public static <T> J_U_Spliterator<T> spliterator(Collection<T> collection) {
        //TODO
    }

    @Stub
    public static <T> J_U_S_Stream<T> stream(Collection<T> collection) {
        //TODO
    }

    @Stub
    public static <T> J_U_S_Stream<T> parallelStream(Collection<T> collection) {
        //TODO
    }

}
