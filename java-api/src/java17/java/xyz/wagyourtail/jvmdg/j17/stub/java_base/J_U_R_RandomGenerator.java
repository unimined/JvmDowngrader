package xyz.wagyourtail.jvmdg.j17.stub.java_base;

import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.j17.impl.random.BasicRandomGeneratorImpl;
import xyz.wagyourtail.jvmdg.j17.impl.random.SplittableRandomGeneratorImpl;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Coerce;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Random;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@Adapter(value = "java/util/random/RandomGenerator")
public interface J_U_R_RandomGenerator {

    static boolean jvmdg$instanceof(Object obj) {
        return obj instanceof J_U_R_RandomGenerator ||
                obj instanceof Random ||
                obj instanceof SplittableRandom;
    }

    static J_U_R_RandomGenerator jvmdg$checkcast(Object obj) {
        if (obj instanceof Random random) {
            return new BasicRandomGeneratorImpl(random);
        }
        if (obj instanceof SplittableRandom random) {
            return new SplittableRandomGeneratorImpl(random);
        }
        return (J_U_R_RandomGenerator) obj;
    }

    static J_U_R_RandomGenerator of(String name) {
        throw MissingStubError.create();
    }

    static J_U_R_RandomGenerator getDefault() {
        return new BasicRandomGeneratorImpl(new Random());
    }

    @Stub(noSpecial = true)
    static boolean isDeprecated(@Coerce(J_U_R_RandomGenerator.class) Object obj) {
        return jvmdg$checkcast(obj).isDeprecated();
    }

    @Stub(noSpecial = true)
    static DoubleStream doubles(@Coerce(J_U_R_RandomGenerator.class) Object obj) {
        return jvmdg$checkcast(obj).doubles();
    }

    @Stub(noSpecial = true)
    static DoubleStream doubles(@Coerce(J_U_R_RandomGenerator.class) Object obj, double lower, double upper) {
        return jvmdg$checkcast(obj).doubles(lower, upper);
    }

    @Stub(noSpecial = true)
    static DoubleStream doubles(@Coerce(J_U_R_RandomGenerator.class) Object obj, long limit) {
        return jvmdg$checkcast(obj).doubles(limit);
    }

    @Stub(noSpecial = true)
    static DoubleStream doubles(@Coerce(J_U_R_RandomGenerator.class) Object obj, long limit, double origin, double bound) {
        return jvmdg$checkcast(obj).doubles(limit, origin, bound);
    }

    @Stub(noSpecial = true)
    static IntStream ints(@Coerce(J_U_R_RandomGenerator.class) Object obj) {
        return jvmdg$checkcast(obj).ints();
    }

    @Stub(noSpecial = true)
    static IntStream ints(@Coerce(J_U_R_RandomGenerator.class) Object obj, int origin, int bound) {
        return jvmdg$checkcast(obj).ints(origin, bound);
    }

    @Stub(noSpecial = true)
    static IntStream ints(@Coerce(J_U_R_RandomGenerator.class) Object obj, long limit) {
        return jvmdg$checkcast(obj).ints(limit);
    }

    @Stub(noSpecial = true)
    static IntStream ints(@Coerce(J_U_R_RandomGenerator.class) Object obj, long limit, int origin, int bound) {
        return jvmdg$checkcast(obj).ints(limit, origin, bound);
    }

    @Stub(noSpecial = true)
    static LongStream longs(@Coerce(J_U_R_RandomGenerator.class) Object obj) {
        return jvmdg$checkcast(obj).longs();
    }

    @Stub(noSpecial = true)
    static LongStream longs(@Coerce(J_U_R_RandomGenerator.class) Object obj, long origin, long bound) {
        return jvmdg$checkcast(obj).longs(origin, bound);
    }

    @Stub(noSpecial = true)
    static LongStream longs(@Coerce(J_U_R_RandomGenerator.class) Object obj, long limit) {
        return jvmdg$checkcast(obj).longs(limit);
    }

