package xyz.wagyourtail.jvmdg.internal.mods.stub;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IteratorSupport<T> implements Iterator<T> {

    public final BooleanSupplier hasNext;
    public final Supplier<T> next;

    public IteratorSupport(BooleanSupplier hasNext, Supplier<T> next) {
        this.hasNext = hasNext;
        this.next = next;
    }

    @Override
    public boolean hasNext() {
        return hasNext.getAsBoolean();
    }

    @Override
    public T next() {
        return next.get();
    }

    public Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
    }

}
