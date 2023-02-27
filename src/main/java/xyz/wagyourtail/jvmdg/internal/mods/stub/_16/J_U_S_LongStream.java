package xyz.wagyourtail.jvmdg.internal.mods.stub._16;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class J_U_S_LongStream {

    @Stub(value = JavaVersion.VERSION_16)
    public static LongStream mapMulti(LongStream stream, LongMapMultiConsumer mapper) {
        Objects.requireNonNull(mapper);
        return stream.flatMap(mapper::of);
    }

    @FunctionalInterface
    @Stub(value = JavaVersion.VERSION_16, desc = "Ljava/util/stream/LongStream$LongMapMultiConsumer;")
    public interface LongMapMultiConsumer {

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
         * @param dc a {@code DoubleConsumer} accepting the mapped values
         */
        void accept(long value, LongConsumer dc);
    }
}
