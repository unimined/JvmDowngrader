package xyz.wagyourtail.jvmdg.internal.mods.stub._10;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class J_U_S_Collectors {

    @Stub(value = JavaVersion.VERSION_1_10, desc = "Ljava/util/stream/Collectors;")
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        Collector<T, List<T>, List<T>> listCollector = (Collector) Collectors.toList();
        return Collector.of(
            listCollector.supplier(),
            listCollector.accumulator(),
            listCollector.combiner(),
            List::copyOf,
            listCollector.characteristics().toArray(new Collector.Characteristics[0])
        );
    }

    @Stub(value = JavaVersion.VERSION_1_10, desc = "Ljava/util/stream/Collectors;")
    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        Collector<T, Set<T>, Set<T>> setCollector = (Collector) Collectors.toSet();
        return Collector.of(
            setCollector.supplier(),
            setCollector.accumulator(),
            setCollector.combiner(),
            Set::copyOf,
            setCollector.characteristics().toArray(new Collector.Characteristics[0])
        );
    }

    @Stub(value = JavaVersion.VERSION_1_10, desc = "Ljava/util/stream/Collectors;")
    public static <T, K, U> Collector<T, ?, Map<K,U>> toUnmodifiableMap(Function<? super T, ? extends K> keyMapper,
        Function<? super T, ? extends U> valueMapper) {
        Collector<T, Map<K, U>, Map<K, U>> mapCollector = (Collector) Collectors.toMap(keyMapper, valueMapper);
        return Collector.of(
            mapCollector.supplier(),
            mapCollector.accumulator(),
            mapCollector.combiner(),
            Map::copyOf,
            mapCollector.characteristics().toArray(new Collector.Characteristics[0])
        );
    }

    @Stub(value = JavaVersion.VERSION_1_10, desc = "Ljava/util/stream/Collectors;")
    public static <T, K, U>
    Collector<T, ?, Map<K,U>> toUnmodifiableMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        Collector<T, Map<K, U>, Map<K, U>> mapCollector = (Collector) Collectors.toMap(keyMapper, valueMapper, mergeFunction);
        return Collector.of(
            mapCollector.supplier(),
            mapCollector.accumulator(),
            mapCollector.combiner(),
            Map::copyOf,
            mapCollector.characteristics().toArray(new Collector.Characteristics[0])
        );
    }


}
