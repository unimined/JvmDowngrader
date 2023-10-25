package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_DoubleConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_IntConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Iterator;
import java.util.Objects;

@Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/PrimitiveIterator;"))
public interface J_U_PrimitiveIterator<T, T_CONS> extends Iterator<T> {

    void forEachRemaining(T_CONS action);

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/PrimitiveIterator$OfInt;"))
    interface OfInt extends J_U_PrimitiveIterator<Integer, J_U_F_IntConsumer> {

        int nextInt();

        @Override
        default void forEachRemaining(J_U_F_IntConsumer action) {
            Objects.requireNonNull(action);
            while (hasNext())
                action.accept(nextInt());
        }

        @Override
        default Integer next() {
            return nextInt();
        }

        @Override
        default void forEachRemaining(J_U_F_Consumer<? super Integer> action) {
            if (action instanceof J_U_F_IntConsumer) {
                forEachRemaining((J_U_F_IntConsumer) action);
            } else {
                Objects.requireNonNull(action);
                while (hasNext())
                    action.accept(nextInt());
            }
        }
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/PrimitiveIterator$OfLong;"))
    interface OfLong extends J_U_PrimitiveIterator<Long, J_U_F_LongConsumer> {

        long nextLong();

        @Override
        default void forEachRemaining(J_U_F_LongConsumer action) {
            Objects.requireNonNull(action);
            while (hasNext())
                action.accept(nextLong());
        }

        @Override
        default Long next() {
            return nextLong();
        }

        @Override
        default void forEachRemaining(J_U_F_Consumer<? super Long> action) {
            if (action instanceof J_U_F_LongConsumer) {
                forEachRemaining((J_U_F_LongConsumer) action);
            } else {
                Objects.requireNonNull(action);
                while (hasNext())
                    action.accept(nextLong());
            }
        }
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("Ljava/util/PrimitiveIterator$OfDouble;"))
    interface OfDouble extends J_U_PrimitiveIterator<Double, J_U_F_DoubleConsumer> {

        double nextDouble();

        @Override
        default void forEachRemaining(J_U_F_DoubleConsumer action) {
            Objects.requireNonNull(action);
            while (hasNext())
                action.accept(nextDouble());
        }

        @Override
        default Double next() {
            return nextDouble();
        }

        @Override
        default void forEachRemaining(J_U_F_Consumer<? super Double> action) {
            if (action instanceof J_U_F_DoubleConsumer) {
                forEachRemaining((J_U_F_DoubleConsumer) action);
            } else {
                Objects.requireNonNull(action);
                while (hasNext())
                    action.accept(nextDouble());
            }
        }
    }

}
