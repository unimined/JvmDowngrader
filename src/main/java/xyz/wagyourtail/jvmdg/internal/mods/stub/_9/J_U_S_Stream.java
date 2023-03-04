package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntUnaryOperator;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class J_U_S_Stream {

    @Stub(value = JavaVersion.VERSION_1_9, include = TakeWhileStream.class)
    public static <T> Stream<T> takeWhile(Stream<T> stream, Predicate<T> predicate) {
        return new TakeWhileStream<>(stream.iterator(), predicate).stream();
    }

    @Stub(value = JavaVersion.VERSION_1_9)
    public static <T> Stream<T> dropWhile(Stream<T> stream, Predicate<T> predicate) {
        Iterator<T> iterator = stream.iterator();
        T next;
        if (!iterator.hasNext()) return Stream.empty();
        if (!predicate.test(next = iterator.next())) {
            next = iterator.next();
        } else {
            while (iterator.hasNext() && predicate.test(next = iterator.next())) {}
        }
        return Stream.concat(Stream.of(next),StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false));
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/stream/Stream;", include = {StreamIterator.class})
    public static <T> Stream<T> iterate(T seed, Predicate<T> hasNext, UnaryOperator<T> next) {
        return new StreamIterator<>(seed, hasNext, next).stream();
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/stream/Stream;")
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
        public T next() {
            var prev = next;
            hasNext = iterator.hasNext() && predicate.test(next = iterator.next());
            return prev;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        public Stream<T> stream() {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }

    }

    public static class StreamIterator<T> implements Iterator<T> {
        private T prev;
        private final Predicate<T> hasNext;
        private final UnaryOperator<T> computeNext;

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
            var next = prev;
            prev = computeNext.apply(prev);
            return next;
        }

        public Stream<T> stream() {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }
    }
}
