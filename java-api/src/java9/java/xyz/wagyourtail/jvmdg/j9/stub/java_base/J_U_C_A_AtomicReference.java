package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.concurrent.atomic.AtomicReference;

/**
 * these can't have the same memory promises as the real AtomicReference :(
 */
public class J_U_C_A_AtomicReference {

    @Stub
    public static <V> V compareAndExchange(AtomicReference<V> ref, V expected, V newValue) {
        return ref.getAndUpdate(a -> {
            if (a == expected) {
                return newValue;
            } else {
                return a;
            }
        });
    }

    @Stub
    public static <V> V compareAndExchangeAcquire(AtomicReference<V> ref, V expected, V newValue) {
        return ref.getAndUpdate(a -> {
            if (a == expected) {
                return newValue;
            } else {
                return a;
            }
        });
    }

    @Stub
    public static <V> V compareAndExchangeRelease(AtomicReference<V> ref, V expected, V newValue) {
        return ref.getAndUpdate(a -> {
            if (a == expected) {
                return newValue;
            } else {
                return a;
            }
        });
    }

    @Stub
    public static <V> V getAcquire(AtomicReference<V> ref) {
        return ref.get();
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
        ref.set(newValue);
    }

    @Stub
    public static <V> boolean weakCompareAndSetAcquire(AtomicReference<V> ref, V expected, V newValue) {
        return ref.weakCompareAndSet(expected, newValue);
    }

    @Stub
    public static <V> boolean weakCompareAndSetPlain(AtomicReference<V> ref, V expected, V newValue) {
        return ref.weakCompareAndSet(expected, newValue);
    }

    @Stub
    public static <V> boolean weakCompareAndSetRelease(AtomicReference<V> ref, V expected, V newValue) {
        return ref.weakCompareAndSet(expected, newValue);
    }

    @Stub
    public static <V> boolean weakCompareAndSetVolatile(AtomicReference<V> ref, V expected, V newValue) {
        return ref.weakCompareAndSet(expected, newValue);
    }

}
