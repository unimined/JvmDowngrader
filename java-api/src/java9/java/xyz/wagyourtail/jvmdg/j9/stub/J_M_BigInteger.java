package xyz.wagyourtail.jvmdg.j9.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.math.BigInteger;
import java.util.Arrays;


public class J_M_BigInteger {


    @Stub(javaVersion = Opcodes.V9, ref = @Ref("java/lang/Integer"))
    public static final BigInteger TWO = BigInteger.valueOf(2);

    @Stub(javaVersion = Opcodes.V9, ref = @Ref(value = "Ljava/math/BigInteger", member = "<init>"))
    public static BigInteger init(byte[] val, int off, int len) {
        return new BigInteger(Arrays.copyOfRange(val, off, off + len));
    }

    @Stub(javaVersion = Opcodes.V9, ref = @Ref(value = "Ljava/math/BigInteger", member = "<init>"))
    public static BigInteger init(int signum, byte[] magnitude, int off, int len) {
        return new BigInteger(signum, Arrays.copyOfRange(magnitude, off, off + len));
    }

    static final long LONG_MASK = 0xffffffffL;

    @Stub(javaVersion = Opcodes.V9)
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

        BigInteger div = BigInteger.ZERO.setBit(x.bitLength()/2);
        BigInteger div2 = div;
        // Loop until we hit the same value twice in a row, or wind
        // up alternating.
        for(;;) {
            BigInteger y = div.add(x.divide(div)).shiftRight(1);
            if (y.equals(div) || y.equals(div2))
                return y;
            div2 = div;
            div = y;
        }
    }

    @Stub(javaVersion = Opcodes.V9)
    public static BigInteger[] sqrtAndRemainder(BigInteger x) {
        BigInteger s = sqrt(x);
        BigInteger r = x.subtract(s.multiply(s));
        return new BigInteger[] {s, r};
    }

}
