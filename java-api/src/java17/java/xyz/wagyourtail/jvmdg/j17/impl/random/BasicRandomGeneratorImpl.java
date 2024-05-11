package xyz.wagyourtail.jvmdg.j17.impl.random;

import xyz.wagyourtail.jvmdg.j17.stub.java_base.J_U_R_RandomGenerator;

import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class BasicRandomGeneratorImpl implements J_U_R_RandomGenerator {

    private final Random random;

    public BasicRandomGeneratorImpl(Random random) {
        this.random = random;
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
        return random.nextFloat();
    }

    @Override
    public float nextFloat(float bound) {
        return J_U_R_RandomGenerator.super.nextFloat(bound);
    }

    @Override
    public float nextFloat(float origin, float bound) {
        return J_U_R_RandomGenerator.super.nextFloat(origin, bound);
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }

    @Override
    public double nextDouble(double bound) {
        return J_U_R_RandomGenerator.super.nextDouble(bound);
    }

    @Override
    public double nextDouble(double origin, double bound) {
        return J_U_R_RandomGenerator.super.nextDouble(origin, bound);
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
        return J_U_R_RandomGenerator.super.nextInt(origin, bound);
    }

    @Override
    public long nextLong() {
        return random.nextLong();
    }

    @Override
    public long nextLong(long bound) {
        return J_U_R_RandomGenerator.super.nextLong(bound);
    }

    @Override
    public long nextLong(long origin, long bound) {
        return J_U_R_RandomGenerator.super.nextLong(origin, bound);
    }

    @Override
    public double nextGaussian() {
        return random.nextGaussian();
    }

    @Override
    public double nextGaussian(double mean, double stdDev) {
        return J_U_R_RandomGenerator.super.nextGaussian(mean, stdDev);
    }

    @Override
    public double nextExponential() {
        return J_U_R_RandomGenerator.super.nextExponential();
    }
}
