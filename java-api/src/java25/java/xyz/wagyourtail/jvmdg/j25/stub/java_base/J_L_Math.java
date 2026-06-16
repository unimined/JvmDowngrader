package xyz.wagyourtail.jvmdg.j25.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import static java.lang.Math.multiplyExact;
import static java.lang.Math.unsignedMultiplyHigh;

public class J_L_Math {
    
    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static int powExact(int x, int n) {
        if (n < 0) {
            throw new ArithmeticException("negative exponent");
        } else if (n == 0) {
            return 1;
        }
        if (Math.abs(x) <= 1) {
            if (x < 0) {
                return (n & 0b1) == 0 ? 1 : -1;
            }
            return x;
        }

        int p = 1;
        while (n > 1) {
            if ((n & 0b1) != 0) {
                p *= x;
            }
            x = multiplyExact(x, x);
            n >>>= 1;
        }
        return multiplyExact(p, x);
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static long powExact(long x, int n) {
        if (n < 0) {
            throw new ArithmeticException("negative exponent");
        } else if (n == 0) {
            return 1;
        }
        if (Math.abs(x) <= 1) {
            if (x < 0) {
                return (n & 0b1) == 0 ? 1 : -1;
            }
            return x;
        }

        long p = 1;
        while (n > 1) {
            if ((n & 0b1) != 0) {
                p *= x;
            }
            x = multiplyExact(x, x);
            n >>>= 1;
        }
        return multiplyExact(p, x);
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static int unsignedMultiplyExact(int x, int y) {
        long r = (x & 0xFFFF_FFFFL) * (y & 0xFFFF_FFFFL);
        if (r >>> 32 != 0) {
            throw new ArithmeticException("unsigned integer overflow");
        }
        return (int)r;
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static long unsignedMultiplyExact(long x, int y) {
        return unsignedMultiplyExact(x, y & 0xFFFF_FFFFL);
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static long unsignedMultiplyExact(long x, long y) {
        long l = x * y;
        long h = unsignedMultiplyHigh(x, y);
        if (h == 0) {
            return l;
        }
        throw new ArithmeticException("unsigned long overflow");
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static int unsignedPowExact(int x, int n) {
        if (n < 0) {
            throw new ArithmeticException("negative exponent");
        } else if (n == 0) {
            return 1;
        }
        if (x == 0 || x == 1) {
            return x;
        }

        int p = 1;
        while (n > 1) {
            if ((n & 0b1) != 0) {
                p *= x;
            }
            x = unsignedMultiplyExact(x, x);
            n >>>= 1;
        }
        return unsignedMultiplyExact(p, x);
    }

    @Stub(ref = @Ref("Ljava/lang/Math;"))
    public static long unsignedPowExact(long x, int n) {
        if (n < 0) {
            throw new ArithmeticException("negative exponent");
        } else if (n == 0) {
            return 1;
        }
        if (x == 0 || x == 1) {
            return x;
        }

        long p = 1;
        while (n > 1) {
            if ((n & 0b1) != 0) {
                p *= x;
            }
            x = unsignedMultiplyExact(x, x);
            n >>>= 1;
        }
        return unsignedMultiplyExact(p, x);
    }
    
}
