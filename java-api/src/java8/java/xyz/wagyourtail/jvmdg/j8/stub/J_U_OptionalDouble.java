package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_DoubleConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_DoubleSupplier;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Supplier;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.NoSuchElementException;

@Adapter("java/util/OptionalDouble")
public class J_U_OptionalDouble {

    private static final J_U_OptionalDouble EMPTY = new J_U_OptionalDouble();

    private final boolean isPresent;
    private final double value;

    private J_U_OptionalDouble() {
        this.isPresent = false;
        this.value = Double.NaN;
    }

    private J_U_OptionalDouble(double value) {
        this.isPresent = true;
        this.value = value;
    }

    public static J_U_OptionalDouble empty() {
        return EMPTY;
    }

    public static J_U_OptionalDouble of(double value) {
        return new J_U_OptionalDouble(value);
    }

    public double getAsDouble() {
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

    public void ifPresent(J_U_F_DoubleConsumer action) {
        if (isPresent) {
            action.accept(value);
        }
    }

    public double orElse(double other) {
        return isPresent ? value : other;
    }

    public double orElseGet(J_U_F_DoubleSupplier supplier) {
        return isPresent ? value : supplier.getAsDouble();
    }

    public double orElseThrow() {
        if (!isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public <X extends Throwable> double orElseThrow(J_U_F_Supplier<? extends X> exceptionSupplier) throws X {
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
        if (!(obj instanceof J_U_OptionalDouble)) {
            return false;
        }
        J_U_OptionalDouble other = (J_U_OptionalDouble) obj;
        return (isPresent && other.isPresent
            ? Double.compare(value, other.value) == 0
            : isPresent == other.isPresent);
    }

    @Override
    public int hashCode() {
        return isPresent ? hashCode(value) : 0;
    }

    private int hashCode(double value) {
        long bits = Double.doubleToLongBits(value);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public String toString() {
        return isPresent ? String.format("OptionalDouble[%s]", value) : "OptionalDouble.empty";
    }

}
