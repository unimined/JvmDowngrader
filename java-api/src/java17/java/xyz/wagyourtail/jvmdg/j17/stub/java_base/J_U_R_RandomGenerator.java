package xyz.wagyourtail.jvmdg.j17.stub.java_base;

import xyz.wagyourtail.jvmdg.exc.MissingStubError;
import xyz.wagyourtail.jvmdg.version.Adapter;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.Random;

@Adapter(value = "java/util/random/RandomGenerator")
public interface J_U_R_RandomGenerator {

    public static boolean jvmdg$instanceof(Object obj) {
        return obj instanceof J_U_R_RandomGenerator ||
                obj instanceof java.util.Random;
    }

    public static J_U_R_RandomGenerator jvmdg$checkcast(Object obj) {
        if (obj instanceof Random) {
            throw MissingStubError.create();
        }
        return (J_U_R_RandomGenerator) obj;
    }

    @Stub
    static long nextLong(Random random, long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        final long m = bound - 1;
        long r = random.nextLong();
        if ((bound & m) == 0L) {
            r &= m;
        } else {
            long u = r >>> 1;
            while (u + m - (r = u % bound) < 0L) {
                u = random.nextLong() >>> 1;
            }
        }
        return r;
    }

    // TODO: Rework engine to not need this to implement for both,
    //  by adding checkCast and then call this version instead of above
    //  need to add java classes that inherit from this interface to @Adapter
    default long nextLong(long bound) {
        throw MissingStubError.create();
    }

}
