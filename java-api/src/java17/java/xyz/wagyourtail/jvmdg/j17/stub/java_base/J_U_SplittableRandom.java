package xyz.wagyourtail.jvmdg.j17.stub.java_base;

import xyz.wagyourtail.jvmdg.j17.impl.random.SplittableRandomGeneratorImpl;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.SplittableRandom;
import java.util.stream.Stream;

public class J_U_SplittableRandom {

    @Stub
    public static J_U_R_RandomGenerator.SplittableGenerator split(SplittableRandom random) {
        return new SplittableRandomGeneratorImpl(random).split();
    }

    @Stub
    public static J_U_R_RandomGenerator.SplittableGenerator split(SplittableRandom random, J_U_R_RandomGenerator.SplittableGenerator source) {
        return new SplittableRandomGeneratorImpl(random).split(source);
    }

    @Stub
    public static Stream<J_U_R_RandomGenerator.SplittableGenerator> splits(SplittableRandom random) {
        return new SplittableRandomGeneratorImpl(random).splits();
    }

    @Stub
    public static Stream<J_U_R_RandomGenerator.SplittableGenerator> splits(SplittableRandom random, long size) {
        return new SplittableRandomGeneratorImpl(random).splits(size);
    }

    @Stub
    public static Stream<J_U_R_RandomGenerator.SplittableGenerator> splits(SplittableRandom random, J_U_R_RandomGenerator.SplittableGenerator source) {
        return new SplittableRandomGeneratorImpl(random).splits(source);
    }

    @Stub
    public static Stream<J_U_R_RandomGenerator.SplittableGenerator> splits(SplittableRandom random, long size, J_U_R_RandomGenerator.SplittableGenerator source) {
        return new SplittableRandomGeneratorImpl(random).splits(size, source);
    }

    @Stub
    public Stream<J_U_R_RandomGenerator> rngs(SplittableRandom random) {
        return new SplittableRandomGeneratorImpl(random).rngs();
    }

    @Stub
    public Stream<J_U_R_RandomGenerator> rngs(SplittableRandom random, long size) {
        return new SplittableRandomGeneratorImpl(random).rngs(size);
    }

    @Stub
    public static float nextFloat(SplittableRandom random) {
        return new SplittableRandomGeneratorImpl(random).nextFloat();
    }

    @Stub
    public static float nextFloat(SplittableRandom random, float bound) {
        return new SplittableRandomGeneratorImpl(random).nextFloat(bound);
    }

    @Stub
    public static float nextFloat(SplittableRandom random, float origin, float bound) {
        return new SplittableRandomGeneratorImpl(random).nextFloat(origin, bound);
    }

    @Stub
    public static double nextGaussian(SplittableRandom random) {
        return new SplittableRandomGeneratorImpl(random).nextGaussian();
    }

    @Stub
    public static double nextGaussian(SplittableRandom random, double mean, double stdDev) {
        return new SplittableRandomGeneratorImpl(random).nextGaussian(mean, stdDev);
    }

    @Stub
    public static double nextExponential(SplittableRandom random) {
        return new SplittableRandomGeneratorImpl(random).nextExponential();
    }

}
