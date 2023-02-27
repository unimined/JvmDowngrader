package xyz.wagyourtail.jvmdg.internal.mods.stub._16;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.stream.DoubleStream;

public class J_U_S_DoubleStream {

    @Stub(value = JavaVersion.VERSION_16)
    public static DoubleStream mapMulti(DoubleStream stream, DoubleMapMultiConsumer mapper) {
        Objects.requireNonNull(mapper);
        return stream.flatMap(mapper::of);
    }

    @FunctionalInterface
    @Stub(value = JavaVersion.VERSION_16, desc = "Ljava/util/stream/DoubleStream$DoubleMapMultiConsumer;")
    public interface DoubleMapMultiConsumer {

        default DoubleStream of(double d) {
            DoubleStream.Builder doubles = DoubleStream.builder();
            accept(d, doubles);
            return doubles.build();
        }

        /**
         * Replaces the given {@code value} with zero or more values by feeding the mapped
         * values to the {@code dc} consumer.
         *
         * @param value the double value coming from upstream
         * @param dc a {@code DoubleConsumer} accepting the mapped values
         */
        void accept(double value, DoubleConsumer dc);
    }
}
