package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Function;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Predicate;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Supplier;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Objects;

@Stub(ref = @Ref("Ljava/util/Optional;"))
public class J_U_Optional<T> {

    private static final J_U_Optional EMPTY = new J_U_Optional(null);

    private final T value;

    public static <T> J_U_Optional<T> empty() {
        return EMPTY;
    }

    private J_U_Optional(T value) {
        this.value = value;
    }

    public static <T> J_U_Optional<T> of(T value) {
        return new J_U_Optional<>(Objects.requireNonNull(value));
    }

    public static <T> J_U_Optional<T> ofNullable(T value) {
        return value == null ? EMPTY : new J_U_Optional<>(value);
    }

    public T get() {
        if (value == null) {
            throw new NullPointerException("No value present");
        }
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }

    public void ifPresent(J_U_F_Consumer<? super T> consumer) {
        if (value != null)
            consumer.accept(value);
    }

    public J_U_Optional<T> filter(J_U_F_Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (!isPresent())
            return this;
        else
            return predicate.test(value) ? this : empty();
    }

    public <U> J_U_Optional<U> map(J_U_F_Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent())
            return empty();
        else {
            return J_U_Optional.ofNullable(mapper.apply(value));
        }
    }

    public <U> J_U_Optional<U> flatMap(J_U_F_Function<? super T, J_U_Optional<U>> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent())
            return empty();
        else {
            return Objects.requireNonNull(mapper.apply(value));
        }
    }

    public T orElse(T other) {
        return value != null ? value : other;
    }

    public T orElseGet(J_U_F_Supplier<? extends T> other) {
        return value != null ? value : other.get();
    }

    public <X extends Throwable> T orElseThrow(J_U_F_Supplier<? extends X> exceptionSupplier) throws X {
        if (value != null) {
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
        if (!(obj instanceof J_U_Optional)) {
            return false;
        }
        J_U_Optional<?> other = (J_U_Optional<?>) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        if (value != null) {
            return String.format("Optional[%s]", value);
        } else {
            return "Optional.empty";
        }
    }
}
