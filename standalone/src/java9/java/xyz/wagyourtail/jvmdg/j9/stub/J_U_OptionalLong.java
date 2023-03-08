package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.OptionalLong;
import java.util.function.LongConsumer;
import java.util.stream.LongStream;

public class J_U_OptionalLong {

    @Stub(javaVersion = Opcodes.V9)
    public static void ifPresentOrElse(OptionalLong optional, LongConsumer action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.getAsLong());
        } else {
            emptyAction.run();
        }
    }

    @Stub(javaVersion = Opcodes.V9)
    public static LongStream stream(OptionalLong optional) {
        return optional.isPresent() ? LongStream.of(optional.getAsLong()) : LongStream.empty();
    }

}
