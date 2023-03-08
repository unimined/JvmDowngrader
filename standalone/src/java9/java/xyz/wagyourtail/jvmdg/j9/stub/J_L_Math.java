package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.math.BigDecimal;

public class J_L_Math {

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/lang/Math;"))
    public static long multiplyExact(long x, int y) {
        return Math.multiplyExact(x, (long) y);
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/lang/Math;"))
    public static long multiplyFull(int x, int y) {
        return (long) x * (long) y;
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/lang/Math;"))
    public static long multiplyHigh(long x, long y) {
        if (x < 0 || y < 0) {
            // Use technique from section 8-2 of Henry S. Warren, Jr.,
            // Hacker's Delight (2nd ed.) (Addison Wesley, 2013), 173-174.
            long x1 = x >> 32;
            long x2 = x & 0xFFFFFFFFL;
            long y1 = y >> 32;
            long y2 = y & 0xFFFFFFFFL;
            long z2 = x2 * y2;
            long t = x1 * y2 + (z2 >>> 32);
            long z1 = t & 0xFFFFFFFFL;
            long z0 = t >> 32;
            z1 += x2 * y1;
            return x1 * y1 + z0 + (z1 >> 32);
        } else {
            // Use Karatsuba technique with two base 2^32 digits.
            long x1 = x >>> 32;
            long y1 = y >>> 32;
            long x2 = x & 0xFFFFFFFFL;
            long y2 = y & 0xFFFFFFFFL;
            long A = x1 * y1;
            long B = x2 * y2;
            long C = (x1 + x2) * (y1 + y2);
            long K = C - A - B;
            return (((B >>> 32) + K) >>> 32) + A;
        }
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/lang/Math;"))
    public static long floorDiv(long x, int y) {
        return Math.floorDiv(x, (long) y);
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/lang/Math;"))
    public static int floorMod(long x, int y) {
        return (int) Math.floorMod(x, (long) y);
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/lang/Math;"))
    public static float fma(float a, float b, float c) {
        if (Float.isFinite(a) && Float.isFinite(b) && Float.isFinite(c)) {
            if (a == 0.0 || b == 0.0) {
                return a * b + c; // Handled signed zero cases
            } else {
                return (
                    new BigDecimal((double) a * (double) b) // Exact multiply
                        .add(new BigDecimal(c))
                )      // Exact sum
                    .floatValue();                            // One rounding
                // to a float value
            }
        } else {
            return (float) fma((double) a, (double) b, (double) c);
        }
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref("Ljava/lang/Math;"))
    public static double fma(double a, double b, double c) {
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isNaN(c)) {
            return Double.NaN;
        } else { // All inputs non-NaN
            boolean infiniteA = Double.isInfinite(a);
            boolean infiniteB = Double.isInfinite(b);
            boolean infiniteC = Double.isInfinite(c);
            double result;

            if (infiniteA || infiniteB || infiniteC) {
                if (infiniteA && b == 0.0 ||
                    infiniteB && a == 0.0) {
                    return Double.NaN;
                }
                double product = a * b;
                if (Double.isInfinite(product) && !infiniteA && !infiniteB) {
                    assert Double.isInfinite(c);
                    return c;
                } else {
                    result = product + c;
                    assert !Double.isFinite(result);
                    return result;
                }
            } else { // All inputs finite
                BigDecimal product = (new BigDecimal(a)).multiply(new BigDecimal(b));
                if (c == 0.0) {
                    if (a == 0.0 || b == 0.0) {
                        return a * b + c;
                    } else {
                        return product.doubleValue();
                    }
                } else {
                    return product.add(new BigDecimal(c)).doubleValue();
                }
            }
        }
    }

}
