package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.j8.intl.spliterator.*;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_DoubleConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_IntConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

@Adapter("Ljava/util/Spliterators;")
public class J_U_Spliterators {

    private static final J_U_Spliterator<Object> EMPTY_SPLITERATOR = new EmptySpliterator.OfRef<>();
    private static final J_U_Spliterator.OfInt EMPTY_INT_SPLITERATOR = new EmptySpliterator.OfInt();
    private static final J_U_Spliterator.OfLong EMPTY_LONG_SPLITERATOR = new EmptySpliterator.OfLong();
    private static final J_U_Spliterator.OfDouble EMPTY_DOUBLE_SPLITERATOR = new EmptySpliterator.OfDouble();

    private J_U_Spliterators() {
    }

    public static <T> J_U_Spliterator<T> emptySpliterator() {
        return (J_U_Spliterator<T>) EMPTY_SPLITERATOR;
    }

    public static J_U_Spliterator.OfInt emptyIntSpliterator() {
        return EMPTY_INT_SPLITERATOR;
    }

    public static J_U_Spliterator.OfLong emptyLongSpliterator() {
        return EMPTY_LONG_SPLITERATOR;
    }

    public static J_U_Spliterator.OfDouble emptyDoubleSpliterator() {
        return EMPTY_DOUBLE_SPLITERATOR;
    }

    public static <T> J_U_Spliterator<T> spliterator(T[] array, int additionalCharacteristics) {
        return new ArraySpliterator<>(Objects.requireNonNull(array), additionalCharacteristics);
    }

