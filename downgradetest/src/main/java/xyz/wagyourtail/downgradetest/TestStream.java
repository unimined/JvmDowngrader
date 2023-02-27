package xyz.wagyourtail.downgradetest;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class TestStream {
    public static void main(String[] args) {

        DoubleStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).mapMulti((d, dc) -> {
            dc.accept(d);
            dc.accept(d * 2);
            dc.accept(d * 3);
        }).forEach(System.out::println);

        IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).mapMulti((d, dc) -> {
            dc.accept(d);
            dc.accept(d * 2);
            dc.accept(d * 3);
        }).forEach(System.out::println);

        LongStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).mapMulti((d, dc) -> {
            dc.accept(d);
            dc.accept(d * 2);
            dc.accept(d * 3);
        }).forEach(System.out::println);
    }
}
