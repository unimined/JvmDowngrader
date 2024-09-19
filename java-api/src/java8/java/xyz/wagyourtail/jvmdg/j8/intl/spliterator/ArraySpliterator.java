package xyz.wagyourtail.jvmdg.j8.intl.spliterator;

import xyz.wagyourtail.jvmdg.j8.stub.J_U_Spliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;

import java.util.Comparator;

public class ArraySpliterator<T> implements J_U_Spliterator<T> {

    private final T[] array;
    private final int fence;
    private final int characteristics;
    private int index;
    private long estimatedSize;

    public ArraySpliterator(T[] array, int additionalCharacteristics) {
        this(array, 0, array.length, additionalCharacteristics);
    }

    public ArraySpliterator(T[] array, int origin, int fence, int additionalCharacteristics) {
        this.array = array;
        this.index = origin;
        this.fence = fence;
        this.characteristics = additionalCharacteristics | J_U_Spliterator.SIZED | J_U_Spliterator.SUBSIZED;
        this.estimatedSize = -1;
    }

    public ArraySpliterator(T[] array, int origin, int fence, int additionalCharacteristics, long estimatedSize) {
        this.array = array;
        this.index = origin;
        this.fence = fence;
        this.characteristics = additionalCharacteristics | J_U_Spliterator.SIZED | J_U_Spliterator.SUBSIZED;
        this.estimatedSize = estimatedSize;
    }

    @Override
    public J_U_Spliterator<T> trySplit() {
        int lo = index, mid = (lo + fence) >>> 1;
        if (lo >= mid) {
            return null;
        }
        if (estimatedSize == -1) {
            return new ArraySpliterator<>(array, lo, index = mid, characteristics);
        }
        long prefixEstimate = estimatedSize >>> 1;
        estimatedSize -= prefixEstimate;
        return new ArraySpliterator<>(array, lo, index = mid, characteristics, prefixEstimate);
    }

    @Override
    public void forEachRemaining(J_U_F_Consumer<? super T> action) {
        T[] a;
        int i, hi;
        if (action == null) {
            throw new NullPointerException();
        }
        if ((a = array).length >= (hi = fence) && (i = index) >= 0 && i < (index = hi)) {
            do {
                action.accept(a[i]);
            } while (++i < hi);
        }
    }

    @Override
    public boolean tryAdvance(J_U_F_Consumer<? super T> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        if (index >= 0 && index < fence) {
            action.accept(array[index++]);
            return true;
        }
        return false;
    }

    @Override
    public long estimateSize() {
        if (estimatedSize < 0) {
            estimatedSize = fence - index;
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
        return (characteristics() & characteristics) == characteristics;
    }

    @Override
    public Comparator<? super T> getComparator() {
        return null;
    }

}
