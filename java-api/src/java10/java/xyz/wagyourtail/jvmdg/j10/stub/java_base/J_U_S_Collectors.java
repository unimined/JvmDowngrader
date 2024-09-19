package xyz.wagyourtail.jvmdg.j10.stub.java_base;


import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class J_U_S_Collectors {

    @Stub(ref = @Ref("Ljava/util/stream/Collectors;"))
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        Collector<T, List<T>, List<T>> listCollector = (Collector) Collectors.toList();
        return Collector.of(
            listCollector.supplier(),
            listCollector.accumulator(),
            listCollector.combiner(),
            J_U_List::copyOf,
            listCollector.characteristics().toArray(new Collector.Characteristics[0])
        );
    }

    @Stub(ref = @Ref("Ljava/util/stream/Collectors;"))
    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        Collector<T, Set<T>, Set<T>> setCollector = (Collector) Collectors.toSet();
        return Collector.of(
            setCollector.supplier(),
            setCollector.accumulator(),
            setCollector.combiner(),
            J_U_Set::copyOf,
            setCollector.characteristics().toArray(new Collector.Characteristics[0])
        );
    }

    @Stub(ref = @Ref("Ljava/util/stream/Collectors;"))
    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(
        Function<? super T, ? extends K> keyMapper,
        Function<? super T, ? extends U> valueMapper
    ) {
        Collector<T, Map<K, U>, Map<K, U>> mapCollector = (Collector) Collectors.toMap(keyMapper, valueMapper);
        return Collector.of(
            mapCollector.supplier(),
            mapCollector.accumulator(),
            mapCollector.combiner(),
            J_U_Map::copyOf,
            mapCollector.characteristics().toArray(new Collector.Characteristics[0])
        );
    }

    @Stub(ref = @Ref("Ljava/util/stream/Collectors;"))
    public static <T, K, U>
    Collector<T, ?, Map<K, U>> toUnmodifiableMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        Collector<T, Map<K, U>, Map<K, U>> mapCollector = (Collector) Collectors.toMap(
            keyMapper,
            valueMapper,
            mergeFunction
        );
        return Collector.of(
            mapCollector.supplier(),
            mapCollector.accumulator(),
            mapCollector.combiner(),
            J_U_Map::copyOf,
            mapCollector.characteristics().toArray(new Collector.Characteristics[0])
        );
    }


}
