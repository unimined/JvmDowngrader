package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class J_U_OptionalDouble {

    @Stub(JavaVersion.VERSION_1_9)
    public static void ifPresentOrElse(OptionalDouble optional, DoubleConsumer action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.getAsDouble());
        } else {
            emptyAction.run();
        }
    }

    @Stub(JavaVersion.VERSION_1_9)
    public static DoubleStream stream(OptionalDouble optional) {
        return optional.isPresent() ? DoubleStream.of(optional.getAsDouble()) : DoubleStream.empty();
    }
}
