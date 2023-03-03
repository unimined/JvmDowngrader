package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class J_U_Optional {

    @Stub(JavaVersion.VERSION_1_9)
    public static <T> void ifPresentOrElse(Optional<T> optional, Consumer<? super T> action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.get());
        } else {
            emptyAction.run();
        }
    }

    @Stub(JavaVersion.VERSION_1_9)
    public static <T> Optional<T> or(Optional<T> optional, Supplier<? extends Optional<? extends T>> supplier) {
        if (optional.isPresent()) {
            return optional;
        } else {
            Optional<? extends T> result = supplier.get();
            return (Optional<T>) result;
        }
    }

    @Stub(JavaVersion.VERSION_1_9)
    public static <T> Stream<T> stream(Optional<T> optional) {
        return optional.isPresent() ? Stream.of(optional.get()) : Stream.empty();
    }
}
