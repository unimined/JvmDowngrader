package xyz.wagyourtail.jvmdg.j23.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

public class J_L_R_ExactConversionsSupport {

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isDoubleToByteExact(double value) {
        return value == (byte) value && isNotNegativeZero(value);
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isDoubleToCharExact(double value) {
        return value == (char) value && isNotNegativeZero(value);
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isDoubleToFloatExact(double value) {
        return value == (float) value && isNotNegativeZero(value);
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isDoubleToIntExact(double value) {
        return value == (int) value && isNotNegativeZero(value);
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isDoubleToLongExact(double value) {
        return value == (long) value && isNotNegativeZero(value);
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isDoubleToShortExact(double value) {
        return value == (short) value && isNotNegativeZero(value);
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isFloatToByteExact(float value) {
        return value == (byte) value && isNotNegativeZero(value);
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isFloatToCharExact(float value) {
        return value == (char) value && isNotNegativeZero(value);
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isFloatToIntExact(float value) {
        return value == (int) value && isNotNegativeZero(value);
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isFloatToLongExact(float value) {
        return value == (long) value && isNotNegativeZero(value);
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isFloatToShortExact(float value) {
        return value == (short) value && isNotNegativeZero(value);
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isIntToByteExact(int value) {
        return value == (byte) value;
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isIntToCharExact(int value) {
        return value == (char) value;
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isIntToFloatExact(int value) {
        return value == (float) value;
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isIntToShortExact(int value) {
        return value == (short) value;
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isLongToByteExact(long value) {
        return value == (byte) value;
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isLongToCharExact(long value) {
        return value == (char) value;
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isLongToDoubleExact(long value) {
        return value == (double) value;
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isLongToFloatExact(long value) {
        return value == (float) value;
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isLongToIntExact(long value) {
        return value == (int) value;
    }

    @Stub(ref = @Ref("java/lang/runtime/ExactConversionsSupport"))
    public static boolean isLongToShortExact(long value) {
        return value == (short) value;
    }

    private static boolean isNotNegativeZero(float value) {
        return Float.floatToRawIntBits(value) != Integer.MIN_VALUE;
    }

    private static boolean isNotNegativeZero(double value) {
        return Double.doubleToRawLongBits(value) != Long.MIN_VALUE;
    }

}
