package xyz.wagyourtail.jvmdg.j8.stub;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Math {

    @Stub(ref = @Ref("java/lang/Math"))
    public static int addExact(int x, int y) {
        int r = x + y;
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static long addExact(long x, long y) {
        long r = x + y;
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static int subtractExact(int x, int y) {
        int r = x - y;
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static long subtractExact(long x, long y) {
        long r = x - y;
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static int multiplyExact(int x, int y) {
        long r = (long) x * (long) y;
        if ((int) r != r) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) r;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static long multiplyExact(long x, long y) {
        long r = x * y;
        long ax = Math.abs(x);
        long ay = Math.abs(y);
        if (((ax | ay) >>> 31 != 0)) {
            if (((y != 0) && (r / y != x)) ||
                (x == Long.MIN_VALUE && y == -1)) {
                throw new ArithmeticException("long overflow");
            }
        }
        return r;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static int incrementExact(int a) {
        if (a == Integer.MAX_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        return a + 1;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static long incrementExact(long a) {
        if (a == Long.MAX_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return a + 1L;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static int decrementExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        return a - 1;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static long decrementExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return a - 1L;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static int negateExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        return -a;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static long negateExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return -a;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static int toIntExact(long value) {
        if ((int) value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) value;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static int floorDiv(int x, int y) {
        int r = x / y;
        if (((x ^ y) < 0) && ((r * y) != x)) {
            r--;
        }
        return r;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static long floorDiv(long x, long y) {
        long r = x / y;
        if (((x ^ y) < 0) && ((r * y) != x)) {
            r--;
        }
        return r;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static int floorMod(int x, int y) {
        int r = x % y;
        if (((x ^ y) < 0) && (r < 0)) {
            return r + y;
        }
        return r;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static long floorMod(long x, long y) {
        long r = x % y;
        if (((x ^ y) < 0) && (r < 0)) {
            return r + y;
        }
        return r;
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static double nextDown(double d) {
        if (Double.isNaN(d) || d == Double.NEGATIVE_INFINITY) {
            return d;
        } else {
            if (d == 0) {
                return -Double.MIN_VALUE;
            } else {
                return Double.longBitsToDouble(Double.doubleToLongBits(d) + ((d > 0.0d) ? -1 : 1));
            }
        }
    }

    @Stub(ref = @Ref("java/lang/Math"))
    public static float nextDown(float f) {
        if (Float.isNaN(f) || f == Float.NEGATIVE_INFINITY) {
            return f;
        } else {
            if (f == 0) {
                return -Float.MIN_VALUE;
            } else {
                return Float.intBitsToFloat(Float.floatToIntBits(f) + ((f > 0.0f) ? -1 : 1));
            }
        }
    }


}
