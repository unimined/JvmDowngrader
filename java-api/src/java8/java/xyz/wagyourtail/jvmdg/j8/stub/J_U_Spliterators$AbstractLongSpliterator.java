package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.longl.spliterator.LongArraySpliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Comparator;

@Stub(ref = @Ref(value = "java/util/Spliterators$AbstractLongSpliterator"))
public abstract class J_U_Spliterators$AbstractLongSpliterator implements J_U_Spliterator.OfLong {
    static final int MAX_BATCH = J_U_Spliterators$AbstractSpliterator.MAX_BATCH;
    static final int BATCH_UNIT = J_U_Spliterators$AbstractSpliterator.BATCH_UNIT;
    private final int characteristics;
    private long est;
    private int batch;

    protected J_U_Spliterators$AbstractLongSpliterator(long est, int additionalCharacteristics) {
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
            forEachRemaining(new J_U_F_LongConsumer() {
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

    static final class Holder implements J_U_F_LongConsumer {
        long value;


        @Override
        public void accept(long t) {
            value = t;
        }
    }
}
