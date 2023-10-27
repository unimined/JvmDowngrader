package xyz.wagyourtail.jvmdg.j8.stub.stream;

import xyz.wagyourtail.jvmdg.j8.stub.J_U_Optional;
import xyz.wagyourtail.jvmdg.j8.stub.function.*;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Comparator;

@Stub(ref = @Ref("java/util/stream/Stream"))
public interface J_U_S_Stream<T> extends J_U_S_BaseStream<T, J_U_S_Stream<T>> {

    J_U_S_Stream<T> filter(J_U_F_Predicate<? super T> predicate);

    <R> J_U_S_Stream<R> map(J_U_F_Function<? super T, ? extends R> mapper);

    J_U_S_IntStream mapToInt(J_U_F_ToIntFunction<? super T> mapper);

    J_U_S_LongStream mapToLong(J_U_F_ToLongFunction<? super T> mapper);

    J_U_S_DoubleStream mapToDouble(J_U_F_ToDoubleFunction<? super T> mapper);

    <R> J_U_S_Stream<R> flatMap(J_U_F_Function<? super T, ? extends J_U_S_Stream<? extends R>> mapper);

    J_U_S_IntStream flatMapToInt(J_U_F_Function<? super T, ? extends J_U_S_IntStream> mapper);

    J_U_S_LongStream flatMapToLong(J_U_F_Function<? super T, ? extends J_U_S_LongStream> mapper);

    J_U_S_DoubleStream flatMapToDouble(J_U_F_Function<? super T, ? extends J_U_S_DoubleStream> mapper);

    J_U_S_Stream<T> distinct();

    J_U_S_Stream<T> sorted();

    J_U_S_Stream<T> sorted(Comparator<? super T> comparator);

    J_U_S_Stream<T> peek(J_U_F_Consumer<? super T> action);

    J_U_S_Stream<T> limit(long maxSize);

    J_U_S_Stream<T> skip(long n);

    void forEach(J_U_F_Consumer<? super T> action);

    void forEachOrdered(J_U_F_Consumer<? super T> action);

    Object[] toArray();

    <A> A[] toArray(J_U_F_IntFunction<A[]> generator);

    T reduce(T identity, J_U_F_BinaryOperator<T> accumulator);

    J_U_Optional<T> reduce(J_U_F_BinaryOperator<T> accumulator);

    <U> U reduce(U identity, J_U_F_BiFunction<U, ? super T, U> accumulator, J_U_F_BinaryOperator<U> combiner);

    <R> R collect(J_U_F_Supplier<R> supplier, J_U_F_BiConsumer<R, ? super T> accumulator, J_U_F_BiConsumer<R, R> combiner);

    <R, A> R collect(J_U_S_Collector<? super T, A, R> collector);

    J_U_Optional<T> min(Comparator<? super T> comparator);

    J_U_Optional<T> max(Comparator<? super T> comparator);

    long count();

    boolean anyMatch(J_U_F_Predicate<? super T> predicate);

    boolean allMatch(J_U_F_Predicate<? super T> predicate);

    boolean noneMatch(J_U_F_Predicate<? super T> predicate);

    J_U_Optional<T> findFirst();

    J_U_Optional<T> findAny();

    class StreamStatics {

        @Stub(ref = @Ref("Ljava/util/stream/Stream;"))
        public static <T> Builder<T> builder() {
            //TODO
        }

        @Stub(ref = @Ref("Ljava/util/stream/Stream;"))
        public static <T> J_U_S_Stream<T> empty() {
            //TODO
        }

        @Stub(ref = @Ref("Ljava/util/stream/Stream;"))
        public static <T> J_U_S_Stream<T> of(T t) {
            //TODO
        }

        @Stub(ref = @Ref("Ljava/util/stream/Stream;"))
        public static <T> J_U_S_Stream<T> ofNullable(T t) {
            //TODO
        }

        @Stub(ref = @Ref("Ljava/util/stream/Stream;"))
        public static <T> J_U_S_Stream<T> of(T... values) {
            //TODO
        }

        @Stub(ref = @Ref("Ljava/util/stream/Stream;"))
        public static <T> J_U_S_Stream<T> iterate(final T seed, final J_U_F_UnaryOperator<T> f) {
            //TODO
        }

        @Stub(ref = @Ref("Ljava/util/stream/Stream;"))
        public static <T> J_U_S_Stream<T> iterate(final T seed, J_U_F_Predicate<? super T> hasNext, J_U_F_UnaryOperator<T> next) {
            //TODO
        }

        @Stub(ref = @Ref("Ljava/util/stream/Stream;"))
        public static <T> J_U_S_Stream<T> generate(J_U_F_Supplier<T> s) {
            //TODO
        }

        @Stub(ref = @Ref("Ljava/util/stream/Stream;"))
        public static <T> J_U_S_Stream<T> concat(J_U_S_Stream<? extends T> a, J_U_S_Stream<? extends T> b) {
            //TODO
        }



    }

    @Stub(ref = @Ref("Ljava/util/stream/Stream$Builder;"))
    interface Builder<T> extends J_U_F_Consumer<T> {

        @Override
        void accept(T t);

        Builder<T> add(T t);

        J_U_S_Stream<T> build();

        class BuilderDefaults {

            @Stub(abstractDefault = true)
            public static <T> Builder<T> add(Builder<T> builder, T t) {
                builder.accept(t);
                return builder;
            }

        }

    }


}
