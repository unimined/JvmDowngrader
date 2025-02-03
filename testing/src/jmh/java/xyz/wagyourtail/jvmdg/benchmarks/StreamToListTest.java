package xyz.wagyourtail.jvmdg.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@State(Scope.Benchmark)
public class StreamToListTest {

    private static final Class<?> REF_PIPELINE;

    static {
        try {
            REF_PIPELINE = Class.forName("java.util.stream.ReferencePipeline");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public void testNewArrayList10(Blackhole bh) {
        Stream<Integer> s = IntStream.rangeClosed(0, 10).mapToObj(i -> i + 10);

        List<Integer> l = (List) Collections.unmodifiableList(new ArrayList<>(Arrays.asList(s.toArray())));

        bh.consume(l);
    }

    @Benchmark
    public void testCheckClass10(Blackhole bh) {
        Stream<Integer> s = IntStream.rangeClosed(0, 10).mapToObj(i -> i + 10);

        List<Integer> l;
        if (REF_PIPELINE.isAssignableFrom(s.getClass())) {
            l = (List) Collections.unmodifiableList(Arrays.asList(s.toArray()));
        } else {
            l = (List) Collections.unmodifiableList(new ArrayList<>(Arrays.asList(s.toArray())));
        }

        bh.consume(l);
    }

}
