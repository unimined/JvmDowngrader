package xyz.wagyourtail.downgradetest;

import java.util.Random;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

public class TestRandom {

    public static void main(String[] args) {
        RandomGenerator random = new Random(0x12345678);
        System.out.println(random.nextInt());

        random = new SplittableRandom(0x12345678);
        System.out.println(random.nextInt());

        SplittableRandom sr = new SplittableRandom(0x12345678);
        System.out.println(sr.nextLong(0, 100));

        Random r = new Random(0x12345678);
        System.out.println(r.nextLong(0, 100));

        sr.splits();

        random = new RandomGenImpl();
        System.out.println(random.nextInt());
    }

    private static class RandomGenImpl implements RandomGenerator {

        @Override
        public int nextInt() {
            return RandomGenerator.super.nextInt();
        }

        @Override
        public long nextLong() {
            return 0;
        }

    }

}
