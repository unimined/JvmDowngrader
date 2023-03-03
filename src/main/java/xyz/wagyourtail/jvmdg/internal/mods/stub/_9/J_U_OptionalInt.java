package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class J_U_OptionalInt {

    @Stub(JavaVersion.VERSION_1_9)
    public static void ifPresentOrElse(OptionalInt optional, IntConsumer action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.getAsInt());
        } else {
            emptyAction.run();
        }
    }

    @Stub(JavaVersion.VERSION_1_9)
    public static IntStream stream(OptionalInt optional) {
        return optional.isPresent() ? IntStream.of(optional.getAsInt()) : IntStream.empty();
    }
}
