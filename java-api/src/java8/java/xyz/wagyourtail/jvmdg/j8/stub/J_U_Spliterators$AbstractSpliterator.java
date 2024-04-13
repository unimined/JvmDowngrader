package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.intl.spliterator.ArraySpliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Comparator;

@Stub(ref = @Ref(value = "java/util/Spliterators$AbstractSpliterator"))
public abstract class J_U_Spliterators$AbstractSpliterator<T> implements J_U_Spliterator<T> {
    static final int BATCH_UNIT = 1 << 10;
    static final int MAX_BATCH = 1 << 25;
    private final int characteristics;
    private long est;
    private int batch;

    protected J_U_Spliterators$AbstractSpliterator(long est, int additionalCharacteristics) {
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

    static final class Holder<T> implements J_U_F_Consumer<T> {
        T value;


        @Override
        public void accept(T t) {
            value = t;
        }
    }
}
