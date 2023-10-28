package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_Consumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_DoubleConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_IntConsumer;
import xyz.wagyourtail.jvmdg.j8.stub.function.J_U_F_LongConsumer;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Comparator;

@Stub(ref = @Ref("java/util/Spliterator"))
public interface J_U_Spliterator<T> {

    int ORDERED = 0x00000010;
    int DISTINCT = 0x00000001;
    int SORTED = 0x00000004;
    int SIZED = 0x00000040;
    int NONNULL = 0x00000100;
    int IMMUTABLE = 0x00000400;
    int CONCURRENT = 0x00001000;
    int SUBSIZED = 0x00004000;


    boolean tryAdvance(J_U_F_Consumer<? super T> action);

    void forEachRemaining(J_U_F_Consumer<? super T> action);

    J_U_Spliterator<T> trySplit();

    long estimateSize();

    long getExactSizeIfKnown();

    int characteristics();

    boolean hasCharacteristics(int characteristics);

    Comparator<? super T> getComparator();

    class SpliteratorDefaults {

        @Stub(ref = @Ref("java/util/Spliterator"), abstractDefault = true)
        public static <T> void forEachRemaining(J_U_Spliterator<T> spliterator, J_U_F_Consumer<? super T> action) {
            do { } while (spliterator.tryAdvance(action));
        }

        @Stub(ref = @Ref("java/util/Spliterator"), abstractDefault = true)
        public static long getExactSizeIfKnown(J_U_Spliterator<?> spliterator) {
            return (spliterator.characteristics() & J_U_Spliterator.SIZED) == 0 ? -1 : spliterator.estimateSize();
        }

        @Stub(ref = @Ref("java/util/Spliterator"), abstractDefault = true)
        public static boolean hasCharacteristics(J_U_Spliterator<?> spliterator, int characteristics) {
            return (spliterator.characteristics() & characteristics) == characteristics;
        }

        @Stub(ref = @Ref("java/util/Spliterator"), abstractDefault = true)
        public static Comparator<?> getComparator(J_U_Spliterator<?> spliterator) {
            if (!hasCharacteristics(spliterator, J_U_Spliterator.SORTED)) {
                throw new IllegalStateException();
            }
            return null;
        }


    }


    interface OfPrimitive<T, T_CONS, T_SPLITR extends J_U_Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends J_U_Spliterator<T> {

        @Override
        T_SPLITR trySplit();

        boolean tryAdvance(T_CONS action);

        void forEachRemaining(T_CONS action);

        class OfPrimitiveDefaults {

            @Stub(ref = @Ref("java/util/Spliterator$OfPrimitive"), abstractDefault = true)
            public static <T, T_CONS> void forEachRemaining(J_U_Spliterator.OfPrimitive<T, T_CONS, ?> spliterator, T_CONS action) {
                do { } while (spliterator.tryAdvance(action));
            }

        }

    }



    @Stub(ref = @Ref("java/util/Spliterator$OfInt"))
    interface OfInt extends OfPrimitive<Integer, J_U_F_IntConsumer, OfInt> {

        @Override
        OfInt trySplit();

        @Override
        boolean tryAdvance(J_U_F_IntConsumer action);

        @Override
        void forEachRemaining(J_U_F_IntConsumer action);

        @Override
        boolean tryAdvance(J_U_F_Consumer<? super Integer> action);

        @Override
        void forEachRemaining(J_U_F_Consumer<? super Integer> action);

        class OfIntDefaults {

            @Stub(ref = @Ref("java/util/Spliterator$OfInt"), abstractDefault = true)
            public static void forEachRemaining(J_U_Spliterator.OfInt spliterator, J_U_F_IntConsumer action) {
                do { } while (spliterator.tryAdvance(action));
            }

            @Stub(ref = @Ref("java/util/Spliterator$OfInt"), abstractDefault = true)
            public static boolean tryAdvance(J_U_Spliterator.OfInt spliterator, final J_U_F_Consumer<? super Integer> action) {
                if (action instanceof J_U_F_IntConsumer) {
                    return spliterator.tryAdvance((J_U_F_IntConsumer) action);
                } else {
                    java.util.Objects.requireNonNull(action);
                    return spliterator.tryAdvance(new J_U_F_IntConsumer.IntConsumerAdapter() {
                        @Override
                        public void accept(int value) {
                            action.accept(value);
                        }
                    });
                }
            }

