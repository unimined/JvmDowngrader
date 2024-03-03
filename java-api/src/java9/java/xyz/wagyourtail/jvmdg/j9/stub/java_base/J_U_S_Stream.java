package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class J_U_S_Stream {

    @Stub
    public static <T> Stream<T> takeWhile(Stream<T> stream, Predicate<T> predicate) {
        return new TakeWhileStream<>(stream.iterator(), predicate).stream();
    }

    @Stub
    public static <T> Stream<T> dropWhile(Stream<T> stream, Predicate<T> predicate) {
        Iterator<T> iterator = stream.iterator();
        T next;
        if (!iterator.hasNext()) {
            return Stream.empty();
        }
        if (!predicate.test(next = iterator.next())) {
            next = iterator.next();
        } else {
            while (iterator.hasNext() && predicate.test(next = iterator.next())) {
            }
        }
        return Stream.concat(Stream.of(next),
            StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false));
    }

    @Stub(ref = @Ref("Ljava/util/stream/Stream;"))
    public static <T> Stream<T> iterate(T seed, Predicate<T> hasNext, UnaryOperator<T> next) {
        return new StreamIterator<>(seed, hasNext, next).stream();
    }

    @Stub(ref = @Ref("Ljava/util/stream/Stream;"))
    public static <T> Stream<T> ofNullable(T t) {
        return t == null ? Stream.empty() : Stream.of(t);
    }

    public static class TakeWhileStream<T> implements Iterator<T> {
        private final Iterator<T> iterator;
        private final Predicate<T> predicate;

        private T next;
        private boolean hasNext;

        public TakeWhileStream(Iterator<T> iterator, Predicate<T> predicate) {
            this.iterator = iterator;
            this.predicate = predicate;
            next();
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public T next() {
            T prev = next;
            hasNext = iterator.hasNext() && predicate.test(next = iterator.next());
            return prev;
        }

        public Stream<T> stream() {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }

    }

    public static class StreamIterator<T> implements Iterator<T> {
        private final Predicate<T> hasNext;
        private final UnaryOperator<T> computeNext;
        private T prev;

        public StreamIterator(T seed, Predicate<T> hasNext, UnaryOperator<T> computeNext) {
            this.prev = seed;
            this.hasNext = hasNext;
            this.computeNext = computeNext;
        }

        public boolean hasNext() {
            return hasNext.test(prev);
        }

        @Override
        public T next() {
            T next = prev;
            prev = computeNext.apply(prev);
            return next;
        }

        public Stream<T> stream() {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }

    }

}
