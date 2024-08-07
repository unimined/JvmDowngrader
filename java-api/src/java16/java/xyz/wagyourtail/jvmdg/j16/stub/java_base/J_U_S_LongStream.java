package xyz.wagyourtail.jvmdg.j16.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.CoverageIgnore;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.function.LongConsumer;
import java.util.stream.LongStream;

public class J_U_S_LongStream {

    @Stub
    public static LongStream mapMulti(LongStream stream, LongMapMultiConsumer mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        return stream.flatMap(mapper::of);
    }

    @FunctionalInterface
    @Adapter("Ljava/util/stream/LongStream$LongMapMultiConsumer;")
    public interface LongMapMultiConsumer {

        @CoverageIgnore
        default LongStream of(long d) {
            LongStream.Builder longs = LongStream.builder();
            accept(d, longs);
            return longs.build();
        }

        /**
         * Replaces the given {@code value} with zero or more values by feeding the mapped
         * values to the {@code dc} consumer.
         *
         * @param value the double value coming from upstream
         * @param dc    a {@code DoubleConsumer} accepting the mapped values
         */
        void accept(long value, LongConsumer dc);

    }

}
