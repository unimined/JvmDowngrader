package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class J_U_S_IntStream {

    @Stub(javaVersion = Opcodes.V9, include = TakeWhileStream.class)
    public static IntStream takeWhile(IntStream stream, IntPredicate predicate) {
        return new TakeWhileStream(stream.iterator(), predicate).stream();
    }

    @Stub(javaVersion = Opcodes.V9)
    public static IntStream dropWhile(IntStream stream, IntPredicate predicate) {
        PrimitiveIterator.OfInt iterator = stream.iterator();
        int next;
        if (!iterator.hasNext()) {
            return IntStream.empty();
        }
        if (!predicate.test(next = iterator.nextInt())) {
            next = iterator.nextInt();
        } else {
            while (iterator.hasNext() && predicate.test(next = iterator.nextInt())) {
            }
        }
        return IntStream.concat(IntStream.of(next),
            StreamSupport.intStream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false));
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/util/stream/IntStream;"), include = {IntIterator.class})
    public static IntStream iterate(int seed, IntPredicate hasNext, IntUnaryOperator next) {
        return new IntIterator(seed, hasNext, next).stream();
    }

    public static class TakeWhileStream implements PrimitiveIterator.OfInt {
        private final OfInt iterator;
        private final IntPredicate predicate;

        private int next;
        private boolean hasNext;

        public TakeWhileStream(OfInt iterator, IntPredicate predicate) {
            this.iterator = iterator;
            this.predicate = predicate;
            nextInt();
        }

        @Override
        public int nextInt() {
            int prev = next;
            hasNext = iterator.hasNext() && predicate.test(next = iterator.nextInt());
            return prev;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        public IntStream stream() {
            return StreamSupport.intStream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }

    }

    public static class IntIterator implements PrimitiveIterator.OfInt {
        private final IntPredicate hasNext;
        private final IntUnaryOperator computeNext;
        private int prev;

        public IntIterator(int seed, IntPredicate hasNext, IntUnaryOperator computeNext) {
            this.prev = seed;
            this.hasNext = hasNext;
            this.computeNext = computeNext;
        }

        public boolean hasNext() {
            return hasNext.test(prev);
        }

        @Override
        public int nextInt() {
            int next = prev;
            prev = computeNext.applyAsInt(prev);
            return next;
        }

        public IntStream stream() {
            return StreamSupport.intStream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }

    }

}