    @Stub(noSpecial = true)
    static LongStream longs(@Coerce(J_U_R_RandomGenerator.class) Object obj, long limit, long origin, long bound) {
        return jvmdg$checkcast(obj).longs(limit, origin, bound);
    }

    @Stub(noSpecial = true)
    static boolean nextBoolean(@Coerce(J_U_R_RandomGenerator.class) Object obj) {
        return jvmdg$checkcast(obj).nextBoolean();
    }

    @Stub(noSpecial = true)
    static void nextBytes(@Coerce(J_U_R_RandomGenerator.class) Object obj, byte[] bytes) {
        jvmdg$checkcast(obj).nextBytes(bytes);
    }

    @Stub(noSpecial = true)
    static float nextFloat(@Coerce(J_U_R_RandomGenerator.class) Object obj) {
        return jvmdg$checkcast(obj).nextFloat();
    }

    @Stub(noSpecial = true)
    static float nextFloat(@Coerce(J_U_R_RandomGenerator.class) Object obj, float bound) {
        return jvmdg$checkcast(obj).nextFloat(bound);
    }

    @Stub(noSpecial = true)
    static float nextFloat(@Coerce(J_U_R_RandomGenerator.class) Object obj, float origin, float bound) {
        return jvmdg$checkcast(obj).nextFloat(origin, bound);
    }

    @Stub(noSpecial = true)
    static double nextDouble(@Coerce(J_U_R_RandomGenerator.class) Object obj) {
        return jvmdg$checkcast(obj).nextDouble();
    }

    @Stub(noSpecial = true)
    static double nextDouble(@Coerce(J_U_R_RandomGenerator.class) Object obj, double bound) {
        return jvmdg$checkcast(obj).nextDouble(bound);
    }

    @Stub(noSpecial = true)
    static double nextDouble(@Coerce(J_U_R_RandomGenerator.class) Object obj, double origin, double bound) {
        return jvmdg$checkcast(obj).nextDouble(origin, bound);
    }

    @Stub(noSpecial = true)
    static int nextInt(@Coerce(J_U_R_RandomGenerator.class) Object obj) {
        return jvmdg$checkcast(obj).nextInt();
    }

    @Stub(noSpecial = true)
    static int nextInt(@Coerce(J_U_R_RandomGenerator.class) Object obj, int bound) {
        return jvmdg$checkcast(obj).nextInt(bound);
    }

    @Stub(noSpecial = true)
    static int nextInt(@Coerce(J_U_R_RandomGenerator.class) Object obj, int origin, int bound) {
        return jvmdg$checkcast(obj).nextInt(origin, bound);
    }

    @Stub(noSpecial = true)
    static long nextLong(@Coerce(J_U_Random.class) Object obj) {
        return jvmdg$checkcast(obj).nextLong();
    }

    @Stub(noSpecial = true)
    static long nextLong(@Coerce(J_U_Random.class) Object obj, long bound) {
        return jvmdg$checkcast(obj).nextLong(bound);
    }

    @Stub(noSpecial = true)
    static long nextLong(@Coerce(J_U_Random.class) Object obj, long origin, long bound) {
        return jvmdg$checkcast(obj).nextLong(origin, bound);
    }

    @Stub(noSpecial = true)
    static double nextGaussian(@Coerce(J_U_Random.class) Object obj) {
        return jvmdg$checkcast(obj).nextGaussian();
    }

    @Stub(noSpecial = true)
    static double nextGaussian(@Coerce(J_U_Random.class) Object obj, double mean, double stdDev) {
        return jvmdg$checkcast(obj).nextGaussian(mean, stdDev);
    }

    @Stub(noSpecial = true)
    static double nextExponential(@Coerce(J_U_Random.class) Object obj) {
        return jvmdg$checkcast(obj).nextExponential();
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
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }

