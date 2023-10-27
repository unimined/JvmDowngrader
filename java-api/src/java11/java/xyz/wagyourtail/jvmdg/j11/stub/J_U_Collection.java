package xyz.wagyourtail.jvmdg.j11.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collection;
import java.util.function.IntFunction;

public class J_U_Collection {

    @Stub
    public static <T> T[] toArray(Collection<T> collection, IntFunction<T[]> generator) {
        return collection.toArray(generator.apply(0));
    }

}
