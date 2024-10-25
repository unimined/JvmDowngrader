package xyz.wagyourtail.jvmdg.j8.intl.spliterator;

import xyz.wagyourtail.jvmdg.j8.stub.J_U_PrimitiveIterator;
import xyz.wagyourtail.jvmdg.j8.stub.J_U_Spliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_DoubleConsumer;

import java.util.Comparator;

public class DoubleIteratorSpliterator implements J_U_Spliterator.OfDouble {
    static final int BATCH_UNIT = 1 << 10;
    static final int MAX_BATCH = 1 << 25;
    private final int characteristics;
    private final J_U_PrimitiveIterator.OfDouble it;
    private long est;
    private int batch;

    public DoubleIteratorSpliterator(J_U_PrimitiveIterator.OfDouble it, long size, int characteristics) {
        this.it = it;
        this.est = size;
        if ((characteristics & J_U_Spliterator.CONCURRENT) == 0) {
            this.characteristics = characteristics | J_U_Spliterator.SIZED | J_U_Spliterator.SUBSIZED;
        } else {
            this.characteristics = characteristics;
        }
    }

    public DoubleIteratorSpliterator(J_U_PrimitiveIterator.OfDouble it, int characteristics) {
        this.it = it;
        this.est = Long.MAX_VALUE;
        this.characteristics = characteristics & ~(J_U_Spliterator.SIZED | J_U_Spliterator.SUBSIZED);
    }


    @Override
    public boolean tryAdvance(J_U_F_DoubleConsumer action) {
        if (action == null) {
            throw new NullPointerException();
        }
        if (it.hasNext()) {
            action.accept(it.nextDouble());
            return true;
        }
        return false;
    }

    @Override
    public void forEachRemaining(final J_U_F_Consumer<? super Double> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        it.forEachRemaining(new J_U_F_DoubleConsumer.DoubleConsumerAdapter() {
            @Override
            public void accept(double value) {
                action.accept(value);
            }
        });
    }

    @Override
    public void forEachRemaining(J_U_F_DoubleConsumer action) {
        if (action == null) {
            throw new NullPointerException();
        }
        it.forEachRemaining(action);
    }

    @Override
    public boolean tryAdvance(J_U_F_Consumer<? super Double> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        if (it.hasNext()) {
            action.accept(it.nextDouble());
            return true;
        }
        return false;
    }

    @Override
    public OfDouble trySplit() {
        long s = est;
        if (s > 1 && it.hasNext()) {
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
                a[j] = it.nextDouble();
            } while (++j < n && it.hasNext());
            batch = j;
            if (est != Double.MAX_VALUE) {
                est -= j;
                return new DoubleArraySpliterator(a, 0, j, characteristics);
            }
            return new DoubleArraySpliterator(a, 0, j, characteristics, Long.MAX_VALUE / 2);
        }
        return null;
    }

    @Override
    public long estimateSize() {
        return est;
    }

    @Override
    public long getExactSizeIfKnown() {
        if ((characteristics & SIZED) == 0) {
            return -1;
        } else {
            return estimateSize();
        }
    }

    @Override
    public int characteristics() {
        return characteristics;
    }

    @Override
    public boolean hasCharacteristics(int characteristics) {
        return (this.characteristics & characteristics) == characteristics;
    }

    @Override
    public Comparator<? super Double> getComparator() {
        if (hasCharacteristics(J_U_Spliterator.SORTED)) {
            return null;
        }
        throw new IllegalStateException();
    }

}
