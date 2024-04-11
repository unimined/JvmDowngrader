package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_DoubleConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_IntConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Iterator;
import java.util.Objects;

@Adapter("Ljava/util/PrimitiveIterator;")
public interface J_U_PrimitiveIterator<T, T_CONS> extends Iterator<T> {

    void forEachRemaining(T_CONS action);

    @Adapter("Ljava/util/PrimitiveIterator$OfInt;")
    interface OfInt extends J_U_PrimitiveIterator<Integer, J_U_F_IntConsumer> {

        int nextInt();

        @Override
        void forEachRemaining(J_U_F_IntConsumer action);

        @Override
        Integer next();

        class OfIntDefaults {

            @Stub(abstractDefault = true)
            public static void forEachRemaining(J_U_PrimitiveIterator.OfInt it, J_U_F_Consumer<? super Integer> action) {
                if (action instanceof J_U_F_IntConsumer) {
                    forEachRemaining(it, (J_U_F_IntConsumer) action);
                } else {
                    Objects.requireNonNull(action);
                    while (it.hasNext())
                        action.accept(it.nextInt());
                }
            }

            @Stub(abstractDefault = true)
            public static void forEachRemaining(J_U_PrimitiveIterator.OfInt it, J_U_F_IntConsumer action) {
                it.forEachRemaining(action);
            }

            @Stub(abstractDefault = true)
            public static Integer next(J_U_PrimitiveIterator.OfInt it) {
                return it.nextInt();
            }

        }
    }

    @Adapter("Ljava/util/PrimitiveIterator$OfLong;")
    interface OfLong extends J_U_PrimitiveIterator<Long, J_U_F_LongConsumer> {

        long nextLong();

        @Override
        void forEachRemaining(J_U_F_LongConsumer action);

        @Override
        Long next();

        class OfLongDefaults {

            @Stub(ref = @Ref("Ljava/util/PrimitiveIterator$OfLong;"), abstractDefault = true)
            public static void forEachRemaining(J_U_PrimitiveIterator.OfLong it, J_U_F_Consumer<? super Long> action) {
                if (action instanceof J_U_F_LongConsumer) {
                    forEachRemaining(it, (J_U_F_LongConsumer) action);
                } else {
                    Objects.requireNonNull(action);
                    while (it.hasNext())
                        action.accept(it.nextLong());
                }
            }

            @Stub(ref = @Ref("Ljava/util/PrimitiveIterator$OfLong;"), abstractDefault = true)
            public static void forEachRemaining(J_U_PrimitiveIterator.OfLong it, J_U_F_LongConsumer action) {
                it.forEachRemaining(action);
            }

            @Stub(ref = @Ref("Ljava/util/PrimitiveIterator$OfLong;"), abstractDefault = true)
            public static Long next(J_U_PrimitiveIterator.OfLong it) {
                return it.nextLong();
            }

        }
    }

    @Adapter("Ljava/util/PrimitiveIterator$OfDouble;")
    interface OfDouble extends J_U_PrimitiveIterator<Double, J_U_F_DoubleConsumer> {

        double nextDouble();

        @Override
        void forEachRemaining(J_U_F_DoubleConsumer action);

        @Override
        Double next();

        class OfDoubleDefaults {

            @Stub(ref = @Ref("Ljava/util/PrimitiveIterator$OfDouble;"), abstractDefault = true)
            public static void forEachRemaining(J_U_PrimitiveIterator.OfDouble it, J_U_F_Consumer<? super Double> action) {
                if (action instanceof J_U_F_DoubleConsumer) {
                    forEachRemaining(it, (J_U_F_DoubleConsumer) action);
                } else {
                    Objects.requireNonNull(action);
                    while (it.hasNext())
                        action.accept(it.nextDouble());
                }
            }

            @Stub(ref = @Ref("Ljava/util/PrimitiveIterator$OfDouble;"), abstractDefault = true)
            public static void forEachRemaining(J_U_PrimitiveIterator.OfDouble it, J_U_F_DoubleConsumer action) {
                it.forEachRemaining(action);
            }

            @Stub(ref = @Ref("Ljava/util/PrimitiveIterator$OfDouble;"), abstractDefault = true)
            public static Double next(J_U_PrimitiveIterator.OfDouble it) {
                return it.nextDouble();
            }

        }
    }

}
