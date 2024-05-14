package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.j8.intl.spliterator.IntArraySpliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_IntConsumer;

import java.util.Comparator;

@Adapter(value = "java/util/Spliterators$AbstractIntSpliterator")
public abstract class J_U_Spliterators$AbstractIntSpliterator implements J_U_Spliterator.OfInt {
    static final int MAX_BATCH = J_U_Spliterators$AbstractSpliterator.MAX_BATCH;
    static final int BATCH_UNIT = J_U_Spliterators$AbstractSpliterator.BATCH_UNIT;
    private final int characteristics;
    private long est;
    private int batch;

    protected J_U_Spliterators$AbstractIntSpliterator(long est, int additionalCharacteristics) {
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
