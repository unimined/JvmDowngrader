package xyz.wagyourtail.downgradetest;

import java.util.Arrays;
import java.util.HexFormat;

public class TestNumber {

    public static byte[] bytes(int... bytes) {
        byte[] ret = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            ret[i] = (byte) bytes[i];
        }
        return ret;
    }

    public static void main(String[] args) {
        HexFormat format = HexFormat.of();
        System.out.println(format.toHexDigits(0x1A2B3C4D));
        format = format.withUpperCase();
        System.out.println(format.toHexDigits(0x1A2B3C4D));
        format = format.withPrefix("0x");
        System.out.println(format.toHexDigits(0x1A2B3C4D));
        format = HexFormat.ofDelimiter(":");
        System.out.println(format.formatHex(bytes(0x1A, 0x2B, 0x3C, 0x4D)));
        format = HexFormat.ofDelimiter(", ").withPrefix("0x").withUpperCase();
        System.out.println(format.formatHex(bytes(0x1A, 0x2B, 0x3C, 0x4D)));
        System.out.println(Arrays.toString(format.parseHex("0x1A, 0x2B, 0x3C, 0x4D")));
        format = HexFormat.ofDelimiter(":").withUpperCase();
        System.out.println(Arrays.toString(format.parseHex("1A:2B:3C:4D")));
        format = HexFormat.ofDelimiter("-").withPrefix("0x").withSuffix("h");
        System.out.println(format.formatHex(bytes(0x1A, 0x2B, 0x3C, 0x4D)));
        System.out.println(Arrays.toString(format.parseHex("0x1Ah-0x2Bh-0x3Ch-0x4Dh")));
    }

}
