package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.function.*;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.util.Comparator;

public class J_U_Arrays {

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(byte[] a) {
        parallelSort(a, 0, a.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(byte[] a, int fromIndex, int toIndex) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(char[] a) {
        parallelSort(a, 0, a.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(char[] a, int fromIndex, int toIndex) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(short[] a) {
        parallelSort(a, 0, a.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(short[] a, int fromIndex, int toIndex) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(int[] a) {
        parallelSort(a, 0, a.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(int[] a, int fromIndex, int toIndex) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(long[] a) {
        parallelSort(a, 0, a.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(long[] a, int fromIndex, int toIndex) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(float[] a) {
        parallelSort(a, 0, a.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(float[] a, int fromIndex, int toIndex) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(double[] a) {
        parallelSort(a, 0, a.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSort(double[] a, int fromIndex, int toIndex) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static <T> void parallelSort(T[] a) {
        parallelSort(a, 0, a.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static <T> void parallelSort(T[] a, int fromIndex, int toIndex) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static <T> void parallelSort(T[] a, Comparator<? super T> cmp) {
        parallelSort(a, 0, a.length, cmp);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static <T> void parallelSort(T[] a, int fromIndex, int toIndex, Comparator<? super T> cmp) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static <T> void parallelPrefix(T[] array, J_U_F_BinaryOperator<T> op) {
        parallelPrefix(array, 0, array.length, op);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static <T> void parallelPrefix(T[] array, int fromIndex, int toIndex, J_U_F_BinaryOperator<T> op) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelPrefix(long[] array, J_U_F_LongBinaryOperator op) {
        parallelPrefix(array, 0, array.length, op);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelPrefix(long[] array, int fromIndex, int toIndex, J_U_F_LongBinaryOperator op) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelPrefix(double[] array, J_U_F_DoubleBinaryOperator op) {
        parallelPrefix(array, 0, array.length, op);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelPrefix(double[] array, int fromIndex, int toIndex, J_U_F_DoubleBinaryOperator op) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void setAll(int[] array, J_U_F_IntUnaryOperator generator) {
        setAll(array, 0, array.length, generator);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void setAll(int[] array, int fromIndex, int toIndex, J_U_F_IntUnaryOperator generator) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static <T> void setAll(T[] array, J_U_F_IntFunction<? extends T> generator) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static <T> void parallelSetAll(T[] array, J_U_F_IntUnaryOperator generator) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void setAll(int[], J_U_F_IntUnaryOperator) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSetAll(int[] array, J_U_F_IntUnaryOperator generator) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void setAll(long[] array, J_U_F_IntToLongFunction generator) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSetAll(long[] array, J_U_F_IntToLongFunction generator) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void setAll(double[] array, J_U_F_IntToDoubleFunction generator) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static void parallelSetAll(double[] array, J_U_F_IntToDoubleFunction generator) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static <T> J_U_Spliterator<T> spliterator(T[] array) {
        return spliterator(array, 0, array.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static <T> J_U_Spliterator<T> spliterator(T[] array, int startInclusive, int endExclusive) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static J_U_Spliterator.OfInt spliterator(int[] array) {
        return spliterator(array, 0, array.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static J_U_Spliterator.OfInt spliterator(int[] array, int startInclusive, int endExclusive) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static J_U_Spliterator.OfLong spliterator(long[] array) {
        return spliterator(array, 0, array.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static J_U_Spliterator.OfLong spliterator(long[] array, int startInclusive, int endExclusive) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static J_U_Spliterator.OfDouble spliterator(double[] array) {
        return spliterator(array, 0, array.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static J_U_Spliterator.OfDouble spliterator(double[] array, int startInclusive, int endExclusive) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static <T> J_U_S_Stream<T> stream(T[] array) {
        return stream(array, 0, array.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static <T> J_U_S_Stream<T> stream(T[] array, int startInclusive, int endExclusive) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static J_U_S_IntStream stream(int[] array) {
        return stream(array, 0, array.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static J_U_S_IntStream stream(int[] array, int startInclusive, int endExclusive) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static J_U_S_LongStream stream(long[] array) {
        return stream(array, 0, array.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static J_U_S_LongStream stream(long[] array, int startInclusive, int endExclusive) {
        //TODO
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static J_U_S_DoubleStream stream(double[] array) {
        return stream(array, 0, array.length);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Arrays"))
    public static J_U_S_DoubleStream stream(double[] array, int startInclusive, int endExclusive) {
        //TODO
    }

}
