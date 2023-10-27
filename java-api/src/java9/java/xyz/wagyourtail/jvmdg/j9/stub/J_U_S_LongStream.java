package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.LongPredicate;
import java.util.function.IntUnaryOperator;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

public class J_U_S_LongStream {

    @Stub
    public static LongStream takeWhile(LongStream stream, LongPredicate predicate) {
        return new TakeWhileStream(stream.iterator(), predicate).stream();
    }

    @Stub
    public static LongStream dropWhile(LongStream stream, LongPredicate predicate) {
        PrimitiveIterator.OfLong iterator = stream.iterator();
        long next;
        if (!iterator.hasNext()) {
            return LongStream.empty();
        }
        if (!predicate.test(next = iterator.nextLong())) {
            next = iterator.nextLong();
        } else {
            while (iterator.hasNext() && predicate.test(next = iterator.nextLong())) {
            }
        }
        return LongStream.concat(LongStream.of(next),
            StreamSupport.longStream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false));
    }

    @Stub(ref = @Ref("Ljava/util/stream/LongStream;"))
    public static LongStream iterate(int seed, LongPredicate hasNext, IntUnaryOperator next) {
        return new LongIterator(seed, hasNext, next).stream();
    }

    public static class TakeWhileStream implements PrimitiveIterator.OfLong {
        private final OfLong iterator;
        private final LongPredicate predicate;

        private long next;
        private boolean hasNext;

        public TakeWhileStream(OfLong iterator, LongPredicate predicate) {
            this.iterator = iterator;
            this.predicate = predicate;
            nextLong();
        }

        @Override
        public long nextLong() {
            long prev = next;
            hasNext = iterator.hasNext() && predicate.test(next = iterator.nextLong());
            return prev;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        public LongStream stream() {
            return StreamSupport.longStream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }

    }

    public static class LongIterator implements PrimitiveIterator.OfLong {
        private final LongPredicate hasNext;
        private final IntUnaryOperator computeNext;
        private int prev;

        public LongIterator(int seed, LongPredicate hasNext, IntUnaryOperator computeNext) {
            this.prev = seed;
            this.hasNext = hasNext;
            this.computeNext = computeNext;
        }

        public boolean hasNext() {
            return hasNext.test(prev);
        }

        @Override
        public long nextLong() {
            long next = prev;
            prev = computeNext.applyAsInt(prev);
            return next;
        }

        public LongStream stream() {
            return StreamSupport.longStream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }

    }

}
