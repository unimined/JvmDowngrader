package xyz.wagyourtail.jvmdg.j16.stub.java_base;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.*;
import java.util.stream.*;

public class J_U_S_Stream {

    @Stub
    public static <T, R> Stream<R> mapMulti(Stream<T> stream, BiConsumer<? super T, ? super Consumer<R>> mapper) {
        return stream.flatMap(new MapMultiConsumer<>(mapper));
    }

    @Stub
    public static <T> IntStream mapMultiToInt(Stream<T> stream, BiConsumer<? super T, ? super IntConsumer> mapper) {
        return stream.flatMapToInt(new MapMultiIntConsumer<>(mapper));
    }

    @Stub
    public static <T> LongStream mapMultiToLong(Stream<T> stream, BiConsumer<? super T, ? super LongConsumer> mapper) {
        return stream.flatMapToLong(new MapMultiLongConsumer<>(mapper));
    }

    @Stub
    public static <T> DoubleStream mapMultiToDouble(Stream<T> stream, BiConsumer<? super T, ? super DoubleConsumer> mapper) {
        return stream.flatMapToDouble(new MapMultiDoubleConsumer<>(mapper));
    }

    @Stub
    public static <T> List<T> toList(Stream<T> stream) {
        return (List<T>) Collections.unmodifiableList(new ArrayList<>(Arrays.asList(stream.toArray())));
    }

    public static class MapMultiConsumer<T, R> implements Function<T, Stream<R>> {
        private final BiConsumer<? super T, ? super Consumer<R>> mapper;

        public MapMultiConsumer(BiConsumer<? super T, ? super Consumer<R>> mapper) {
            this.mapper = mapper;
        }

        @Override
        public Stream<R> apply(T t) {
            Stream.Builder<R> builder = Stream.builder();
            mapper.accept(t, builder);
            return builder.build();
        }

    }

    public static class MapMultiIntConsumer<T> implements Function<T, IntStream> {
        private final BiConsumer<? super T, ? super IntConsumer> mapper;

        public MapMultiIntConsumer(BiConsumer<? super T, ? super IntConsumer> mapper) {
            this.mapper = mapper;
        }

        @Override
        public IntStream apply(T t) {
            IntStream.Builder builder = IntStream.builder();
            mapper.accept(t, builder);
            return builder.build();
        }

    }

    public static class MapMultiLongConsumer<T> implements Function<T, LongStream> {
        private final BiConsumer<? super T, ? super LongConsumer> mapper;

        public MapMultiLongConsumer(BiConsumer<? super T, ? super LongConsumer> mapper) {
            this.mapper = mapper;
        }

        @Override
        public LongStream apply(T t) {
            LongStream.Builder builder = LongStream.builder();
            mapper.accept(t, builder);
            return builder.build();
        }

    }

    public static class MapMultiDoubleConsumer<T> implements Function<T, DoubleStream> {
        private final BiConsumer<? super T, ? super DoubleConsumer> mapper;

        public MapMultiDoubleConsumer(BiConsumer<? super T, ? super DoubleConsumer> mapper) {
            this.mapper = mapper;
        }

        @Override
        public DoubleStream apply(T t) {
            DoubleStream.Builder builder = DoubleStream.builder();
            mapper.accept(t, builder);
            return builder.build();
        }

    }

}
