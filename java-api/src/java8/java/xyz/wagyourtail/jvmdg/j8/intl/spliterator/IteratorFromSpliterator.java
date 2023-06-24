package xyz.wagyourtail.jvmdg.j8.intl.spliterator;

import xyz.wagyourtail.jvmdg.j8.stub.J_U_Spliterator;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IteratorFromSpliterator<T> implements Iterator<T>, J_U_F_Consumer<T> {
    private final J_U_Spliterator<? extends T> spliterator;

    private boolean nextAvailable = false;
    private T next;

    public IteratorFromSpliterator(J_U_Spliterator<? extends T> spliterator) {
        this.spliterator = spliterator;
    }

    @Override
    public boolean hasNext() {
        if (!nextAvailable) {
            spliterator.tryAdvance(this);
        }
        return nextAvailable;
    }

    @Override
    public T next() {
        if (!nextAvailable && !hasNext()) {
            throw new NoSuchElementException();
        }
        nextAvailable = false;
        T t = next;
        next = null;
        return t;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public void accept(T t) {
        next = t;
        nextAvailable = true;
    }

    @Override
    public J_U_F_Consumer<T> andThen(J_U_F_Consumer<? super T> after) {
        return J_U_F_Consumer.ConsumerDefaults.andThen(this, after);
    }
}
