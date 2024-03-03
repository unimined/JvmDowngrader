package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.OptionalInt;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class J_U_OptionalInt {

    @Stub
    public static void ifPresentOrElse(OptionalInt optional, IntConsumer action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.getAsInt());
        } else {
            emptyAction.run();
        }
    }

    @Stub
    public static IntStream stream(OptionalInt optional) {
        return optional.isPresent() ? IntStream.of(optional.getAsInt()) : IntStream.empty();
    }

}
