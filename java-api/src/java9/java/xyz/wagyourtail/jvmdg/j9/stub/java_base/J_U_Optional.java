package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class J_U_Optional {

    @Stub
    public static <T> void ifPresentOrElse(Optional<T> optional, Consumer<? super T> action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.get());
        } else {
            emptyAction.run();
        }
    }

    @Stub
    public static <T> Optional<T> or(Optional<T> optional, Supplier<? extends Optional<? extends T>> supplier) {
        if (optional.isPresent()) {
            return optional;
        } else {
            Optional<? extends T> result = supplier.get();
            return (Optional<T>) result;
        }
    }

    @Stub
    public static <T> Stream<T> stream(Optional<T> optional) {
        return optional.map(Stream::of).orElseGet(Stream::empty);
    }

}
