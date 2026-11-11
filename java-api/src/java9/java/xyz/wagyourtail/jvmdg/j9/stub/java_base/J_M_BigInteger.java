package xyz.wagyourtail.jvmdg.j9.stub.java_base;


import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitPre;
import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.math.BigInteger;
import java.util.Arrays;


@InitRewrite(ref = @Ref(value = "Ljava/math/BigInteger;", member = "<init>", desc = "([BII)V"), to = "([B)V", keep = {-1})
@InitRewrite(ref = @Ref(value = "Ljava/math/BigInteger;", member = "<init>", desc = "(I[BII)V"), to = "(I[B)V", keep = {0, -1})
public class J_M_BigInteger {


    static final long LONG_MASK = 0xffffffffL;

    @Stub(ref = @Ref(value = "java/math/BigInteger", member = "TWO", desc = "Ljava/math/BigInteger;"))
    public static BigInteger getTwo() {
        return BigInteger.valueOf(2);
    }

    /**
     * Validation matching the real constructor: any out-of-range
     * offset/length produces {@link IndexOutOfBoundsException}, never
     * {@link IllegalArgumentException} (which {@code Arrays.copyOfRange}
     * would throw for {@code from > to}).
     */
    @InitPre(target = "([BII)V", from = {0, 1, 2})
    @InitPre(target = "(I[BII)V", from = {1, 2, 3})
    public static void checkRange(byte[] bytes, int offset, int len) {
        if (offset < 0 || len < 0 || len > bytes.length - offset) {
            throw new IndexOutOfBoundsException();
        }
    }

    // copyOfRange(b, off, off + len) — checkRange guards the from > to IllegalArgumentException case
    @InitArg(target = "([BII)V", slot = 0, from = {0, 1, 2})
    @InitArg(target = "(I[BII)V", slot = 1, from = {1, 2, 3})
    public static byte[] copyRange(byte[] bytes, int offset, int len) {
        return Arrays.copyOfRange(bytes, offset, offset + len);
    }

    @Stub
    public static BigInteger sqrt(BigInteger x) {
        // Special cases.
        // is zero
        if (x.signum() == 0) {
            return BigInteger.ZERO;
        } else if (x.signum() < 0) {
            throw new ArithmeticException("square root of negative number");
        } else if (x.equals(BigInteger.ONE)) {
            return BigInteger.ONE;
        }

        BigInteger div = BigInteger.ZERO.setBit(x.bitLength() / 2);
        BigInteger div2 = div;
        // Loop until we hit the same value twice in a row, or wind
        // up alternating.
        for (; ; ) {
            BigInteger y = div.add(x.divide(div)).shiftRight(1);
            if (y.equals(div) || y.equals(div2))
                return y;
            div2 = div;
            div = y;
        }
    }

    @Stub
    public static BigInteger[] sqrtAndRemainder(BigInteger x) {
        BigInteger s = sqrt(x);
        BigInteger r = x.subtract(s.multiply(s));
        return new BigInteger[]{s, r};
    }

}
