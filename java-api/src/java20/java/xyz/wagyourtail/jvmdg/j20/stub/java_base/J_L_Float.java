package xyz.wagyourtail.jvmdg.j20.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_Float {

    @Stub(ref = @Ref("java/lang/Float"))
    public static float float16ToFloat(short floatBinary16) {
        int bin16SignBit = 0x8000 & (int) floatBinary16;
        int bin16ExpBits = 0x7c00 & (int) floatBinary16;
        int bin16SignifBits = 0x03FF & (int) floatBinary16;

        final int SIGNIF_SHIFT = 13;

        float sign = (bin16SignBit != 0) ? -1.0f : 1.0f;

        int bin16Exp = (bin16ExpBits >> 10) - 15;
        if (bin16Exp == -15) {
            return sign * (0x1p-24f * bin16SignifBits);
        } else if (bin16Exp == 16) {
            return (bin16SignifBits == 0) ?
                    sign * Float.POSITIVE_INFINITY :
                    Float.intBitsToFloat((bin16SignBit << 16) | 0x7f80_0000 | (bin16SignifBits << SIGNIF_SHIFT));
        }

        int floatExpBits = (bin16Exp + 127) << 23;
        return Float.intBitsToFloat((bin16SignBit << 16) | floatExpBits | (bin16SignifBits << SIGNIF_SHIFT));
    }

    @Stub(ref = @Ref("java/lang/Float"))
    public static short floatToFloat16(float f) {
        int doppel = Float.floatToRawIntBits(f);
        short sign_bit = (short) ((doppel & 0x8000_0000) >> 16);

        if (Float.isNaN(f)) {
            return (short) (sign_bit | 0x7c00 | (doppel & 0x007f_e000) >> 13 | (doppel & 0x0000_1ff0) >> 4 | (doppel & 0x0000_000f));
        }

        float abs_f = Math.abs(f);

        if (abs_f >= (0x1.ffcp15f + 0x0.002p15f)) {
            return (short) (sign_bit | 0x7c00);
        }

        if (abs_f <= 0x1.0p-24f * 0.5f) {
            return sign_bit;
        }

        int exp = Math.getExponent(f);
        assert -25 <= exp && exp <= 15;

        int expdelta = 0;
        int msb = 0x0000_0000;
        if (exp < -14) {
            expdelta = -14 - exp;
            exp = -15;
            msb = 0x0080_0000;
        }
        int f_signif_bits = doppel & 0x007f_ffff | msb;

        short signif_bits = (short) (f_signif_bits >> (13 + expdelta));

        int lsb = f_signif_bits & (1 << 13 + expdelta);
        int round = f_signif_bits & (1 << 12 + expdelta);
        int sticky = f_signif_bits & ((1 << 12 + expdelta) - 1);

        if (round != 0 && ((lsb | sticky) != 0)) {
            signif_bits++;
        }

        return (short) (sign_bit | (((exp + 15) << 10) + signif_bits));
    }

}
