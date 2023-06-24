package xyz.wagyourtail.jvmdg.j8.intl.spliterator;

import xyz.wagyourtail.jvmdg.j8.stub.J_U_PrimitiveIterator;
import xyz.wagyourtail.jvmdg.j8.stub.J_U_Spliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_IntConsumer;

import java.util.NoSuchElementException;

public class IntIteratorFromSpliterator implements J_U_F_IntConsumer, J_U_PrimitiveIterator.OfInt {
    private final J_U_Spliterator.OfInt spliterator;

    private boolean nextAvailable = false;
    private int next;

    public IntIteratorFromSpliterator(J_U_Spliterator.OfInt spliterator) {
        this.spliterator = spliterator;
    }

    @Override
    public int nextInt() {
        if (!nextAvailable && !hasNext()) {
            throw new NoSuchElementException();
        }
        nextAvailable = false;
        return next;
    }

    @Override
    public void forEachRemaining(J_U_F_IntConsumer action) {
        J_U_PrimitiveIterator.OfInt.OfIntDefaults.forEachRemaining(this, action);
    }

    @Override
    public Integer next() {
        return nextInt();
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
    public void accept(int value) {
        next = value;
        nextAvailable = true;
    }

    @Override
    public J_U_F_IntConsumer andThen(J_U_F_IntConsumer after) {
        return J_U_F_IntConsumer.IntConsumerDefaults.andThen(this, after);
    }
}
