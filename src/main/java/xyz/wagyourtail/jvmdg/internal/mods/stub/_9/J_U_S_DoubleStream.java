package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.StreamSupport;

public class J_U_S_DoubleStream {

    @Stub(value = JavaVersion.VERSION_1_9, include = TakeWhileStream.class)
    public static DoubleStream takeWhile(DoubleStream stream, DoublePredicate predicate) {
        return new TakeWhileStream(stream.iterator(), predicate).stream();
    }

    @Stub(value = JavaVersion.VERSION_1_9)
    public static DoubleStream dropWhile(DoubleStream stream, DoublePredicate predicate) {
        PrimitiveIterator.OfDouble iterator = stream.iterator();
        double next;
        if (!iterator.hasNext()) return DoubleStream.empty();
        if (!predicate.test(next = iterator.nextDouble())) {
            next = iterator.nextDouble();
        } else {
            while (iterator.hasNext() && predicate.test(next = iterator.nextDouble())) {}
        }
        return DoubleStream.concat(DoubleStream.of(next),StreamSupport.doubleStream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false));
    }

    @Stub(value = JavaVersion.VERSION_1_9, desc = "Ljava/util/stream/DoubleStream;", include = {DoubleIterator.class})
    public static DoubleStream iterate(double seed, DoublePredicate hasNext, DoubleUnaryOperator next) {
        return new DoubleIterator(seed, hasNext, next).stream();
    }

    public static class TakeWhileStream implements PrimitiveIterator.OfDouble {
        private final PrimitiveIterator.OfDouble iterator;
        private final DoublePredicate predicate;

        private double next;
        private boolean hasNext;

        public TakeWhileStream(PrimitiveIterator.OfDouble iterator, DoublePredicate predicate) {
            this.iterator = iterator;
            this.predicate = predicate;
            nextDouble();
        }

        @Override
        public double nextDouble() {
            var prev = next;
            hasNext = iterator.hasNext() && predicate.test(next = iterator.nextDouble());
            return prev;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        public DoubleStream stream() {
            return StreamSupport.doubleStream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }

    }

    public static class DoubleIterator implements PrimitiveIterator.OfDouble {
        private double prev;
        private final DoublePredicate hasNext;
        private final DoubleUnaryOperator computeNext;

        public DoubleIterator(double seed, DoublePredicate hasNext, DoubleUnaryOperator computeNext) {
            this.prev = seed;
            this.hasNext = hasNext;
            this.computeNext = computeNext;
        }

        public boolean hasNext() {
            return hasNext.test(prev);
        }

        @Override
        public double nextDouble() {
            var next = prev;
            prev = computeNext.applyAsDouble(prev);
            return next;
        }

        public DoubleStream stream() {
            return StreamSupport.doubleStream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }
    }
}
