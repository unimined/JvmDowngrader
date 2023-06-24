package xyz.wagyourtail.jvmdg.j8.intl.spliterator;

import xyz.wagyourtail.jvmdg.j8.stub.J_U_PrimitiveIterator;
import xyz.wagyourtail.jvmdg.j8.stub.J_U_Spliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_DoubleConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;

import java.util.NoSuchElementException;

        public class DoubleIteratorFromSpliterator implements J_U_F_DoubleConsumer, J_U_PrimitiveIterator.OfDouble {
    private final J_U_Spliterator.OfDouble spliterator;

    private boolean nextAvailable = false;
    private double next;

    public DoubleIteratorFromSpliterator(J_U_Spliterator.OfDouble spliterator) {
        this.spliterator = spliterator;
    }

    @Override
    public double nextDouble() {
        if (!nextAvailable && !hasNext()) {
            throw new NoSuchElementException();
        }
        nextAvailable = false;
        return next;
    }

    @Override
    public void forEachRemaining(J_U_F_DoubleConsumer action) {
        J_U_PrimitiveIterator.OfDouble.OfDoubleDefaults.forEachRemaining(this, action);
    }

    @Override
    public Double next() {
        return nextDouble();
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
    public void accept(double value) {
        next = value;
        nextAvailable = true;
    }

    @Override
    public J_U_F_DoubleConsumer andThen(J_U_F_DoubleConsumer after) {
        return J_U_F_DoubleConsumer.DoubleConsumerDefaults.andThen(this, after);
    }
}
