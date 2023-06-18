package xyz.wagyourtail.jvmdg.j16.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.stream.DoubleStream;

public class J_U_S_DoubleStream {

    @Stub(opcVers = Opcodes.V16)
    public static DoubleStream mapMulti(DoubleStream stream, DoubleMapMultiConsumer mapper) {
        Objects.requireNonNull(mapper);
        return stream.flatMap(mapper::of);
    }

    @FunctionalInterface
    @Stub(opcVers = Opcodes.V16, ref = @Ref("Ljava/util/stream/DoubleStream$DoubleMapMultiConsumer;"))
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