            @Stub(ref = @Ref("java/util/Spliterator$OfInt"), abstractDefault = true)
            public static void forEachRemaining(J_U_Spliterator.OfInt spliterator, final J_U_F_Consumer<? super Integer> action) {
                if (action instanceof J_U_F_IntConsumer) {
                    forEachRemaining(spliterator, (J_U_F_IntConsumer) action);
                } else {
                    forEachRemaining(spliterator, new J_U_F_IntConsumer.IntConsumerAdapter() {
                        @Override
                        public void accept(int value) {
                            action.accept(value);
                        }
                    });
                }
            }

        }

    }

    @Stub(ref = @Ref("java/util/Spliterator$OfLong"))
    interface OfLong extends OfPrimitive<Long, J_U_F_LongConsumer, OfLong> {

        @Override
        OfLong trySplit();

        @Override
        boolean tryAdvance(J_U_F_LongConsumer action);

        @Override
        void forEachRemaining(J_U_F_LongConsumer action);

        @Override
        boolean tryAdvance(J_U_F_Consumer<? super Long> action);

        @Override
        void forEachRemaining(J_U_F_Consumer<? super Long> action);

        class OfLongDefaults {

            @Stub(ref = @Ref("java/util/Spliterator$OfLong"), abstractDefault = true)
            public static void forEachRemaining(J_U_Spliterator.OfLong spliterator, J_U_F_LongConsumer action) {
                do { } while (spliterator.tryAdvance(action));
            }

            @Stub(ref = @Ref("java/util/Spliterator$OfLong"), abstractDefault = true)
            public static boolean tryAdvance(J_U_Spliterator.OfLong spliterator, final J_U_F_Consumer<? super Long> action) {
                if (action instanceof J_U_F_LongConsumer) {
                    return spliterator.tryAdvance((J_U_F_LongConsumer) action);
                } else {
                    java.util.Objects.requireNonNull(action);
                    return spliterator.tryAdvance(new J_U_F_LongConsumer.LongConsumerAdapter() {
                        @Override
                        public void accept(long value) {
                            action.accept(value);
                        }
                    });
                }
            }

            @Stub(ref = @Ref("java/util/Spliterator$OfLong"), abstractDefault = true)
            public static void forEachRemaining(J_U_Spliterator.OfLong spliterator, final J_U_F_Consumer<? super Long> action) {
                if (action instanceof J_U_F_LongConsumer) {
                    forEachRemaining(spliterator, (J_U_F_LongConsumer) action);
                } else {
                    forEachRemaining(spliterator, new J_U_F_LongConsumer.LongConsumerAdapter() {
                        @Override
                        public void accept(long value) {
                            action.accept(value);
                        }
                    });
                }
            }
        }

    }

    @Stub(ref = @Ref("java/util/Spliterator$OfDouble"))
    interface OfDouble extends OfPrimitive<Double, J_U_F_DoubleConsumer, OfDouble> {

        @Override
        OfDouble trySplit();

        @Override
        boolean tryAdvance(J_U_F_DoubleConsumer action);

        @Override
        void forEachRemaining(J_U_F_DoubleConsumer action);

        @Override
        boolean tryAdvance(J_U_F_Consumer<? super Double> action);

        @Override
        void forEachRemaining(J_U_F_Consumer<? super Double> action);

        class OfDoubleDefaults {

            @Stub(ref = @Ref("java/util/Spliterator$OfDouble"), abstractDefault = true)
            public static void forEachRemaining(J_U_Spliterator.OfDouble spliterator, J_U_F_DoubleConsumer action) {
                do {
                } while (spliterator.tryAdvance(action));
            }

            @Stub(ref = @Ref("java/util/Spliterator$OfDouble"), abstractDefault = true)
            public static boolean tryAdvance(J_U_Spliterator.OfDouble spliterator, final J_U_F_Consumer<? super Double> action) {
                if (action instanceof J_U_F_DoubleConsumer) {
                    return spliterator.tryAdvance((J_U_F_DoubleConsumer) action);
                } else {
                    java.util.Objects.requireNonNull(action);
                    return spliterator.tryAdvance(new J_U_F_DoubleConsumer.DoubleConsumerAdapter() {
                        @Override
                        public void accept(double value) {
                            action.accept(value);
                        }
                    });
                }
            }

            @Stub(ref = @Ref("java/util/Spliterator$OfDouble"), abstractDefault = true)
            public static void forEachRemaining(J_U_Spliterator.OfDouble spliterator, final J_U_F_Consumer<? super Double> action) {
                if (action instanceof J_U_F_DoubleConsumer) {
                    forEachRemaining(spliterator, (J_U_F_DoubleConsumer) action);
                } else {
                    forEachRemaining(spliterator, new J_U_F_DoubleConsumer.DoubleConsumerAdapter() {
                        @Override
                        public void accept(double value) {
                            action.accept(value);
                        }
                    });
                }
            }
        }
    }

}
