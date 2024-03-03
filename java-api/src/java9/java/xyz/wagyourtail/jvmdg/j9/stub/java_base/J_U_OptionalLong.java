package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.OptionalLong;
import java.util.function.LongConsumer;
import java.util.stream.LongStream;

public class J_U_OptionalLong {

    @Stub
    public static void ifPresentOrElse(OptionalLong optional, LongConsumer action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.getAsLong());
        } else {
            emptyAction.run();
        }
    }

    @Stub
    public static LongStream stream(OptionalLong optional) {
        return optional.isPresent() ? LongStream.of(optional.getAsLong()) : LongStream.empty();
    }

}
