package xyz.wagyourtail.jvmdg.j8.intl.spliterator;

import xyz.wagyourtail.jvmdg.j8.stub.J_U_Spliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;

import java.util.Comparator;

public class LongArraySpliterator implements J_U_Spliterator.OfLong {

    private final long[] array;
    private final int fence;
    private final int characteristics;
    private int index;
    private long estimatedSize;

    public LongArraySpliterator(long[] array, int additionalCharacteristics) {
        this(array, 0, array.length, additionalCharacteristics);
    }

    public LongArraySpliterator(long[] array, int origin, int fence, int additionalCharacteristics) {
        this.array = array;
        this.index = origin;
        this.fence = fence;
        this.characteristics = additionalCharacteristics | J_U_Spliterator.SIZED | J_U_Spliterator.SUBSIZED;
        this.estimatedSize = -1;
    }

    public LongArraySpliterator(long[] array, int origin, int fence, int additionalCharacteristics, long estimatedSize) {
        this.array = array;
        this.index = origin;
        this.fence = fence;
        this.characteristics = additionalCharacteristics | J_U_Spliterator.SIZED | J_U_Spliterator.SUBSIZED;
        this.estimatedSize = estimatedSize;
    }

    @Override
    public boolean tryAdvance(final J_U_F_Consumer<? super Long> action) {
        return tryAdvance(new J_U_F_LongConsumer.LongConsumerAdapter() {
            @Override
            public void accept(long value) {
                action.accept(value);
            }
        });
    }

    @Override
    public void forEachRemaining(final J_U_F_Consumer<? super Long> action) {
        forEachRemaining(new J_U_F_LongConsumer.LongConsumerAdapter() {
            @Override
            public void accept(long value) {
                action.accept(value);
            }
        });
    }

    @Override
    public long estimateSize() {
        if (estimatedSize < 0) {
            estimatedSize = (long) (fence - index);
        }
        return estimatedSize;
    }

    @Override
    public long getExactSizeIfKnown() {
        return estimateSize();
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
    public Comparator<? super Long> getComparator() {
        return null;
    }

    @Override
    public OfLong trySplit() {
        int lo = index, mid = (lo + fence) >>> 1;
        if (lo >= mid) return null;
        if (estimatedSize == -1) {
            return new LongArraySpliterator(array, lo, index = mid, characteristics);
        }
        long prefixEstimatedSize = estimatedSize >>> 1;
        estimatedSize -= prefixEstimatedSize;
        return new LongArraySpliterator(array, lo, index = mid, characteristics, prefixEstimatedSize);
    }

    @Override
    public boolean tryAdvance(J_U_F_LongConsumer action) {
        if (action == null) throw new NullPointerException();
        if (index >= 0 && index < fence) {
            action.accept(array[index++]);
            return true;
        }
        return false;
    }

    @Override
    public void forEachRemaining(J_U_F_LongConsumer action) {
        if (action == null) throw new NullPointerException();
        long[] a;
        int i, hi; // hoist accesses and checks from loop
        if ((a = array).length >= (hi = fence) && (i = index) >= 0 && i < (index = hi)) {
            do {
                action.accept(a[i]);
            } while (++i < hi);
        }
    }

}
