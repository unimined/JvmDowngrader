package xyz.wagyourtail.jvmdg.j16.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.CoverageIgnore;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.stream.DoubleStream;

public class J_U_S_DoubleStream {

    @Stub
    public static DoubleStream mapMulti(DoubleStream stream, DoubleMapMultiConsumer mapper) {
        Objects.requireNonNull(mapper);
        return stream.flatMap(mapper::of);
    }

    @FunctionalInterface
    @Adapter("Ljava/util/stream/DoubleStream$DoubleMapMultiConsumer;")
    public interface DoubleMapMultiConsumer {

        @CoverageIgnore
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
         * @param dc    a {@code DoubleConsumer} accepting the mapped values
         */
        void accept(double value, DoubleConsumer dc);

    }

}
