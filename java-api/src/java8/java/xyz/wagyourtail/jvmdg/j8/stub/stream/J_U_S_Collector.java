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

@Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/stream/Collector"))
public interface J_U_S_Collector<T, A, R> {

    J_U_F_Supplier<A> supplier();

    J_U_F_BiConsumer<A, T> accumulator();

    J_U_F_BinaryOperator<A> combiner();

    J_U_F_Function<A, R> finisher();

    Set<Characteristics> characteristics();


    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/stream/Collector$Characteristics"))
    enum Characteristics {
        CONCURRENT,
        UNORDERED,
        IDENTITY_FINISH
    }

    class CollectorStatics {

        @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/stream/Collector"))
        public static <T, R> J_U_S_Collector<T, R, R> of(J_U_F_Supplier<R> supplier, J_U_F_BiConsumer<R, T> accumulator, J_U_F_BinaryOperator<R> combiner, Characteristics... characteristics) {
            //TODO
        }

        @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/stream/Collector"))
        public static <T, A, R> J_U_S_Collector<T, A, R> of(J_U_F_Supplier<A> supplier, J_U_F_BiConsumer<A, T> accumulator, J_U_F_BinaryOperator<A> combiner, Function<A, R> finisher, Characteristics... characteristics) {
            //TODO
        }
    }

}
