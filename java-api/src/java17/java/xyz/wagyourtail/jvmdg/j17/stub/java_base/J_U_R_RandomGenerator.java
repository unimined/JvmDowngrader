package xyz.wagyourtail.jvmdg.j17.stub.java_base;

import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j17.impl.random.BasicRandomGeneratorImpl;
import xyz.wagyourtail.jvmdg.j17.impl.random.SplittableRandomGeneratorImpl;
import xyz.wagyourtail.jvmdg.stub.RetroTypes;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.JEP;

import java.util.Random;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@JEP(356)
@Adapter(value = "java/util/random/RandomGenerator", impls = {BasicRandomGeneratorImpl.class})
public interface J_U_R_RandomGenerator {

    static J_U_R_RandomGenerator of(String name) {
        if ("SplittableRandom".equals(name)) {
            return new SplittableRandomGeneratorImpl(new SplittableRandom());
        }
        if ("Random".equals(name)) {
            return new BasicRandomGeneratorImpl(new Random());
        }
        throw new IllegalArgumentException("Unknown implementation: " + name);
    }

    static J_U_R_RandomGenerator getDefault() {
        return new BasicRandomGeneratorImpl(new Random());
    }

    default boolean isDeprecated() {
        return false;
    }

    default DoubleStream doubles() {
        return DoubleStream.generate(this::nextDouble).sequential();
    }