    public static <T> J_U_Spliterator<T> spliterator(T[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        checkFromToBounds(Objects.requireNonNull(array).length, fromIndex, toIndex);
        return new ArraySpliterator<>(Objects.requireNonNull(array), fromIndex, toIndex, additionalCharacteristics);
    }

    public static J_U_Spliterator.OfInt spliterator(int[] array, int additionalCharacteristics) {
        return new IntArraySpliterator(Objects.requireNonNull(array), additionalCharacteristics);
    }

    public static J_U_Spliterator.OfInt spliterator(int[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        checkFromToBounds(Objects.requireNonNull(array).length, fromIndex, toIndex);
        return new IntArraySpliterator(Objects.requireNonNull(array), fromIndex, toIndex, additionalCharacteristics);
    }

    public static J_U_Spliterator.OfLong spliterator(long[] array, int additionalCharacteristics) {
        return new LongArraySpliterator(Objects.requireNonNull(array), additionalCharacteristics);
    }

    public static J_U_Spliterator.OfLong spliterator(long[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        checkFromToBounds(Objects.requireNonNull(array).length, fromIndex, toIndex);
        return new LongArraySpliterator(Objects.requireNonNull(array), fromIndex, toIndex, additionalCharacteristics);
    }

    public static J_U_Spliterator.OfDouble spliterator(double[] array, int additionalCharacteristics) {
        return new DoubleArraySpliterator(Objects.requireNonNull(array), additionalCharacteristics);
    }

    public static J_U_Spliterator.OfDouble spliterator(double[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        return new DoubleArraySpliterator(Objects.requireNonNull(array), fromIndex, toIndex, additionalCharacteristics);
    }

    private static void checkFromToBounds(int arrayLength, int origin, int fence) {
        if (origin > fence)
            throw new ArrayIndexOutOfBoundsException("origin(" + origin + ") > fence(" + fence + ")");
        if (origin < 0)
            throw new ArrayIndexOutOfBoundsException(origin);
        if (fence > arrayLength)
            throw new ArrayIndexOutOfBoundsException(fence);
    }

    public static <T> J_U_Spliterator<T> spliterator(Collection<? extends T> c, int characteristics) {
        return new IteratorSpliterator<>(Objects.requireNonNull(c), characteristics);
    }

    public static <T> J_U_Spliterator<T> spliteratorUnknownSize(Iterator<? extends T> iterator, int characteristics) {
        return new IteratorSpliterator<>(Objects.requireNonNull(iterator), characteristics);
    }

    public static J_U_Spliterator.OfInt spliterator(J_U_PrimitiveIterator.OfInt iterator, long size, int characteristics) {
        return new IntIteratorSpliterator(Objects.requireNonNull(iterator), size, characteristics);
    }

    public static J_U_Spliterator.OfInt spliterator(J_U_PrimitiveIterator.OfInt iterator, int characteristics) {
        return new IntIteratorSpliterator(Objects.requireNonNull(iterator), characteristics);
    }

    public static J_U_Spliterator.OfLong spliterator(J_U_PrimitiveIterator.OfLong iterator, long size, int characteristics) {
        return new LongIteratorSpliterator(Objects.requireNonNull(iterator), size, characteristics);
    }

    public static J_U_Spliterator.OfLong spliterator(J_U_PrimitiveIterator.OfLong iterator, int characteristics) {
        return new LongIteratorSpliterator(Objects.requireNonNull(iterator), characteristics);
    }

    public static J_U_Spliterator.OfDouble spliterator(J_U_PrimitiveIterator.OfDouble iterator, long size, int characteristics) {
        return new DoubleIteratorSpliterator(Objects.requireNonNull(iterator), size, characteristics);
    }

    public static J_U_Spliterator.OfDouble spliterator(J_U_PrimitiveIterator.OfDouble iterator, int characteristics) {
        return new DoubleIteratorSpliterator(Objects.requireNonNull(iterator), characteristics);
    }

    public static <T> Iterator<T> iterator(J_U_Spliterator<? extends T> spliterator) {
        Objects.requireNonNull(spliterator);
        return new IteratorFromSpliterator<>(spliterator);
    }

    public static J_U_PrimitiveIterator.OfInt iterator(J_U_Spliterator.OfInt spliterator) {
        Objects.requireNonNull(spliterator);
        return new IntIteratorFromSpliterator(spliterator);
    }

    public static J_U_PrimitiveIterator.OfLong iterator(J_U_Spliterator.OfLong spliterator) {
        Objects.requireNonNull(spliterator);
        return new LongIteratorFromSpliterator(spliterator);
    }

    public static J_U_PrimitiveIterator.OfDouble iterator(J_U_Spliterator.OfDouble spliterator) {
        Objects.requireNonNull(spliterator);
        return new DoubleIteratorFromSpliterator(spliterator);
    }

    @Adapter(value = "java/util/Spliterators$AbstractDoubleSpliterator")
    public abstract static class AbstractDoubleSpliterator implements J_U_Spliterator.OfDouble {
        static final int MAX_BATCH = AbstractSpliterator.MAX_BATCH;
        static final int BATCH_UNIT = AbstractSpliterator.BATCH_UNIT;
        private final int characteristics;
        private long est;
        private int batch;

        protected AbstractDoubleSpliterator(long est, int additionalCharacteristics) {
            this.est = est;
            if ((additionalCharacteristics & J_U_Spliterator.SIZED) != 0) {
                characteristics = additionalCharacteristics | J_U_Spliterator.SUBSIZED;
            } else {
                characteristics = additionalCharacteristics;
            }
        }

        @Override
        public OfDouble trySplit() {
            Holder holder = new Holder();

            long s = est;
            if (s > 1 && tryAdvance(holder)) {
                int n = batch + BATCH_UNIT;
                if (n > s) {
                    n = (int) s;
                }
                if (n > MAX_BATCH) {
                    n = MAX_BATCH;
                }
                double[] a = new double[n];
                int j = 0;
                do {
                    a[j] = holder.value;
                } while (++j < n && tryAdvance(holder));
                batch = j;
                if (est != Long.MAX_VALUE) {
                    est -= j;
                    return new DoubleArraySpliterator(a, 0, j, characteristics());
                }
                return new DoubleArraySpliterator(a, 0, j, characteristics(), Long.MAX_VALUE / 2);
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return est;
        }

        @Override
        public int characteristics() {
            return characteristics;
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

        @Override
        public void forEachRemaining(J_U_F_DoubleConsumer action) {
            while (tryAdvance(action)) {
            }
        }

        @Override
        public long getExactSizeIfKnown() {
            return (characteristics & SIZED) == 0 ? -1L : est;
        }

        @Override
        public boolean hasCharacteristics(int characteristics) {
            return (this.characteristics & characteristics) == characteristics;
        }

        @Override
        public Comparator<? super Double> getComparator() {
            throw new IllegalStateException();
        }


        static final class Holder extends J_U_F_DoubleConsumer.DoubleConsumerAdapter {
            double value;

            @Override
            public void accept(double t) {
                value = t;
            }

        }
    }

    @Adapter(value = "java/util/Spliterators$AbstractIntSpliterator")
    public abstract static class AbstractIntSpliterator implements J_U_Spliterator.OfInt {
        static final int MAX_BATCH = AbstractSpliterator.MAX_BATCH;
        static final int BATCH_UNIT = AbstractSpliterator.BATCH_UNIT;
        private final int characteristics;
        private long est;
        private int batch;

        protected AbstractIntSpliterator(long est, int additionalCharacteristics) {
            this.est = est;
            if ((additionalCharacteristics & J_U_Spliterator.SIZED) != 0) {
                characteristics = additionalCharacteristics | J_U_Spliterator.SUBSIZED;
            } else {
                characteristics = additionalCharacteristics;
            }
        }

        @Override
        public OfInt trySplit() {
            Holder holder = new Holder();

            long s = est;
            if (s > 1 && tryAdvance(holder)) {
                int n = batch + BATCH_UNIT;
                if (n > s) {
                    n = (int) s;
                }
                if (n > MAX_BATCH) {
                    n = MAX_BATCH;
                }
                int[] a = new int[n];
                int j = 0;
                do {
                    a[j] = holder.value;
                } while (++j < n && tryAdvance(holder));
                batch = j;
                if (est != Long.MAX_VALUE) {
                    est -= j;
                    return new IntArraySpliterator(a, 0, j, characteristics());
                }
                return new IntArraySpliterator(a, 0, j, characteristics(), Long.MAX_VALUE / 2);
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return est;
        }

        @Override
        public int characteristics() {
            return characteristics;
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

        @Override
        public void forEachRemaining(J_U_F_IntConsumer action) {
            while (tryAdvance(action)) {
            }
        }

        @Override
        public long getExactSizeIfKnown() {
            return (characteristics & SIZED) == 0 ? -1L : est;
        }

        @Override
        public boolean hasCharacteristics(int characteristics) {
            return (this.characteristics & characteristics) == characteristics;
        }

        @Override
        public Comparator<? super Integer> getComparator() {
            throw new IllegalStateException();
        }


        static final class Holder extends J_U_F_IntConsumer.IntConsumerAdapter {
            int value;

            @Override
            public void accept(int t) {
                value = t;
            }

        }
    }

    @Adapter(value = "java/util/Spliterators$AbstractLongSpliterator")
    public abstract static class AbstractLongSpliterator implements J_U_Spliterator.OfLong {
        static final int MAX_BATCH = AbstractSpliterator.MAX_BATCH;
        static final int BATCH_UNIT = AbstractSpliterator.BATCH_UNIT;
        private final int characteristics;
        private long est;
        private int batch;

        protected AbstractLongSpliterator(long est, int additionalCharacteristics) {
            this.est = est;
            if ((additionalCharacteristics & J_U_Spliterator.SIZED) != 0) {
                characteristics = additionalCharacteristics | J_U_Spliterator.SUBSIZED;
            } else {
                characteristics = additionalCharacteristics;
            }
        }

        @Override
        public OfLong trySplit() {
            Holder holder = new Holder();

            long s = est;
            if (s > 1 && tryAdvance(holder)) {
                int n = batch + BATCH_UNIT;
                if (n > s) {
                    n = (int) s;
                }
                if (n > MAX_BATCH) {
                    n = MAX_BATCH;
                }
                long[] a = new long[n];
                int j = 0;
                do {
                    a[j] = holder.value;
                } while (++j < n && tryAdvance(holder));
                batch = j;
                if (est != Long.MAX_VALUE) {
                    est -= j;
                    return new LongArraySpliterator(a, 0, j, characteristics());
                }
                return new LongArraySpliterator(a, 0, j, characteristics(), Long.MAX_VALUE / 2);
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return est;
        }

        @Override
        public int characteristics() {
            return characteristics;
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

        @Override
        public void forEachRemaining(J_U_F_LongConsumer action) {
            while (tryAdvance(action)) {
            }
        }

        @Override
        public long getExactSizeIfKnown() {
            return (characteristics & SIZED) == 0 ? -1L : est;
        }

        @Override
        public boolean hasCharacteristics(int characteristics) {
            return (this.characteristics & characteristics) == characteristics;
        }

        @Override
        public Comparator<? super Long> getComparator() {
            throw new IllegalStateException();
        }

        static final class Holder extends J_U_F_LongConsumer.LongConsumerAdapter {
            long value;


            @Override
            public void accept(long t) {
                value = t;
            }
        }
    }

    @Adapter(value = "java/util/Spliterators$AbstractSpliterator")
    public abstract static class AbstractSpliterator<T> implements J_U_Spliterator<T> {
        static final int BATCH_UNIT = 1 << 10;
        static final int MAX_BATCH = 1 << 25;
        private final int characteristics;
        private long est;
        private int batch;

        protected AbstractSpliterator(long est, int additionalCharacteristics) {
            this.est = est;
            if ((additionalCharacteristics & J_U_Spliterator.SIZED) != 0) {
                characteristics = additionalCharacteristics | J_U_Spliterator.SUBSIZED;
            } else {
                characteristics = additionalCharacteristics;
            }
        }

        @Override
        public J_U_Spliterator<T> trySplit() {
            Holder<T> holder = new Holder<>();

            long s = est;
            if (s > 1 && tryAdvance(holder)) {
                int n = batch + BATCH_UNIT;
                if (n > s) {
                    n = (int) s;
                }
                if (n > MAX_BATCH) {
                    n = MAX_BATCH;
                }
                T[] a = (T[]) new Object[n];
                int j = 0;
                do {
                    a[j] = holder.value;
                } while (++j < n && tryAdvance(holder));
                batch = j;
                if (est != Long.MAX_VALUE) {
                    est -= j;
                    return new ArraySpliterator<>(a, 0, j, characteristics());
                }
                return new ArraySpliterator<>(a, 0, j, characteristics(), Long.MAX_VALUE / 2);
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return est;
        }

        @Override
        public int characteristics() {
            return characteristics;
        }

        @Override
        public void forEachRemaining(J_U_F_Consumer<? super T> action) {
            while (tryAdvance(action)) {
            }
        }

        @Override
        public long getExactSizeIfKnown() {
            return (characteristics & SIZED) == 0 ? -1L : est;
        }

        @Override
        public boolean hasCharacteristics(int characteristics) {
            return (this.characteristics & characteristics) == characteristics;
        }

        @Override
        public Comparator<? super T> getComparator() {
            throw new IllegalStateException();
        }

        static final class Holder<T> extends J_U_F_Consumer.ConsumerAdapter<T> {
            T value;

            @Override
            public void accept(T t) {
                value = t;
            }

        }
    }
}
