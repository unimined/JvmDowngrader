package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

public class J_M_BigDecimal {

    @Stub
    public static BigDecimal sqrt(BigDecimal x, MathContext mc) {
        int signum = x.signum();
        if (signum == 1) {
            int preferredScale = x.scale() / 2;
            BigDecimal zeroWithFinalPreferredScale = BigDecimal.valueOf(0L, preferredScale);
            BigDecimal stripped = x.stripTrailingZeros();
            int strippedScale = stripped.scale();
            if (BigInteger.ONE.equals(x.unscaledValue()) &&
                    strippedScale % 2 == 0) {
                BigDecimal result = BigDecimal.valueOf(1L, strippedScale / 2);
                if (result.scale() != preferredScale) {
                    result = result.add(zeroWithFinalPreferredScale, mc);
                }
                return result;
            }
            int scaleAdjust;
            int scale = stripped.scale() - stripped.precision() + 1;
            if (scale % 2 == 0) {
                scaleAdjust = scale;
            } else {
                scaleAdjust = scale - 1;
            }
            BigDecimal working = stripped.scaleByPowerOfTen(scaleAdjust);
            assert BigDecimal.valueOf(1L, 1).compareTo(working) <= 0 && working.compareTo(BigDecimal.TEN) < 0;
            BigDecimal guess = BigDecimal.valueOf(Math.sqrt(working.doubleValue()));
            int guessPrecision = 15;
            int originalPrecision = mc.getPrecision();
            int targetPrecision;
            if (originalPrecision == 0) {
                targetPrecision = stripped.precision() / 2 + 1;
            } else {
                switch (mc.getRoundingMode()) {
                    case HALF_UP:
                    case HALF_DOWN:
                    case HALF_EVEN:
                        targetPrecision = 2 * originalPrecision;
                        if (targetPrecision < 0) // Overflow
                            targetPrecision = Integer.MAX_VALUE - 2;
                        break;

                    default:
                        targetPrecision = originalPrecision;
                        break;
                }
            }
            BigDecimal approx = guess;
            int workingPrecision = working.precision();
            do {
                int tmpPrecision = Math.max(Math.max(guessPrecision, targetPrecision + 2),
                        workingPrecision);
                MathContext mcTmp = new MathContext(tmpPrecision, RoundingMode.HALF_EVEN);
                // approx = 0.5 * (approx + fraction / approx)
                approx = BigDecimal.valueOf(5L, 1).multiply(approx.add(working.divide(approx, mcTmp), mcTmp));
                guessPrecision *= 2;
            } while (guessPrecision < targetPrecision + 2);

            BigDecimal result;
            RoundingMode targetRm = mc.getRoundingMode();
            if (targetRm == RoundingMode.UNNECESSARY || originalPrecision == 0) {
                RoundingMode tmpRm =
                        (targetRm == RoundingMode.UNNECESSARY) ? RoundingMode.DOWN : targetRm;
                MathContext mcTmp = new MathContext(targetPrecision, tmpRm);
                result = approx.scaleByPowerOfTen(-scaleAdjust / 2).round(mcTmp);
                if (x.subtract(result.multiply(result)).compareTo(ZERO) != 0) {
                    throw new ArithmeticException("Computed square root not exact.");
                }
            } else {
                result = approx.scaleByPowerOfTen(-scaleAdjust / 2).round(mc);
                int i = result.multiply(result).compareTo(x);
                switch (targetRm) {
                    case DOWN:
                    case FLOOR:
                        // Check if too big
                        if (i > 0) {
                            BigDecimal ulp = result.ulp();
                            if (approx.compareTo(ONE) == 0) {
                                ulp = ulp.multiply(BigDecimal.valueOf(1L, 1));
                            }
                            result = result.subtract(ulp);
                        }
                        break;
                    case UP:
                    case CEILING:
                        if (i < 0) {
                            result = result.add(result.ulp());
                        }
                        break;
                    default:
                        break;
                }
            }
            if (result.scale() != preferredScale) {
                result = result.stripTrailingZeros().
                        add(zeroWithFinalPreferredScale,
                                new MathContext(originalPrecision, RoundingMode.UNNECESSARY));
            }
            return result;
        } else {
            BigDecimal result;
            switch (signum) {
                case -1:
                    throw new ArithmeticException("Attempted square root " +
                            "of negative BigDecimal");
                case 0:
                    result = BigDecimal.valueOf(0L, x.scale() / 2);
                    return result;
                default:
                    throw new AssertionError("Bad value from signum");
            }
        }
    }
}
