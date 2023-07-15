package xyz.wagyourtail.jvmdg.j8.stub.stream;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BiConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BinaryOperator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Function;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Supplier;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Set;
import java.util.stream.Collector;

@Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/stream/Collectors"))
public final class J_U_S_Collectors {

    private J_U_S_Collectors() {
    }




    class CollectorImpl<T, A, R> implements J_U_S_Collector<T, A, R> {

        private final J_U_F_Supplier<A> supplier;
        private final J_U_F_BiConsumer<A, T> accumulator;
        private final J_U_F_BinaryOperator<A> combiner;
        private final J_U_F_Function<A, R> finisher;
        private final Set<J_U_S_Collector.Characteristics> characteristics;

        public CollectorImpl(J_U_F_Supplier<A> supplier, J_U_F_BiConsumer<A, T> accumulator, J_U_F_BinaryOperator<A> combiner, J_U_F_Function<A, R> finisher, Set<J_U_S_Collector.Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }


        @Override
        public J_U_F_Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public J_U_F_BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        @Override
        public J_U_F_BinaryOperator<A> combiner() {
            return combiner;
        }

        @Override
        public J_U_F_Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return null;
        }
    }

}
