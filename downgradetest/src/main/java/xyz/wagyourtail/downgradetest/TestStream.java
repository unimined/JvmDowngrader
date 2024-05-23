package xyz.wagyourtail.downgradetest;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class TestStream {
    public static void main(String[] args) {

        DoubleStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .mapMulti((d, dc) -> {
                    dc.accept(d);
                    dc.accept(d * 2);
                    dc.accept(d * 3);
                }).mapToObj(Double::toString)
                .forEach(System.out::print);
        System.out.println();

        IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .mapMulti((d, dc) -> {
                    dc.accept(d);
                    dc.accept(d * 2);
                    dc.accept(d * 3);
                }).mapToObj(Integer::toString)
                .forEach(System.out::print);
        System.out.println();

        LongStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .mapMulti((d, dc) -> {
                    dc.accept(d);
                    dc.accept(d * 2);
                    dc.accept(d * 3);
                }).mapToObj(Long::toString)
                .forEach(System.out::print);
        System.out.println();

        DoubleStream.of(1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
                .takeWhile(d -> d % 10 < 4)
                .mapToObj(Double::toString)
                .forEach(System.out::print);
        System.out.println();

        IntStream.of(1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
                .dropWhile(d -> d % 10 < 4)
                .mapToObj(Integer::toString)
                .forEach(System.out::print);
        System.out.println();

        DoubleStream.iterate(0, d -> d < 10, d -> d + 1)
                .mapToObj(Double::toString)
                .forEach(System.out::print);
        System.out.println();
    }

}
