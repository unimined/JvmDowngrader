package xyz.wagyourtail.jvmdg.util;

public abstract class Lazy<T> {

    T value;
    volatile boolean initialized = false;

    protected abstract T init();

    public T get() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    value = init();
                    initialized = true;
                }
            }
        }
        return value;
    }

}
