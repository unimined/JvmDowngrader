package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.intl.spliterator.*;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

@Stub(ref = @Ref("Ljava/util/Spliterators;"))
public class J_U_Spliterators {

    private static final J_U_Spliterator<Object> EMPTY_SPLITERATOR = new EmptySpliterator.OfRef<>();
    private static final J_U_Spliterator.OfInt EMPTY_INT_SPLITERATOR = new EmptySpliterator.OfInt();
    private static final J_U_Spliterator.OfLong EMPTY_LONG_SPLITERATOR = new EmptySpliterator.OfLong();
    private static final J_U_Spliterator.OfDouble EMPTY_DOUBLE_SPLITERATOR = new EmptySpliterator.OfDouble();

    private J_U_Spliterators() {
    }

    public static <T> J_U_Spliterator<T> emptySpliterator() {
        return (J_U_Spliterator<T>) EMPTY_SPLITERATOR;
    }

    public static J_U_Spliterator.OfInt emptyIntSpliterator() {
        return EMPTY_INT_SPLITERATOR;
    }

    public static J_U_Spliterator.OfLong emptyLongSpliterator() {
        return EMPTY_LONG_SPLITERATOR;
    }

    public static J_U_Spliterator.OfDouble emptyDoubleSpliterator() {
        return EMPTY_DOUBLE_SPLITERATOR;
    }

    public static <T> J_U_Spliterator<T> spliterator(T[] array, int additionalCharacteristics) {
        return new ArraySpliterator<>(Objects.requireNonNull(array), additionalCharacteristics);
    }

    public static <T> J_U_Spliterator<T> spliterator(T[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        checkFromToBounds(Objects.requireNonNull(array).length, fromIndex, toIndex);
        return new ArraySpliterator<>(Objects.requireNonNull(array), fromIndex, toIndex, additionalCharacteristics);
    }

    public static J_U_Spliterator.OfInt spliterator(int[] array, int additionalCharacteristics) {
        return new IntArraySpliterator(Objects.requireNonNull(array), additionalCharacteristics);
    }

    public static J_U_Spliterator.OfInt spliterator(int[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        checkFromToBounds(Objects.requireNonNull(array).length, fromIndex, toIndex);
        return new IntArraySpliterator(Objects.requireNonNull(array), fromIndex, toIndex, additionalCharacteristics);
    }

    public static J_U_Spliterator.OfLong spliterator(long[] array, int additionalCharacteristics) {
        return new LongArraySpliterator(Objects.requireNonNull(array), additionalCharacteristics);
    }

    public static J_U_Spliterator.OfLong spliterator(long[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        checkFromToBounds(Objects.requireNonNull(array).length, fromIndex, toIndex);
        return new LongArraySpliterator(Objects.requireNonNull(array), fromIndex, toIndex, additionalCharacteristics);
    }

    public static J_U_Spliterator.OfDouble spliterator(double[] array, int additionalCharacteristics) {
        return new DoubleArraySpliterator(Objects.requireNonNull(array), additionalCharacteristics);
    }

    public static J_U_Spliterator.OfDouble spliterator(double[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        return new DoubleArraySpliterator(Objects.requireNonNull(array), fromIndex, toIndex, additionalCharacteristics);
    }

    private static void checkFromToBounds(int arrayLength, int origin, int fence) {
        if (origin > fence)
            throw new ArrayIndexOutOfBoundsException("origin(" + origin + ") > fence(" + fence + ")");
        if (origin < 0)
            throw new ArrayIndexOutOfBoundsException(origin);
        if (fence > arrayLength)
            throw new ArrayIndexOutOfBoundsException(fence);
    }

    public static <T> J_U_Spliterator<T> spliterator(Collection<? extends T> c, int characteristics) {
        return new IteratorSpliterator<>(Objects.requireNonNull(c), characteristics);
    }

    public static <T> J_U_Spliterator<T> spliteratorUnknownSize(Iterator<? extends T> iterator, int characteristics) {
        return new IteratorSpliterator<>(Objects.requireNonNull(iterator), characteristics);
    }

    public static J_U_Spliterator.OfInt spliterator(J_U_PrimitiveIterator.OfInt iterator, long size, int characteristics) {
        return new IntIteratorSpliterator(Objects.requireNonNull(iterator), size, characteristics);
    }

    public static J_U_Spliterator.OfInt spliterator(J_U_PrimitiveIterator.OfInt iterator, int characteristics) {
        return new IntIteratorSpliterator(Objects.requireNonNull(iterator), characteristics);
    }

    public static J_U_Spliterator.OfLong spliterator(J_U_PrimitiveIterator.OfLong iterator, long size, int characteristics) {
        return new LongIteratorSpliterator(Objects.requireNonNull(iterator), size, characteristics);
    }

    public static J_U_Spliterator.OfLong spliterator(J_U_PrimitiveIterator.OfLong iterator, int characteristics) {
        return new LongIteratorSpliterator(Objects.requireNonNull(iterator), characteristics);
    }

    public static J_U_Spliterator.OfDouble spliterator(J_U_PrimitiveIterator.OfDouble iterator, long size, int characteristics) {
        return new DoubleIteratorSpliterator(Objects.requireNonNull(iterator), size, characteristics);
    }

    public static J_U_Spliterator.OfDouble spliterator(J_U_PrimitiveIterator.OfDouble iterator, int characteristics) {
        return new DoubleIteratorSpliterator(Objects.requireNonNull(iterator), characteristics);
    }

    public static <T> Iterator<T> iterator(J_U_Spliterator<? extends T> spliterator) {
        Objects.requireNonNull(spliterator);
        return new IteratorFromSpliterator<>(spliterator);
    }

    public static J_U_PrimitiveIterator.OfInt iterator(J_U_Spliterator.OfInt spliterator) {
        Objects.requireNonNull(spliterator);
        return new IntIteratorFromSpliterator(spliterator);
    }

    public static J_U_PrimitiveIterator.OfLong iterator(J_U_Spliterator.OfLong spliterator) {
        Objects.requireNonNull(spliterator);
        return new LongIteratorFromSpliterator(spliterator);
    }

    public static J_U_PrimitiveIterator.OfDouble iterator(J_U_Spliterator.OfDouble spliterator) {
        Objects.requireNonNull(spliterator);
        return new DoubleIteratorFromSpliterator(spliterator);
    }


}
