package xyz.wagyourtail.jvmdg.j19.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.Objects;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class J_U_Random {

    @Stub(ref = @Ref("java/util/Random"))
    public static Random from(RandomGenerator gen) {
        Objects.requireNonNull(gen);
        if (gen instanceof Random rand) {
            return rand;
        }
        return new RandomWrapper(gen);
    }


    public static class RandomWrapper extends Random {
        final RandomGenerator rgen;

        public RandomWrapper(RandomGenerator gen) {
            super(0L);
            rgen = gen;
        }

        @Serial
        private void readObject(ObjectInputStream s) throws NotSerializableException {
            throw new NotSerializableException("not serializable");
        }

        @Serial
        private void writeObject(ObjectInputStream s) throws NotSerializableException {
            throw new NotSerializableException("not serializable");
        }

        @Override
        public synchronized void setSeed(long seed) {
            if (rgen != null) {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public boolean isDeprecated() {
            return rgen.isDeprecated();
        }

        @Override
        public void nextBytes(byte[] bytes) {
            rgen.nextBytes(bytes);
        }

        @Override
        public int nextInt() {
            return rgen.nextInt();
        }

        @Override
        public int nextInt(int bound) {
            return rgen.nextInt(bound);
        }

        @Override
        public int nextInt(int origin, int bound) {
            return rgen.nextInt(origin, bound);
        }

        @Override
        public long nextLong() {
            return rgen.nextLong();
        }

        @Override
        public long nextLong(long bound) {
            return rgen.nextLong(bound);
        }

        @Override
        public long nextLong(long origin, long bound) {
            return rgen.nextLong(origin, bound);
        }

        @Override
        public boolean nextBoolean() {
            return rgen.nextBoolean();
        }

        @Override
        public float nextFloat() {
            return rgen.nextFloat();
        }

        @Override
        public float nextFloat(float bound) {
            return rgen.nextFloat(bound);
        }

        @Override
        public float nextFloat(float origin, float bound) {
            return rgen.nextFloat(origin, bound);
        }

        @Override
        public double nextDouble() {
            return rgen.nextDouble();
        }

        @Override
        public double nextDouble(double bound) {
            return rgen.nextDouble(bound);
        }

        @Override
        public double nextDouble(double origin, double bound) {
            return rgen.nextDouble(origin, bound);
        }

        @Override
        public DoubleStream doubles() {
            return rgen.doubles();
        }

        @Override
        public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
            return rgen.doubles(randomNumberOrigin, randomNumberBound);
        }

        @Override
        public DoubleStream doubles(long streamSize) {
            return rgen.doubles(streamSize);
        }

        @Override
        public DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound) {
            return rgen.doubles(streamSize, randomNumberOrigin, randomNumberBound);
        }

        @Override
        public IntStream ints() {
            return rgen.ints();
        }

        @Override
        public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
            return rgen.ints(randomNumberOrigin, randomNumberBound);
        }

        @Override
        public IntStream ints(long streamSize) {
            return rgen.ints(streamSize);
        }

        @Override
        public IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
            return rgen.ints(streamSize, randomNumberOrigin, randomNumberBound);
        }

        @Override
        public LongStream longs() {
            return rgen.longs();
        }

        @Override
        public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
            return rgen.longs(randomNumberOrigin, randomNumberBound);
        }

        @Override
        public LongStream longs(long streamSize) {
            return rgen.longs(streamSize);
        }

        @Override
        public LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {
            return rgen.longs(streamSize, randomNumberOrigin, randomNumberBound);
        }

        @Override
        public double nextGaussian() {
            return rgen.nextGaussian();
        }

        @Override
        public double nextGaussian(double mean, double stddev) {
            return rgen.nextGaussian(mean, stddev);
        }

        @Override
        public double nextExponential() {
            return rgen.nextExponential();
        }

        @Override
        protected int next(int bits) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "RandomWrapper[" + rgen + "]";
        }
    }


}
