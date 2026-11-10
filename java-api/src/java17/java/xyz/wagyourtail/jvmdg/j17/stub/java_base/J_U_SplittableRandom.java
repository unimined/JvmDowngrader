package xyz.wagyourtail.jvmdg.j17.stub.java_base;

import xyz.wagyourtail.jvmdg.j17.impl.random.SplittableRandomGeneratorImpl;
import xyz.wagyourtail.jvmdg.version.JEP;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.SplittableRandom;
import java.util.stream.Stream;

@JEP(356)
public class J_U_SplittableRandom {

    // split() (no-arg) is intentionally unstubbed: SplittableRandom.split()
    // exists since Java 8, so calls resolve natively on all targets.
    // split(source) below IS needed — but the real member is covariant:
    // javac emits invoke* SplittableRandom.split(SG)SplittableRandom, not the
    // (SG)SplittableGenerator bridge, so the stub must return SplittableRandom.
    @Stub
    public static SplittableRandom split(SplittableRandom random, J_U_R_RandomGenerator.SplittableGenerator source) {
        return SplittableRandomGeneratorImpl.splitToSplittableRandom(source);
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

    @Stub
    public static Stream<J_U_R_RandomGenerator> rngs(SplittableRandom random) {
        return new SplittableRandomGeneratorImpl(random).rngs();
    }

    @Stub
    public static Stream<J_U_R_RandomGenerator> rngs(SplittableRandom random, long size) {
        return new SplittableRandomGeneratorImpl(random).rngs(size);
    }

}
