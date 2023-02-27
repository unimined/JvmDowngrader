package xyz.wagyourtail.jvmdg.internal.mods.stub._16;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class J_U_S_IntStream {

    @Stub(value = JavaVersion.VERSION_16)
    public static IntStream mapMulti(IntStream stream, IntMapMultiConsumer mapper) {
        Objects.requireNonNull(mapper);
        return stream.flatMap(mapper::of);
    }

    @FunctionalInterface
    @Stub(value = JavaVersion.VERSION_16, desc = "Ljava/util/stream/IntStream$IntMapMultiConsumer;")
    public interface IntMapMultiConsumer {

        default IntStream of(int d) {
            IntStream.Builder integers = IntStream.builder();
            accept(d, integers);
            return integers.build();
        }

        /**
         * Replaces the given {@code value} with zero or more values by feeding the mapped
         * values to the {@code dc} consumer.
         *
         * @param value the double value coming from upstream
         * @param dc a {@code DoubleConsumer} accepting the mapped values
         */
        void accept(int value, IntConsumer dc);
    }
}
