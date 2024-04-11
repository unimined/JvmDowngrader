package xyz.wagyourtail.jvmdg.j8.stub.stream;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BiConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_BinaryOperator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Function;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Supplier;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Adapter("java/util/stream/Collector")
public interface J_U_S_Collector<T, A, R> {

    J_U_F_Supplier<A> supplier();

    J_U_F_BiConsumer<A, T> accumulator();

    J_U_F_BinaryOperator<A> combiner();

    J_U_F_Function<A, R> finisher();

    Set<Characteristics> characteristics();


    @Adapter("java/util/stream/Collector$Characteristics")
    enum Characteristics {
        CONCURRENT,
        UNORDERED,
        IDENTITY_FINISH
    }

    class CollectorStatics {

        @Stub(ref = @Ref("java/util/stream/Collector"))
        public static <T, R> J_U_S_Collector<T, R, R> of(J_U_F_Supplier<R> supplier, J_U_F_BiConsumer<R, T> accumulator, J_U_F_BinaryOperator<R> combiner, Characteristics... characteristics) {
            Objects.requireNonNull(supplier);
            Objects.requireNonNull(accumulator);
            Objects.requireNonNull(combiner);
            Objects.requireNonNull(characteristics);
            Set<Characteristics> cs = characteristics.length > 0 ?
                    Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH, characteristics)) :
                    Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));
            return new J_U_S_Collectors.CollectorImpl<>(supplier, accumulator, combiner, cs);
        }

        @Stub(ref = @Ref("java/util/stream/Collector"))
        public static <T, A, R> J_U_S_Collector<T, A, R> of(J_U_F_Supplier<A> supplier, J_U_F_BiConsumer<A, T> accumulator, J_U_F_BinaryOperator<A> combiner, J_U_F_Function<A, R> finisher, Characteristics... characteristics) {
            Objects.requireNonNull(supplier);
            Objects.requireNonNull(accumulator);
            Objects.requireNonNull(combiner);
            Objects.requireNonNull(finisher);
            Objects.requireNonNull(characteristics);
            Set<Characteristics> cs = Collections.emptySet();
            if (characteristics.length > 0) {
                cs = EnumSet.noneOf(Characteristics.class);
                Collections.addAll(cs, characteristics);
                cs = Collections.unmodifiableSet(cs);
            }
            return new J_U_S_Collectors.CollectorImpl<>(supplier, accumulator, combiner, finisher, cs);
        }
    }

}
