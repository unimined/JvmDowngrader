package xyz.wagyourtail.jvmdg.j17.impl.random;

import xyz.wagyourtail.jvmdg.j17.stub.java_base.J_U_R_RandomGenerator;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class SplittableRandomGeneratorImpl implements J_U_R_RandomGenerator.SplittableGenerator {
    private static final MethodHandles.Lookup IMPL_LOOKUP = Utils.getImplLookup();
    private static final MethodHandle CTOR;

    static {
        try {
            CTOR = IMPL_LOOKUP.findConstructor(SplittableRandom.class, MethodType.methodType(void.class, long.class, long.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private final SplittableRandom random;

    public SplittableRandomGeneratorImpl(SplittableRandom random) {
        this.random = random;
    }

    private static long mixGamma(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        z = (z ^ (z >>> 33)) | 1L;
        int n = Long.bitCount(z ^ (z >>> 1));
        return (n < 24) ? z ^ 0xaaaaaaaaaaaaaaaaL : z;
    }

    @Override
    public SplittableGenerator split() {
        return new SplittableRandomGeneratorImpl(random.split());
    }

    @Override
    public SplittableGenerator split(SplittableGenerator source) {
        try {
            return new SplittableRandomGeneratorImpl((SplittableRandom) CTOR.invoke(source.nextLong(), mixGamma(source.nextLong())));
        } catch (Throwable e) {
            Utils.sneakyThrow(e);
        }
        throw new RuntimeException("Unreachable");
    }

    @Override
    public Stream<SplittableGenerator> splits() {
        return SplittableGenerator.super.splits();
    }

    @Override
    public Stream<SplittableGenerator> splits(long size) {
        return splits().limit(size);
    }

    @Override
    public Stream<SplittableGenerator> splits(SplittableGenerator source) {
        return Stream.generate(() -> split(source));
    }

    @Override
    public Stream<SplittableGenerator> splits(long size, SplittableGenerator source) {
        return splits(source).limit(size);
    }

    @Override
    public Stream<J_U_R_RandomGenerator> rngs() {
        return SplittableGenerator.super.rngs();
    }

    @Override
    public Stream<J_U_R_RandomGenerator> rngs(long limit) {
        return SplittableGenerator.super.rngs(limit);
    }

    @Override
    public DoubleStream doubles() {
        return random.doubles();
    }

    @Override
    public DoubleStream doubles(double lower, double upper) {
        return random.doubles(lower, upper);
    }

    @Override
    public DoubleStream doubles(long limit) {
        return random.doubles(limit);
    }

    @Override
    public DoubleStream doubles(long limit, double origin, double bound) {
        return random.doubles(limit, origin, bound);
    }

    @Override
    public IntStream ints() {
        return random.ints();
    }

    @Override
    public IntStream ints(int origin, int bound) {
        return random.ints(origin, bound);
    }

    @Override
    public IntStream ints(long limit) {
        return random.ints(limit);
    }

    @Override
    public IntStream ints(long limit, int origin, int bound) {
        return random.ints(limit, origin, bound);
    }

    @Override
    public LongStream longs() {
        return random.longs();
    }

    @Override
    public LongStream longs(long origin, long bound) {
        return random.longs(origin, bound);
    }

    @Override
    public LongStream longs(long limit) {
        return random.longs(limit);
    }

    @Override
    public LongStream longs(long limit, long origin, long bound) {
        return random.longs(limit, origin, bound);
    }

    @Override
    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    @Override
    public void nextBytes(byte[] bytes) {
        random.nextBytes(bytes);
    }

    @Override
    public float nextFloat() {
        return SplittableGenerator.super.nextFloat();
    }

    @Override
    public float nextFloat(float bound) {
        return SplittableGenerator.super.nextFloat(bound);
    }

    @Override
    public float nextFloat(float origin, float bound) {
        return SplittableGenerator.super.nextFloat(origin, bound);
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }

    @Override
    public double nextDouble(double bound) {
        return random.nextDouble(bound);
    }

    @Override
    public double nextDouble(double origin, double bound) {
        return random.nextDouble(origin, bound);
    }

    @Override
    public int nextInt() {
        return random.nextInt();
    }

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    @Override
    public int nextInt(int origin, int bound) {
        return random.nextInt(origin, bound);
    }

    @Override
    public long nextLong() {
        return random.nextLong();
    }

    @Override
    public long nextLong(long bound) {
        return random.nextLong(bound);
    }

    @Override
    public long nextLong(long origin, long bound) {
        return random.nextLong(origin, bound);
    }

    @Override
    public double nextGaussian() {
        return SplittableGenerator.super.nextGaussian();
    }

    @Override
    public double nextGaussian(double mean, double stdDev) {
        return SplittableGenerator.super.nextGaussian(mean, stdDev);
    }

    @Override
    public double nextExponential() {
        return SplittableGenerator.super.nextExponential();
    }
}
