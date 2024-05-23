package xyz.wagyourtail.jvmdg.j8.intl.spliterator;

import xyz.wagyourtail.jvmdg.j8.stub.J_U_PrimitiveIterator;
import xyz.wagyourtail.jvmdg.j8.stub.J_U_Spliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;

import java.util.NoSuchElementException;

public class LongIteratorFromSpliterator implements J_U_F_LongConsumer, J_U_PrimitiveIterator.OfLong {
    private final J_U_Spliterator.OfLong spliterator;

    private boolean nextAvailable = false;
    private long next;

    public LongIteratorFromSpliterator(J_U_Spliterator.OfLong spliterator) {
        this.spliterator = spliterator;
    }

    @Override
    public long nextLong() {
        if (!nextAvailable && !hasNext()) {
            throw new NoSuchElementException();
        }
        nextAvailable = false;
        return next;
    }

    @Override
    public void forEachRemaining(J_U_F_LongConsumer action) {
        J_U_PrimitiveIterator.OfLong.OfLongDefaults.forEachRemaining(this, action);
    }

    @Override
    public Long next() {
        return nextLong();
    }

    @Override
    public boolean hasNext() {
        if (!nextAvailable) {
            spliterator.tryAdvance(this);
        }
        return nextAvailable;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public void accept(long value) {
        next = value;
        nextAvailable = true;
    }

    @Override
    public J_U_F_LongConsumer andThen(J_U_F_LongConsumer after) {
        return J_U_F_LongConsumer.LongConsumerDefaults.andThen(this, after);
    }
}
