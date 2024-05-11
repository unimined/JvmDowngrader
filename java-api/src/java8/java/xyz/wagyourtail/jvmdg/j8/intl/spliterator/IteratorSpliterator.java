package xyz.wagyourtail.jvmdg.j8.intl.spliterator;

import xyz.wagyourtail.jvmdg.j8.stub.J_U_Iterator;
import xyz.wagyourtail.jvmdg.j8.stub.J_U_Spliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

public class IteratorSpliterator<T> implements J_U_Spliterator<T> {
    static final int BATCH_UNIT = 1 << 10;
    static final int MAX_BATCH = 1 << 25;
    private final Collection<? extends T> collection;
    private final int characteristics;
    private Iterator<? extends T> it;
    private long est;
    private int batch;

    public IteratorSpliterator(Collection<? extends T> collection, int characteristics) {
        this.collection = collection;
        this.it = null;
        if ((characteristics & J_U_Spliterator.CONCURRENT) == 0) {
            this.characteristics = characteristics | J_U_Spliterator.SIZED | J_U_Spliterator.SUBSIZED;
        } else {
            this.characteristics = characteristics;
        }
    }

    public IteratorSpliterator(Iterator<? extends T> it, long size, int characteristics) {
        this.collection = null;
        this.it = it;
        this.est = size;
        if ((characteristics & J_U_Spliterator.CONCURRENT) == 0) {
            this.characteristics = characteristics | J_U_Spliterator.SIZED | J_U_Spliterator.SUBSIZED;
        } else {
            this.characteristics = characteristics;
        }
    }

    public IteratorSpliterator(Iterator<? extends T> it, int characteristics) {
        this.collection = null;
        this.it = it;
        this.est = Long.MAX_VALUE;
        this.characteristics = characteristics & ~(J_U_Spliterator.SIZED | J_U_Spliterator.SUBSIZED);
    }


    @Override
    public boolean tryAdvance(J_U_F_Consumer<? super T> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        if (it == null) {
            it = collection.iterator();
            est = collection.size();
        }
        if (it.hasNext()) {
            action.accept(it.next());
            return true;
        }
        return false;
    }

    @Override
    public void forEachRemaining(J_U_F_Consumer<? super T> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        Iterator<? extends T> i;
        if ((i = it) == null) {
            i = it = collection.iterator();
            est = collection.size();
        }
        J_U_Iterator.forEachRemaining(i, action);
    }

    @Override
    public J_U_Spliterator<T> trySplit() {
        Iterator<? extends T> i;
        long s;
        if ((i = it) == null) {
            i = it = collection.iterator();
            s = est = collection.size();
        } else {
            s = est;
        }
        if (s > 1 && it.hasNext()) {
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
                a[j] = it.next();
            } while (++j < n && it.hasNext());
            batch = j;
            if (est != Long.MAX_VALUE) {
                est -= j;
                return new ArraySpliterator<>(a, 0, j, characteristics);
            }
            return new ArraySpliterator<>(a, 0, j, characteristics, Long.MAX_VALUE / 2);
        }
        return null;
    }

    @Override
    public long estimateSize() {
        if (it == null) {
            it = collection.iterator();
            est = collection.size();
        }
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
    public Comparator<? super T> getComparator() {
        if (hasCharacteristics(J_U_Spliterator.SORTED)) {
            return null;
        }
        throw new IllegalStateException();
    }
}
