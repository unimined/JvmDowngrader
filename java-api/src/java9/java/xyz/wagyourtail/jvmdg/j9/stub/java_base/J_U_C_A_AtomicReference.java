package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import sun.misc.Unsafe;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.atomic.AtomicReference;

/**
 * these can't have the same memory promises as the real AtomicReference :(
 */
public class J_U_C_A_AtomicReference {
    private static final Unsafe unsafe = Utils.getUnsafe();

    @Stub
    public static <V> V compareAndExchange(AtomicReference<V> ref, V expected, V newValue) {
        V v;
        do {
            v = ref.get();
            if (v != expected) return v;
        } while (!ref.compareAndSet(expected, newValue));
        return expected;
    }

    @Stub
    public static <V> V compareAndExchangeAcquire(AtomicReference<V> ref, V expected, V newValue) {
        V v = compareAndExchange(ref, expected, newValue);
        unsafe.fullFence();
        return v;
    }

    @Stub
    public static <V> V compareAndExchangeRelease(AtomicReference<V> ref, V expected, V newValue) {
        unsafe.fullFence();
        return compareAndExchange(ref, expected, newValue);
    }

    @Stub
    public static <V> V getAcquire(AtomicReference<V> ref) {
        V v = ref.get();
        unsafe.fullFence();
        return v;
    }

    @Stub
    public static <V> V getOpaque(AtomicReference<V> ref) {
        return ref.get();
    }

    @Stub
    public static <V> V getPlain(AtomicReference<V> ref) {
        return ref.get();
    }

    @Stub
    public static <V> void setOpaque(AtomicReference<V> ref, V newValue) {
        ref.set(newValue);
    }

    @Stub
    public static <V> void setPlain(AtomicReference<V> ref, V newValue) {
        ref.set(newValue);
    }

    @Stub
    public static <V> void setRelease(AtomicReference<V> ref, V newValue) {
        unsafe.fullFence();
        ref.set(newValue);
    }

    @Stub
    public static <V> boolean weakCompareAndSetAcquire(AtomicReference<V> ref, V expected, V newValue) {
        boolean r = ref.weakCompareAndSet(expected, newValue);
        unsafe.fullFence();
        return r;
    }

    @Stub
    public static <V> boolean weakCompareAndSetPlain(AtomicReference<V> ref, V expected, V newValue) {
        return ref.weakCompareAndSet(expected, newValue);
    }

    @Stub
    public static <V> boolean weakCompareAndSetRelease(AtomicReference<V> ref, V expected, V newValue) {
        unsafe.fullFence();
        return ref.weakCompareAndSet(expected, newValue);
    }

    @Stub
    public static <V> boolean weakCompareAndSetVolatile(AtomicReference<V> ref, V expected, V newValue) {
        return ref.weakCompareAndSet(expected, newValue);
    }

}
