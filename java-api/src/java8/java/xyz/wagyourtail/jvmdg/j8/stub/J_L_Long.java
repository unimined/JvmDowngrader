package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.math.BigInteger;

public class J_L_Long {

    @Stub(ref = @Ref("java/lang/Long"))
    public static final int BYTES = Long.SIZE / Byte.SIZE;

    @Stub(ref = @Ref("java/lang/Long"))
    public static String toUnsignedString(long i, int radix) {
        if (i >= 0) {
            return Long.toString(i, radix);
        } else {
            switch (radix) {
                case 2:
                    return Long.toBinaryString(i);
                case 4:
                    return toUnsignedStringByBits(i, 2);
                case 8:
                    return Long.toOctalString(i);
                case 10:
                    long quot = (i >>> 1) / 5;
                    long rem = i - quot * 10;
                    return Long.toString(quot) + rem;
                case 16:
                    return Long.toHexString(i);
                case 32:
                    return toUnsignedStringByBits(i, 5);
            }
            return toUnsignedBigInteger(i).toString(radix);
        }
    }

    private static BigInteger toUnsignedBigInteger(long i) {
        if (i >= 0L)
            return BigInteger.valueOf(i);
        else {
            int upper = (int) (i >>> 32);
            int lower = (int) i;

            // return (upper << 32) + lower
            return (BigInteger.valueOf(J_L_Integer.toUnsignedLong(upper))).shiftLeft(32).
                    add(BigInteger.valueOf(J_L_Integer.toUnsignedLong(lower)));
        }
    }

    private static String toUnsignedStringByBits(long i, int bits) {
        int shift = 64 - bits;
        int radix = 1 << bits;
        long mask = radix - 1;
        char[] buf = new char[64 / bits + 1];
        int charPos = 64 / bits;
        do {
            buf[--charPos] = Character.forDigit((int) (i & mask), radix);
            i >>>= shift;
        } while (i != 0 && charPos > 0);
        return new String(buf, charPos, (64 / bits) - charPos);
    }

    @Stub(ref = @Ref("java/lang/Long"))
    public static String toUnsignedString(long i) {
        return toUnsignedString(i, 10);
    }

    @Stub(ref = @Ref("java/lang/Long"))
    public static long parseUnsignedLong(String s, int radix) throws NumberFormatException {
        if (s == null) {
            throw new NumberFormatException("Cannot parse null string");
        }
        int len = s.length();
        if (len > 0) {
            char firstChar = s.charAt(0);
            if (firstChar == '-') {
                throw new NumberFormatException(String.format("Illegal leading minus sign on unsigned string %s.", s));
            } else {
                if (len <= 12 || (radix == 10 && len <= 18)) {
                    return Long.parseLong(s, radix);
                } else {
                    BigInteger bigInteger = new BigInteger(s, radix);
                    if (bigInteger.signum() < 0 || bigInteger.bitLength() > 64) {
                        throw new NumberFormatException(String.format("String value %s exceeds range of unsigned long.", s));
                    } else {
                        return bigInteger.longValue();
                    }
                }
            }
        } else {
            throw new NumberFormatException(String.format("Illegal leading minus sign on unsigned string %s.", s));
        }
    }

    @Stub(ref = @Ref("java/lang/Long"))
    public static long parseUnsignedLong(String s) throws NumberFormatException {
        return parseUnsignedLong(s, 10);
    }

    @Stub(ref = @Ref("java/lang/Long"))
    public static int hashCode(long value) {
        return (int)(value ^ (value >>> 32));
    }

    @Stub(ref = @Ref("java/lang/Long"))
    public static int compareUnsigned(long x, long y) {
        return Long.compare(x + Long.MIN_VALUE, y + Long.MIN_VALUE);
    }

    @Stub(ref = @Ref("java/lang/Long"))
    public static long divideUnsigned(long dividend, long divisor) {
        if (divisor >= 0) {
            final long q = (dividend >>> 1) / divisor << 1;
            final long r = dividend - q * divisor;
            return q + ((r | ~(r - divisor)) >>> (Long.SIZE - 1));
        }
        return (dividend & ~(dividend - divisor)) >>> (Long.SIZE - 1);
    }

    @Stub(ref = @Ref("java/lang/Long"))
    public static long remainderUnsigned(long dividend, long divisor) {
        if (divisor >= 0) {
            final long q = (dividend >>> 1) / divisor << 1;
            final long r = dividend - q * divisor;
            return r - ((~(r - divisor) >> (Long.SIZE - 1)) & divisor);
        }
        return dividend - (((dividend & ~(dividend - divisor)) >> (Long.SIZE - 1)) & divisor);
    }

    @Stub(ref = @Ref("java/lang/Long"))
    public static long sum(long a, long b) {
        return a + b;
    }

    @Stub(ref = @Ref("java/lang/Long"))
    public static long max(long a, long b) {
        return Math.max(a, b);
    }

    @Stub(ref = @Ref("java/lang/Long"))
    public static long min(long a, long b) {
        return Math.min(a, b);
    }

}
