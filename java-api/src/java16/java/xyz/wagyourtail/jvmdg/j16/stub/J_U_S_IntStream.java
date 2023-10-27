package xyz.wagyourtail.jvmdg.j16.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class J_U_S_IntStream {

    @Stub
    public static IntStream mapMulti(IntStream stream, IntMapMultiConsumer mapper) {
        if (mapper == null)
            throw new NullPointerException();
        return stream.flatMap(mapper::of);
    }

    @FunctionalInterface
    @Stub(ref = @Ref("Ljava/util/stream/IntStream$IntMapMultiConsumer;"))
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
