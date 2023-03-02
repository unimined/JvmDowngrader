package xyz.wagyourtail.downgradetest;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class TestTime {

    public static void main(String[] args) {
        Duration d = Duration.ofNanos(1457812567137895L);
        System.out.println(TimeUnit.SECONDS.convert(d));
    }

}
