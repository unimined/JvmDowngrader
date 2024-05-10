package xyz.wagyourtail.downgradetest;

import java.util.Random;
import java.util.random.RandomGenerator;

public class TestRandom {

    public static void main(String[] args) {
        RandomGenerator random = new Random(0x12345678);
        random.nextInt();
    }

}