        float r = nextFloat();
        r = r * bound;
        if (r >= bound) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    default float nextFloat(float origin, float bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        float r = nextFloat();
        r = r * (bound - origin) + origin;
        if (r >= bound) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    default double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    default double nextDouble(double bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }

        double r = nextDouble();
        r = r * bound;
        if (r >= bound) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    default double nextDouble(double origin, double bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        double r = nextDouble();
        r = r * (bound - origin) + origin;
        if (r >= bound) {
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
        throw MissingStubError.create();
    }

    default double nextGaussian(double mean, double stdDev) {
        throw MissingStubError.create();
    }

    default double nextExponential() {
        throw MissingStubError.create();
    }

    @Adapter(value = "java/util/random/RandomGenerator$StreamableGenerator")
    interface StreamableGenerator extends J_U_R_RandomGenerator {

        static boolean jvmdg$instanceof(Object obj) {
            return obj instanceof StreamableGenerator ||
                    obj instanceof SplittableRandom;
        }

        static StreamableGenerator jvmdg$checkcast(Object obj) {
            if (obj instanceof SplittableRandom random) {
                return new SplittableRandomGeneratorImpl(random);
            }
            return (StreamableGenerator) obj;
        }

        static StreamableGenerator of(String name) {
            throw MissingStubError.create();
        }

        @Stub(noSpecial = true)
        static Stream<J_U_R_RandomGenerator> rngs(@Coerce(J_U_Random.class) Object obj) {
            return jvmdg$checkcast(obj).rngs();
        }

        @Stub(noSpecial = true)
        static Stream<J_U_R_RandomGenerator> rngs(@Coerce(J_U_Random.class) Object obj, long limit) {
            return jvmdg$checkcast(obj).rngs(limit);
        }

        Stream<J_U_R_RandomGenerator> rngs();

        default Stream<J_U_R_RandomGenerator> rngs(long limit) {
            if (limit < 0) {
                throw new IllegalArgumentException("size must be non-negative");
            }
            return rngs().limit(limit);
        }

    }

    @Adapter(value = "java/util/random/RandomGenerator$SplittableGenerator")
    interface SplittableGenerator extends StreamableGenerator {

        static boolean jvmdg$instanceof(Object obj) {
            return obj instanceof SplittableGenerator ||
                    obj instanceof SplittableRandom;
        }

        static SplittableGenerator jvmdg$checkcast(Object obj) {
            if (obj instanceof SplittableRandom random) {
                return new SplittableRandomGeneratorImpl(random);
            }
            return (SplittableGenerator) obj;
        }

        static SplittableGenerator of(String name) {
            throw MissingStubError.create();
        }

        @Stub(noSpecial = true)
        static SplittableGenerator split(@Coerce(J_U_Random.class) Object obj) {
            return jvmdg$checkcast(obj).split();
        }

        @Stub(noSpecial = true)
        static SplittableGenerator split(@Coerce(J_U_Random.class) Object obj, SplittableGenerator source) {
            return jvmdg$checkcast(obj).split(source);
        }

        @Stub(noSpecial = true)
        static Stream<SplittableGenerator> splits(@Coerce(J_U_Random.class) Object obj) {
            return jvmdg$checkcast(obj).splits();
        }

        @Stub(noSpecial = true)
        static Stream<SplittableGenerator> splits(@Coerce(J_U_Random.class) Object obj, long size) {
            return jvmdg$checkcast(obj).splits(size);
        }

        @Stub(noSpecial = true)
        static Stream<SplittableGenerator> splits(@Coerce(J_U_Random.class) Object obj, SplittableGenerator source) {
            return jvmdg$checkcast(obj).splits(source);
        }

        @Stub(noSpecial = true)
        static Stream<SplittableGenerator> splits(@Coerce(J_U_Random.class) Object obj, long size, SplittableGenerator source) {
            return jvmdg$checkcast(obj).splits(size, source);
        }

        @Stub(noSpecial = true)
        static Stream<J_U_R_RandomGenerator> rngs(@Coerce(J_U_Random.class) Object obj) {
            return jvmdg$checkcast(obj).rngs();
        }

        @Stub(noSpecial = true)
        static Stream<J_U_R_RandomGenerator> rngs(@Coerce(J_U_Random.class) Object obj, long limit) {
            return jvmdg$checkcast(obj).rngs(limit);
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
