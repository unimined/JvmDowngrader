package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_IntConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_IntSupplier;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

@Stub(opcVers = 8, ref = @Ref("java/util/Optionalint"))
public class J_U_OptionalInt {

    private static final J_U_OptionalInt EMPTY = new J_U_OptionalInt();

    private final boolean isPresent;
    private final int value;

    private J_U_OptionalInt() {
        this.isPresent = false;
        this.value = 0;
    }

    private J_U_OptionalInt(int value) {
        this.isPresent = true;
        this.value = value;
    }

    public static J_U_OptionalInt empty() {
        return EMPTY;
    }

    public static J_U_OptionalInt of(int value) {
        return new J_U_OptionalInt(value);
    }

    public int getAsint() {
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

    public void ifPresent(J_U_F_IntConsumer action) {
        if (isPresent) {
            action.accept(value);
        }
    }

    public int orElse(int other) {
        return isPresent ? value : other;
    }

    public int orElseGet(J_U_F_IntSupplier supplier) {
        return isPresent ? value : supplier.getAsint();
    }

    public int orElseThrow() {
        if (!isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public <X extends Throwable> int orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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
        if (!(obj instanceof J_U_OptionalInt)) {
            return false;
        }
        J_U_OptionalInt other = (J_U_OptionalInt) obj;
        return (isPresent && other.isPresent
            ? Integer.compare(value, other.value) == 0
            : isPresent == other.isPresent);
    }

    @Override
    public int hashCode() {
        return isPresent ? Integer.hashCode(value) : 0;
    }

    @Override
    public String toString() {
        return isPresent ? String.format("OptionalInt[%s]", value) : "OptionalInt.empty";
    }
}
