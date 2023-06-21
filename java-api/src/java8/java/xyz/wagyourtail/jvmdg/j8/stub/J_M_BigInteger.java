package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.math.BigInteger;

public class J_M_BigInteger {

    @Stub(opcVers = Opcodes.V1_8)
    public static long longValueExact(BigInteger self) {
        // todo: check if mag length is 2 ints, so it's faster because bitlength is a bit slow
        if (self.bitLength() <= 63) {
            return self.longValue();
        } else {
            throw new ArithmeticException("BigInteger out of long range");
        }
    }

    @Stub(opcVers = Opcodes.V1_8)
    public static int intValueExact(BigInteger self) {
        // todo: check if mag length is 1 int, so it's faster because bitlength is a bit slow
        if (self.bitLength() <= 31) {
            return self.intValue();
        } else {
            throw new ArithmeticException("BigInteger out of long range");
        }
    }

    @Stub(opcVers = Opcodes.V1_8)
    public static short shortValueExact(BigInteger self) {
        if (self.bitLength() <= 31) {
            int val = self.intValue();
            if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
                return (short) val;
            }
        }
        throw new ArithmeticException("BigInteger out of short range");
    }

    @Stub(opcVers = Opcodes.V1_8)
    public static byte byteValueExact(BigInteger self) {
        if (self.bitLength() <= 31) {
            int val = self.intValue();
            if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
                return (byte) val;
            }
        }
        throw new ArithmeticException("BigInteger out of byte range");
    }

}
