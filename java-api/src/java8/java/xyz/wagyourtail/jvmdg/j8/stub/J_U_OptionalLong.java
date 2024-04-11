package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Supplier;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongSupplier;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.NoSuchElementException;

@Adapter("java/util/Optionallong")
public class J_U_OptionalLong {

    private static final J_U_OptionalLong EMPTY = new J_U_OptionalLong();

    private final boolean isPresent;
    private final long value;

    private J_U_OptionalLong() {
        this.isPresent = false;
        this.value = 0;
    }

    public static J_U_OptionalLong empty() {
        return EMPTY;
    }

    private J_U_OptionalLong(long value) {
        this.isPresent = true;
        this.value = value;
    }

    public static J_U_OptionalLong of(long value) {
        return new J_U_OptionalLong(value);
    }

    public long getAslong() {
        if (!isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public boolean isEmpty() {
        return !isPresent;
    }

    public void ifPresent(J_U_F_LongConsumer action) {
        if (isPresent) {
            action.accept(value);
        }
    }

    public long orElse(long other) {
        return isPresent ? value : other;
    }

    public long orElseGet(J_U_F_LongSupplier supplier) {
        return isPresent ? value : supplier.getAsLong();
    }

    public long orElseThrow() {
        if (!isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public<X extends Throwable> long orElseThrow(J_U_F_Supplier<? extends X> exceptionSupplier) throws X {
        if (isPresent) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof J_U_OptionalLong)) {
            return false;
        }
        J_U_OptionalLong other = (J_U_OptionalLong) obj;
        return (isPresent && other.isPresent
            ? Long.compare(value, other.value) == 0
            : isPresent == other.isPresent);
    }

    @Override
    public int hashCode() {
        return isPresent ? (int)(value ^ (value >>> 32)) : 0;
    }

    @Override
    public String toString() {
        return isPresent ? String.format("OptionalLong[%s]", value) : "OptionalLong.empty";
    }
}
