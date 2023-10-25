package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Function;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_ToDoubleFunction;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_ToIntFunction;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_ToLongFunction;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class J_U_Comparator {

    @Stub(opcVers = Opcodes.V1_8)
    public static <T> Comparator<T> reversed(Comparator<T> self) {
        return Collections.reverseOrder(self);
    }

    @Stub(opcVers = Opcodes.V1_8)
    public static <T> Comparator<T> thenComparing(final Comparator<T> self, final Comparator<? super T> other) {
        Objects.requireNonNull(other);
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                int res = self.compare(o1, o2);
                return (res != 0) ? res : other.compare(o1, o2);
            }
        };
    }

    @Stub(opcVers = Opcodes.V1_8)
    public static <T> Comparator<T> thenComparing(final Comparator<T> self, final J_U_F_Function<? super T, ? extends T> keyExtractor, final Comparator<? super T> keyComparator) {
        Objects.requireNonNull(keyExtractor);
        Objects.requireNonNull(keyComparator);
        return thenComparing(self, comparing(keyExtractor, keyComparator));
    }

    @Stub(opcVers = Opcodes.V1_8)
    public static <T, U extends Comparable<? super U>> Comparator<T> comparing(final Comparator<T> self, final J_U_F_Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return thenComparing(self, comparing(keyExtractor));
    }

    @Stub(opcVers = Opcodes.V1_8)
    public static <T> Comparator<T> thenComparingInt(Comparator<T> self, J_U_F_ToIntFunction keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return thenComparing(self, comparingInt(keyExtractor));
    }

    @Stub(opcVers = Opcodes.V1_8)
    public static <T> Comparator<T> thenComparingLong(Comparator<T> self, J_U_F_ToLongFunction keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return thenComparing(self, comparingLong(keyExtractor));
    }

    @Stub(opcVers = Opcodes.V1_8)
    public static <T> Comparator<T> thenComparingDouble(Comparator<T> self, J_U_F_ToDoubleFunction keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return thenComparing(self, comparingDouble(keyExtractor));
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Comparator"))
    public static <T> Comparator<T> reverseOrder() {
        return Collections.reverseOrder();
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Comparator"))
    public static <T> Comparator<T> naturalOrder() {
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return ((Comparable<T>) o1).compareTo(o2);
            }
        };
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Comparator"))
    public static <T> Comparator<T> nullsFirst(final Comparator<T> comparator) {
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                if (o1 == null) {
                    return o2 == null ? 0 : -1;
                } else if (o2 == null) {
                    return 1;
                }
                return comparator.compare(o1, o2);
            }
        };
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Comparator"))
    public static <T> Comparator<T> nullsLast(final Comparator<T> comparator) {
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                if (o1 == null) {
                    return o2 == null ? 0 : 1;
                } else if (o2 == null) {
                    return -1;
                }
                return comparator.compare(o1, o2);
            }
        };
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Comparator"))
    public static <T, U> Comparator<T> comparing(final J_U_F_Function<? super T, ? extends U> keyExtractor, final Comparator<? super U> keyComparator) {
        Objects.requireNonNull(keyExtractor);
        Objects.requireNonNull(keyComparator);
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return keyComparator.compare(keyExtractor.apply(o1), keyExtractor.apply(o2));
            }
        };
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Comparator"))
    public static <T, U extends Comparable<? super U>> Comparator<T> comparing(final J_U_F_Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return keyExtractor.apply(o1).compareTo(keyExtractor.apply(o2));
            }
        };
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Comparator"))
    public static <T> Comparator<T> comparingInt(final J_U_F_ToIntFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return Integer.compare(keyExtractor.applyAsInt(o1), keyExtractor.applyAsInt(o2));
            }
        };
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Comparator"))
    public static <T> Comparator<T> comparingLong(final J_U_F_ToLongFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return Long.compare(keyExtractor.applyAsLong(o1), keyExtractor.applyAsLong(o2));
            }
        };
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/util/Comparator"))
    public static <T> Comparator<T> comparingDouble(final J_U_F_ToDoubleFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return Double.compare(keyExtractor.applyAsDouble(o1), keyExtractor.applyAsDouble(o2));
            }
        };
    }


}
