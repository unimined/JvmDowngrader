package xyz.wagyourtail.jvmdg.j17.stub.java_base;

import xyz.wagyourtail.jvmdg.j17.impl.random.BasicRandomGeneratorImpl;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Random;

public class J_U_Random {

    @Stub
    public static float nextFloat(Random random, float bound) {
        return new BasicRandomGeneratorImpl(random).nextFloat(bound);
    }

    @Stub
    public static float nextFloat(Random random, float origin, float bound) {
        return new BasicRandomGeneratorImpl(random).nextFloat(origin, bound);
    }

    @Stub
    public static double nextDouble(Random random, double bound) {
        return new BasicRandomGeneratorImpl(random).nextDouble(bound);
    }

    @Stub
    public static double nextDouble(Random random, double origin, double bound) {
        return new BasicRandomGeneratorImpl(random).nextDouble(origin, bound);
    }

    @Stub
    public static int nextInt(Random random, int origin, int bound) {
        return new BasicRandomGeneratorImpl(random).nextInt(origin, bound);
    }

    @Stub
    public static long nextLong(Random random, long bound) {
        return new BasicRandomGeneratorImpl(random).nextLong(bound);
    }

    @Stub
    public static long nextLong(Random random, long origin, long bound) {
        return new BasicRandomGeneratorImpl(random).nextLong(origin, bound);
    }

    @Stub
    public static double nextExponential(Random random) {
        return new BasicRandomGeneratorImpl(random).nextExponential();
    }

    @Stub
    public double nextGaussian(Random random, double mean, double stdDev) {
        return new BasicRandomGeneratorImpl(random).nextGaussian(mean, stdDev);
    }

}
