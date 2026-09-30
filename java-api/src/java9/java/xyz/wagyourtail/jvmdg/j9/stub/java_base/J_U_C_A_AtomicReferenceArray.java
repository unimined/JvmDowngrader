package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.atomic.AtomicReferenceArray;

public class J_U_C_A_AtomicReferenceArray {

    @Stub
    public static <T> T compareAndExchange(AtomicReferenceArray<T> ref, int index, T expected, T newValue) {
        T v;
        do {
            v = ref.get(index);
            if (v != expected) return v;
        } while (!ref.compareAndSet(index, expected, newValue));
        return expected;
    }

    @Stub
    public static <T> T compareAndExchangeAcquire(AtomicReferenceArray<T> ref, int index, T expected, T newValue) {
        return compareAndExchange(ref, index, expected, newValue);
    }

    @Stub
    public static <T> T compareAndExchangeRelease(AtomicReferenceArray<T> ref, int index, T expected, T newValue) {
        return compareAndExchange(ref, index, expected, newValue);
    }

    @Stub
    public static <T> T getAcquire(AtomicReferenceArray<T> ref, int index) {
        return ref.get(index);
    }

    @Stub
    public static <T> T getOpaque(AtomicReferenceArray<T> ref, int index) {
        return ref.get(index);
    }

    @Stub
    public static <T> T getPlain(AtomicReferenceArray<T> ref, int index) {
        return ref.get(index);
    }

    @Stub
    public static <T> void setOpaque(AtomicReferenceArray<T> ref, int index, T newValue) {
        ref.set(index, newValue);
    }

    @Stub
    public static <T> void setPlain(AtomicReferenceArray<T> ref, int index, T newValue) {
        ref.set(index, newValue);
    }

    @Stub
    public static <T> void setRelease(AtomicReferenceArray<T> ref, int index, T newValue) {
        ref.set(index, newValue);
    }

    @Stub
    public static <T> boolean weakCompareAndSetAcquire(AtomicReferenceArray<T> ref, int index, T expected, T newValue) {
        return ref.weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static <T> boolean weakCompareAndSetPlain(AtomicReferenceArray<T> ref, int index, T expected, T newValue) {
        return ref.weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static <T> boolean weakCompareAndSetRelease(AtomicReferenceArray<T> ref, int index, T expected, T newValue) {
        return ref.weakCompareAndSet(index, expected, newValue);
    }

    @Stub
    public static <T> boolean weakCompareAndSetVolatile(AtomicReferenceArray<T> ref, int index, T expected, T newValue) {
        return ref.compareAndSet(index, expected, newValue);
    }

}
