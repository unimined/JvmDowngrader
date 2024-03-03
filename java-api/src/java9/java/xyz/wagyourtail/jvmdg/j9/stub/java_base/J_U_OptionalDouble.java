package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.OptionalDouble;
import java.util.function.DoubleConsumer;
import java.util.stream.DoubleStream;

public class J_U_OptionalDouble {

    @Stub
    public static void ifPresentOrElse(OptionalDouble optional, DoubleConsumer action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.getAsDouble());
        } else {
            emptyAction.run();
        }
    }

    @Stub
    public static DoubleStream stream(OptionalDouble optional) {
        return optional.isPresent() ? DoubleStream.of(optional.getAsDouble()) : DoubleStream.empty();
    }

}
