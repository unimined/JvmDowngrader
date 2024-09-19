package xyz.wagyourtail.jvmdg.j8.intl.spliterator;

import xyz.wagyourtail.jvmdg.j8.stub.J_U_Spliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_DoubleConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_IntConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;

import java.util.Comparator;
import java.util.Objects;

public abstract class EmptySpliterator<T, S extends J_U_Spliterator<T>, C> {

    EmptySpliterator() {
    }

    public S trySplit() {
        return null;
    }

    public boolean tryAdvance(C consumer) {
        Objects.requireNonNull(consumer);
        return false;
    }

    public void forEachRemaining(C consumer) {
        Objects.requireNonNull(consumer);
    }

    public long estimateSize() {
        return 0;
    }

    public int characteristics() {
        return J_U_Spliterator.SIZED | J_U_Spliterator.SUBSIZED;
    }

    public long getExactSizeIfKnown() {
        return estimateSize();
    }

    public boolean hasCharacteristics(int characteristics) {
        return (characteristics & (J_U_Spliterator.SIZED | J_U_Spliterator.SUBSIZED)) != 0;
    }

    public Comparator<? super T> getComparator() {
        throw new IllegalStateException();
    }

    public static final class OfRef<T> extends EmptySpliterator<T, J_U_Spliterator<T>, J_U_F_Consumer<? super T>> implements J_U_Spliterator<T> {

        public OfRef() {
        }

        @Override
        public void forEachRemaining(J_U_F_Consumer<? super T> action) {
            while (tryAdvance(action)) {
                // do nothing
            }
        }

    }

    public static final class OfInt extends EmptySpliterator<Integer, J_U_Spliterator.OfInt, J_U_F_IntConsumer> implements J_U_Spliterator.OfInt {

        public OfInt() {
        }


        @Override
        public boolean tryAdvance(final J_U_F_Consumer<? super Integer> action) {
            if (action instanceof J_U_F_IntConsumer) {
                return tryAdvance((J_U_F_IntConsumer) action);
            } else {
                return tryAdvance(new J_U_F_IntConsumer.IntConsumerAdapter() {
                    @Override
                    public void accept(int value) {
                        action.accept(value);
                    }
                });
            }
        }

        @Override
        public void forEachRemaining(final J_U_F_Consumer<? super Integer> action) {
            if (action instanceof J_U_F_IntConsumer) {
                forEachRemaining((J_U_F_IntConsumer) action);
            } else {
                forEachRemaining(new J_U_F_IntConsumer.IntConsumerAdapter() {
                    @Override
                    public void accept(int value) {
                        action.accept(value);
                    }
                });
            }
        }

    }

    public static final class OfLong extends EmptySpliterator<Long, J_U_Spliterator.OfLong, J_U_F_LongConsumer> implements J_U_Spliterator.OfLong {

        public OfLong() {
        }

        @Override
        public boolean tryAdvance(final J_U_F_Consumer<? super Long> action) {
            if (action instanceof J_U_F_LongConsumer) {
                return tryAdvance((J_U_F_LongConsumer) action);
            } else {
                return tryAdvance(new J_U_F_LongConsumer.LongConsumerAdapter() {
                    @Override
                    public void accept(long value) {
                        action.accept(value);
                    }
                });
            }
        }

        @Override
        public void forEachRemaining(final J_U_F_Consumer<? super Long> action) {
            if (action instanceof J_U_F_LongConsumer) {
                forEachRemaining((J_U_F_LongConsumer) action);
            } else {
                forEachRemaining(new J_U_F_LongConsumer.LongConsumerAdapter() {
                    @Override
                    public void accept(long value) {
                        action.accept(value);
                    }
                });
            }
        }

    }

    public static final class OfDouble extends EmptySpliterator<Double, J_U_Spliterator.OfDouble, J_U_F_DoubleConsumer> implements J_U_Spliterator.OfDouble {

        public OfDouble() {
        }

        @Override
        public boolean tryAdvance(final J_U_F_Consumer<? super Double> action) {
            if (action instanceof J_U_F_DoubleConsumer) {
                return tryAdvance((J_U_F_DoubleConsumer) action);
            } else {
                return tryAdvance(new J_U_F_DoubleConsumer.DoubleConsumerAdapter() {
                    @Override
                    public void accept(double value) {
                        action.accept(value);
                    }
                });
            }
        }

        @Override
        public void forEachRemaining(final J_U_F_Consumer<? super Double> action) {
            if (action instanceof J_U_F_DoubleConsumer) {
                forEachRemaining((J_U_F_DoubleConsumer) action);
            } else {
                forEachRemaining(new J_U_F_DoubleConsumer.DoubleConsumerAdapter() {
                    @Override
                    public void accept(double value) {
                        action.accept(value);
                    }
                });
            }
        }

    }

}
