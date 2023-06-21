package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_DoubleConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_IntConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Comparator;

@Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Spliterator"))
public interface J_U_Spliterator<T> {

    int ORDERED = 0x00000010;
    int DISTINCT = 0x00000001;
    int SORTED = 0x00000004;
    int SIZED = 0x00000040;
    int NONNULL = 0x00000100;
    int IMMUTABLE = 0x00000400;
    int CONCURRENT = 0x00001000;
    int SUBSIZED = 0x00004000;


    boolean tryAdvance(J_U_F_Consumer<? super T> action);

    void forEachRemaining(J_U_F_Consumer<? super T> action);

    J_U_Spliterator<T> trySplit();

    long estimateSize();

    long getExactSizeIfKnown();

    int characteristics();

    boolean hasCharacteristics(int characteristics);

    Comparator<? super T> getComparator();


    interface OfPrimitive<T, T_CONS, T_SPLITR extends J_U_Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends J_U_Spliterator<T> {

        @Override
        T_SPLITR trySplit();

        boolean tryAdvance(T_CONS action);

        void forEachRemaining(T_CONS action);

    }



    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Spliterator$OfInt"))
    interface OfInt extends OfPrimitive<Integer, J_U_F_IntConsumer, OfInt> {

        @Override
        OfInt trySplit();

        @Override
        boolean tryAdvance(J_U_F_IntConsumer action);

        @Override
        void forEachRemaining(J_U_F_IntConsumer action);

        @Override
        boolean tryAdvance(J_U_F_Consumer<? super Integer> action);

        @Override
        void forEachRemaining(J_U_F_Consumer<? super Integer> action);

    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Spliterator$OfLong"))
    interface OfLong extends OfPrimitive<Long, J_U_F_LongConsumer, OfLong> {

        @Override
        OfLong trySplit();

        @Override
        boolean tryAdvance(J_U_F_LongConsumer action);

        @Override
        void forEachRemaining(J_U_F_LongConsumer action);

        @Override
        boolean tryAdvance(J_U_F_Consumer<? super Long> action);

        @Override
        void forEachRemaining(J_U_F_Consumer<? super Long> action);

    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Spliterator$OfDouble"))
    interface OfDouble extends OfPrimitive<Double, J_U_F_DoubleConsumer, OfDouble> {

        @Override
        OfDouble trySplit();

        @Override
        boolean tryAdvance(J_U_F_DoubleConsumer action);

        @Override
        void forEachRemaining(J_U_F_DoubleConsumer action);

        @Override
        boolean tryAdvance(J_U_F_Consumer<? super Double> action);

        @Override
        void forEachRemaining(J_U_F_Consumer<? super Double> action);
    }

}
