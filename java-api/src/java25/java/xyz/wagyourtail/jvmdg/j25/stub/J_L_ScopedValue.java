package xyz.wagyourtail.jvmdg.j25.stub;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.*;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

@Adapter("java/lang/ScopedValue")
public class J_L_ScopedValue<T> {
    // TODO: make this better
    private static final RandomGenerator KEY_GENERATOR = new Random();
    private final int hash = generateHash();

    private static synchronized int generateHash() {
        return KEY_GENERATOR.nextInt();
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Adapter("java/lang/ScopedValue$Carrier")
    public static class Carrier {
        private static final ThreadLocal<Carrier> CURRENT = ThreadLocal.withInitial(() -> new Carrier(Map.of()));
        private final Map<J_L_ScopedValue<?>, Object> values;

        private Carrier(Map<J_L_ScopedValue<?>, Object> values) {
            this.values = values;
        }

        public <T> Carrier where(J_L_ScopedValue<T> key, T value) {
            Set<Map.Entry<J_L_ScopedValue<?>, Object>> newValues = values.entrySet();
            newValues.add(Map.entry(key, value));
            return new Carrier(Map.ofEntries(newValues.toArray(Map.Entry[]::new)));
        }

        @SuppressWarnings("unchecked")
        public <T> T get(J_L_ScopedValue<T> key) {
            if (!values.containsKey(key)) {
                throw new NullPointerException("no mapping present");
            }
            return (T) values.get(key);
        }

        @SuppressWarnings("unchecked")
        public <T> T orElse(J_L_ScopedValue<T> key, T other) {
            if (!values.containsKey(key)) {
                return other;
            }
            return (T) values.get(key);
        }

        @SuppressWarnings("unchecked")
        public <T> T orElseThrow(J_L_ScopedValue<T> key, Supplier<Throwable> other) throws Throwable {
            if (!values.containsKey(key)) {
                throw other.get();
            }
            return (T) values.get(key);
        }

        public <R, X extends Throwable> R call(CallableOp<? extends R, X> op) throws X {
            var prev = CURRENT.get();
            CURRENT.set(this);
            try {
                return op.call();
            } finally {
                CURRENT.set(prev);
            }
        }

        public void run(Runnable runnable) {
            var prev = CURRENT.get();
            CURRENT.set(this);
            try {
                runnable.run();
            } finally {
                CURRENT.set(prev);
            }
        }
    }

    public static <T> J_L_ScopedValue<T> newInstance() {
        return new J_L_ScopedValue<>();
    }

    public T get() {
        return Carrier.CURRENT.get().get(this);
    }

    public T orElse(T other) {
        return Carrier.CURRENT.get().orElse(this, other);
    }

    public T orElseThrow(Supplier<Throwable> other) throws Throwable {
        return Carrier.CURRENT.get().orElseThrow(this, other);
    }

    public static <T> Carrier where(J_L_ScopedValue<T> key, T value) {
        return Carrier.CURRENT.get().where(key, value);
    }

    @FunctionalInterface
    @Adapter("java/lang/ScopedValue$CallableOp")
    public interface CallableOp<T, X extends Throwable> {
        T call() throws X;
    }
}
