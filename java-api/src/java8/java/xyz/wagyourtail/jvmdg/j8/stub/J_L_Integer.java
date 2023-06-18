package xyz.wagyourtail.jvmdg.j8.stub;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.Ref;
import xyz.wagyourtail.jvmdg.stub.Stub;

public class J_L_Integer {

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static final int BYTES = Integer.SIZE / Byte.SIZE;

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static String toUnsignedString(int i, int radix) {
        return J_L_Long.toUnsignedString(i, radix);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static String toUnsignedString(int i) {
        return J_L_Long.toUnsignedString(i);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static int parseUnsignedInt(String s, int radix) throws NumberFormatException {
        if (s == null) {
            throw new NumberFormatException("Cannot parse null string");
        }

        int len = s.length();
        if (len > 0) {
            char first = s.charAt(0);
            if (first == '-') {
                throw new NumberFormatException(
                        String.format("Illegal leading minus sign on unsigned string %s.", s));
            } else {
                if (len <= 5 || (radix == 10 && len <= 9)) {
                    return Integer.parseInt(s, radix);
                } else {
                    long ell = Long.parseLong(s, radix);
                    if ((ell & 0xffffffff00000000L) == 0L) {
                        return (int) ell;
                    } else {
                        throw new NumberFormatException(
                                String.format("String value %s exceeds range of unsigned int.", s));
                    }
                }
            }
        } else {
            throw new NumberFormatException("Cannot parse empty string");
        }
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static int parseUnsignedInt(String s) throws NumberFormatException {
        return parseUnsignedInt(s, 10);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static int hashCode(int value) {
        return value;
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static int compareUnsigned(int x, int y) {
        return Integer.compare(x + Integer.MIN_VALUE, y + Integer.MIN_VALUE);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static long toUnsignedLong(int x) {
        return ((long) x) & 0xffffffffL;
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static int divideUnsigned(int dividend, int divisor) {
        return (int) (toUnsignedLong(dividend) / toUnsignedLong(divisor));
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static int remainderUnsigned(int dividend, int divisor) {
        return (int) (toUnsignedLong(dividend) % toUnsignedLong(divisor));
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static int sum(int a, int b) {
        return a + b;
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    @Stub(opcVers = Opcodes.V1_8, ref = @Ref("java/lang/Integer"))
    public static int min(int a, int b) {
        return Math.min(a, b);
    }

}
