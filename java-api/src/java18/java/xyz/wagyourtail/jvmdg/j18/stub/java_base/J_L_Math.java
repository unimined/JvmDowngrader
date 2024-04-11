package xyz.wagyourtail.jvmdg.j18.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Math {

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static long floorDivExact(long x, long y) {
        final long q = x / y;
        if ((x & y & q) >= 0) {
            if ((x ^ y) < 0 && (q * y != x)) {
                return q - 1;
            }
            return q;
        }
        throw new ArithmeticException("long overflow");
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static int ceilDivExact(int x, int y) {
        final int q = x / y;
        if ((x & y & q) >= 0) {
            if ((x ^ y) >= 0 && (q * y != x)) {
                return q + 1;
            }
            return q;
        }
        throw new ArithmeticException("integer overflow");
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static int ceilDiv(int x, int y) {
        final int q = x / y;
        if ((x ^ y) >= 0 && (q * y != x)) {
            return q + 1;
        }
        return q;
    }


    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static long divideExact(long x, long y) {
        long q = x / y;
        if ((x & y & q) >= 0) {
            return q;
        }
        throw new ArithmeticException("long overflow");
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static long ceilDiv(long x, long y) {
        final long q = x / y;
        if ((x ^ y) >= 0 && (q * y != x)) {
            return q + 1;
        }
        return q;
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static long unsignedMultiplyHigh(long x, long y) {
        long result = Math.multiplyHigh(x, y);
        result += (y & (x >> 63));
        result += (x & (y >> 63));
        return result;
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static long ceilMod(long x, long y) {
        final long r = x % y;
        if ((x ^ y) >= 0 && r != 0) {
            return r - y;
        }
        return r;
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static int ceilMod(long x, int y) {
        return (int) ceilMod(x, (long) y);
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static int floorDivExact(int x, int y) {
        final int q = x / y;
        if ((x & y & q) >= 0) {
            if ((x ^ y) < 0 && (q * y != x)) {
                return q - 1;
            }
            return q;
        }
        throw new ArithmeticException("integer overflow");
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static long ceilDivExact(long x, long y) {
        final long q = x / y;
        if ((x & y & q) >= 0) {
            if ((x ^ y) >= 0 && (q * y != x)) {
                return q + 1;
            }
            return q;
        }
        throw new ArithmeticException("long overflow");
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static int divideExact(int x, int y) {
        int q = x / y;
        if ((x & y & q) >= 0) {
            return q;
        }
        throw new ArithmeticException("integer overflow");
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static long ceilDiv(long x, int y) {
        return ceilDiv(x, (long) y);
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static int ceilMod(int x, int y) {
        final int r = x % y;
        if ((x ^ y) >= 0 && r != 0) {
            return r - y;
        }
        return r;
    }

}