    default DoubleStream doubles(double lower, double upper) {
        if (!(lower < upper && (upper - lower) < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return DoubleStream.generate(() -> nextDouble(lower, upper)).sequential();
    }

    default DoubleStream doubles(long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        return doubles().limit(limit).sequential();
    }

    default DoubleStream doubles(long limit, double origin, double bound) {
        if (limit < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        if (!(origin < bound && (bound - origin) < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return DoubleStream.generate(() -> nextDouble(origin, bound)).limit(limit).sequential();
    }

    default IntStream ints() {
        return IntStream.generate(this::nextInt).sequential();
    }

    default IntStream ints(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return IntStream.generate(() -> nextInt(origin, bound)).sequential();
    }

    default IntStream ints(long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        return ints().limit(limit).sequential();
    }

    default IntStream ints(long limit, int origin, int bound) {
        if (limit < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return ints(origin, bound).limit(limit).sequential();
    }

    default LongStream longs() {
        return LongStream.generate(this::nextLong).sequential();
    }

    default LongStream longs(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return LongStream.generate(() -> nextLong(origin, bound)).sequential();
    }

    default LongStream longs(long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        return longs().limit(limit).sequential();
    }

    default LongStream longs(long limit, long origin, long bound) {
        if (limit < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return longs(origin, bound).limit(limit).sequential();
    }

    default boolean nextBoolean() {
        return nextInt() < 0;
    }

    default void nextBytes(byte[] bytes) {
        int i = 0;
        int len = bytes.length;
        int words = len >> 3;
        while (words-- > 0) {
            long rnd = nextLong();
            for (int n = 0; n < 8; n++) {
                bytes[i++] = (byte) rnd;
                rnd >>>= Byte.SIZE;
            }
        }
        if (i < len) {
            long rnd = nextLong();
            for (int n = 0; n < len - i; n++) {
                bytes[i++] = (byte) rnd;
                rnd >>>= Byte.SIZE;
            }
        }
    }

    default float nextFloat() {
        return (nextInt() >>> 8) * 0x1.0p-24f;
    }

    default float nextFloat(float bound) {
        // RandomSupport.checkBound: must be finite and positive (rejects NaN)
        if (!(bound > 0.0f && bound < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException("bound must be finite and positive");
        }

        float r = nextFloat();
        r = r * bound;
        if (!(r < bound)) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    default float nextFloat(float origin, float bound) {
        // RandomSupport.checkRange: also rejects a non-finite range width
        if (!(origin < bound && (bound - origin) < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        float r = nextFloat();
        if (bound - origin < Float.POSITIVE_INFINITY) {
            r = r * (bound - origin) + origin;
        } else {
            // matches jdk.internal.util.random.RandomSupport.boundedNextFloat:
            // avoids overflowing (bound - origin) for extreme ranges
            float halfOrigin = 0.5f * origin;
            r = (r * (0.5f * bound - halfOrigin) + halfOrigin) * 2.0f;
        }
        if (!(r < bound)) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    default double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    default double nextDouble(double bound) {
        if (!(bound > 0.0 && bound < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException("bound must be finite and positive");
        }

        double r = nextDouble();
        r = r * bound;
        if (!(r < bound)) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    default double nextDouble(double origin, double bound) {
        if (!(origin < bound && (bound - origin) < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        double r = nextDouble();
        if (bound - origin < Double.POSITIVE_INFINITY) {
            r = r * (bound - origin) + origin;
        } else {
            // avoids overflowing (bound - origin) for extreme ranges
            double halfOrigin = 0.5 * origin;
            r = (r * (0.5 * bound - halfOrigin) + halfOrigin) * 2.0;
        }
        if (!(r < bound)) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    default int nextInt() {
        return (int) (nextLong() >>> 32);
    }

    default int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        int r = nextInt();
        int m = bound - 1;
        if ((bound & m) == 0) {
            r &= m;
        } else {
            int u = r >>> 1;
            while (u + m - (r = u % bound) < 0) {
                u = nextInt() >>> 1;
            }
        }
        return r;
    }

    default int nextInt(int origin, int bound) {
        if (!(origin < bound)) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        int r = nextInt();

        if (origin < bound) {
            final int n = bound - origin;
            final int m = n - 1;
            if ((n & m) == 0) {
                r = (r & m) + origin;
            } else if (n > 0) {
                int u = r >>> 1;
                while (u + m - (r = u % n) < 0) {
                    u = nextInt() >>> 1;
                }
                r += origin;
            } else {
                while (r < origin || r >= bound) {
                    r = nextInt();
                }
            }
        }

        return r;
    }

    long nextLong();

    default long nextLong(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        final long m = bound - 1;
        long r = nextLong();

        if ((bound & m) == 0L) {
            r &= m;
        } else {
            long u = r >>> 1;
            while (u + m - (r = u % bound) < 0L) {
                u = nextLong() >>> 1;
            }
        }

        return r;
    }

    default long nextLong(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        long r = nextLong();
        final long n = bound - origin;
        final long m = n - 1;

        if ((n & m) == 0L) {
            r = (r & m) + origin;
        } else if (n > 0L) {
            long u = r >>> 1;
            while (u + m - (r = u % n) < 0L) {
                u = nextLong() >>> 1;
            }
            r += origin;
        } else {
            while (r < origin || r >= bound) {
                r = nextLong();
            }
        }

        return r;
    }

    default double nextGaussian() {
        // the JDK default draws via RandomSupport.computeNextGaussian
        // (McFarland ziggurat) on nextLong() bits — match it exactly so
        // seeded generators reproduce the native sequence
        return xyz.wagyourtail.jvmdg.j17.impl.random.RandomSupport.computeNextGaussian(this);
    }

    default double nextGaussian(double mean, double stdDev) {
        if (stdDev < 0) {
            throw new IllegalArgumentException("standard deviation must be non-negative");
        }
        // the JDK default calls computeNextGaussian(this) directly — not the
        // overridable nextGaussian() — so overrides like Random's polar
        // sampler are bypassed here
        return mean + stdDev * xyz.wagyourtail.jvmdg.j17.impl.random.RandomSupport.computeNextGaussian(this);
    }

    default double nextExponential() {
        // RandomSupport.computeNextExponential, as above
        return xyz.wagyourtail.jvmdg.j17.impl.random.RandomSupport.computeNextExponential(this);
    }

    @Adapter(value = "java/util/random/RandomGenerator$StreamableGenerator")
    interface StreamableGenerator extends J_U_R_RandomGenerator {

        static StreamableGenerator of(String name) {
            if ("SplittableRandom".equals(name)) {
                return new SplittableRandomGeneratorImpl(new SplittableRandom());
            }
            throw new IllegalArgumentException("Unknown implementation: " + name);
        }

        Stream<J_U_R_RandomGenerator> rngs();

        default Stream<J_U_R_RandomGenerator> rngs(long limit) {
            if (limit < 0) {
                throw new IllegalArgumentException("size must be non-negative");
            }
            return rngs().limit(limit);
        }

    }

    @Adapter(value = "java/util/random/RandomGenerator$SplittableGenerator", impls = {SplittableRandomGeneratorImpl.class})
    interface SplittableGenerator extends StreamableGenerator {

        static SplittableGenerator jvmdg$wrap(Object obj) {
            return (SplittableGenerator) RetroTypes.wrap(obj, "java/util/random/RandomGenerator$SplittableGenerator", SplittableGenerator.class);
        }

        static SplittableGenerator of(String name) {
            if ("SplittableRandom".equals(name)) {
                return new SplittableRandomGeneratorImpl(new SplittableRandom());
            }
            throw new IllegalArgumentException("Unknown implementation: " + name);
        }

        SplittableGenerator split();

        SplittableGenerator split(SplittableGenerator source);

        default Stream<SplittableGenerator> splits() {
            return splits(this);
        }

        Stream<SplittableGenerator> splits(long size);

        Stream<SplittableGenerator> splits(SplittableGenerator source);

        Stream<SplittableGenerator> splits(long size, SplittableGenerator source);

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        default Stream<J_U_R_RandomGenerator> rngs() {
            return (Stream) splits();
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        default Stream<J_U_R_RandomGenerator> rngs(long limit) {
            return (Stream) splits(limit);
        }

    }

    @Adapter(value = "java/util/random/RandomGenerator$JumpableGenerator")
    interface JumpableGenerator extends StreamableGenerator {

        static JumpableGenerator of(String name) {
            throw MissingStubError.create();
        }

        JumpableGenerator copy();

        void jump();

        double jumpDistance();

        default Stream<J_U_R_RandomGenerator> jumps() {
            return Stream.generate(this::copyAndJump).sequential();
        }

        default Stream<J_U_R_RandomGenerator> jumps(long limit) {
            return jumps().limit(limit);
        }

        @Override
        default Stream<J_U_R_RandomGenerator> rngs() {
            return jumps();
        }

        @Override
        default Stream<J_U_R_RandomGenerator> rngs(long limit) {
            return jumps(limit);
        }

        default J_U_R_RandomGenerator copyAndJump() {
            JumpableGenerator copy = copy();
            jump();
            return copy;
        }

    }

    @Adapter(value = "java/util/random/RandomGenerator$LeapableGenerator")
    interface LeapableGenerator extends JumpableGenerator {

        static LeapableGenerator of(String name) {
            throw MissingStubError.create();
        }

        @Override
        LeapableGenerator copy();

        void leap();

        double leapDistance();

        default Stream<JumpableGenerator> leaps() {
            return Stream.generate(this::copyAndLeap).sequential();
        }

        default Stream<JumpableGenerator> leaps(long limit) {
            return leaps().limit(limit);
        }

        default JumpableGenerator copyAndLeap() {
            LeapableGenerator copy = copy();
            leap();
            return copy;
        }

    }

    @Adapter(value = "java/util/random/RandomGenerator$ArbitrarilyJumpableGenerator")
    interface ArbitrarilyJumpableGenerator extends LeapableGenerator {

        static ArbitrarilyJumpableGenerator of(String name) {
            throw MissingStubError.create();
        }

        @Override
        ArbitrarilyJumpableGenerator copy();

        void jumpPowerOfTwo(int logDistance);

        void jump(double distance);

        @Override
        default void jump() {
            jump(jumpDistance());
        }

        default Stream<ArbitrarilyJumpableGenerator> jumps(double distance) {
            return Stream.generate(() -> copyAndJump(distance)).sequential();
        }

        default Stream<ArbitrarilyJumpableGenerator> jumps(long limit, double distance) {
            return jumps(distance).limit(limit);
        }

        default void leap() {
            jump(leapDistance());
        }

        default ArbitrarilyJumpableGenerator copyAndJump(double distance) {
            ArbitrarilyJumpableGenerator copy = copy();
            jump(distance);
            return copy;
        }

    }

}
