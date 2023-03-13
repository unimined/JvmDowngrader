package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.OptionalInt;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class J_U_OptionalInt {

    @Stub(javaVersion = Opcodes.V9)
    public static void ifPresentOrElse(OptionalInt optional, IntConsumer action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.getAsInt());
        } else {
            emptyAction.run();
        }
    }

    @Stub(javaVersion = Opcodes.V9)
    public static IntStream stream(OptionalInt optional) {
        return optional.isPresent() ? IntStream.of(optional.getAsInt()) : IntStream.empty();
    }

}
