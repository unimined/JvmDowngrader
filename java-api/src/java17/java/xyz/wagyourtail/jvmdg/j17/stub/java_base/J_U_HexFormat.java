package xyz.wagyourtail.jvmdg.j17.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Adapter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.CharBuffer;
import java.util.Objects;

@Adapter("java/util/HexFormat")
public record J_U_HexFormat(String delimiter, String prefix, String suffix, char[] digits) {
    private static final char[] lowercase = "0123456789abcdef".toCharArray();
    private static final char[] uppercase = "0123456789ABCDEF".toCharArray();

    public static J_U_HexFormat of() {
        return new J_U_HexFormat("", "", "", lowercase);
    }

    public static J_U_HexFormat ofDelimiter(String delimiter) {
        return new J_U_HexFormat(delimiter, "", "", lowercase);
    }

    public J_U_HexFormat withDelimiter(String delimiter) {
        return new J_U_HexFormat(delimiter, prefix, suffix, digits);
    }

    public J_U_HexFormat withPrefix(String prefix) {
        return new J_U_HexFormat(delimiter, prefix, suffix, digits);
    }

    public J_U_HexFormat withSuffix(String suffix) {
        return new J_U_HexFormat(delimiter, prefix, suffix, digits);
    }

    public J_U_HexFormat withUpperCase() {
        return new J_U_HexFormat(delimiter, prefix, suffix, uppercase);
    }

    public J_U_HexFormat withLowerCase() {
        return new J_U_HexFormat(delimiter, prefix, suffix, lowercase);
    }

    public String formatHex(byte[] bytes) {
        return formatHex(bytes, 0, bytes.length);
    }

    public String formatHex(byte[] bytes, int offset, int toIndex) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.checkFromToIndex(offset, toIndex, bytes.length);
        if (toIndex == offset) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < toIndex; i++) {
            if (i != offset) {
                sb.append(delimiter);
            }
            sb.append(prefix);
            sb.append(digits[(bytes[i] >> 4) & 0xF]);
            sb.append(digits[bytes[i] & 0xF]);
            sb.append(suffix);
        }
        return sb.toString();
    }

    public <A extends Appendable> A formatHex(A out, byte[] bytes) {
        return formatHex(out, bytes, 0, bytes.length);
    }

    public <A extends Appendable> A formatHex(A out, byte[] bytes, int offset, int toIndex) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.checkFromToIndex(offset, toIndex, bytes.length);
        if (toIndex == offset) {
            return out;
        }
        try {
            for (int i = offset; i < toIndex; i++) {
                if (i != offset) {
                    out.append(delimiter);
                }
                out.append(prefix);
                out.append(digits[(bytes[i] >> 4) & 0xF]);
                out.append(digits[bytes[i] & 0xF]);
                out.append(suffix);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
        return out;
    }

    public byte[] parseHex(CharSequence string) {
        return parseHex(string, 0, string.length());
    }

    public byte[] parseHex(CharSequence string, int offset, int toIndex) {
        Objects.requireNonNull(string, "string");
        Objects.checkFromToIndex(offset, toIndex, string.length());
        if (offset == toIndex) {
            return new byte[0];
        }
        int len = toIndex - offset;
        // get length of each sequence
        long valueLength = prefix.length() + 2 + suffix.length();
        long stepLength = valueLength + delimiter.length();
        if ((string.length() - valueLength) % stepLength != 0) {
            throw new IllegalArgumentException("extra or missing delimiters or values consisting of prefix, two hexadecimal digits, and suffix");
        }
        // get number of bytes
        int byteCount = (int) ((len + delimiter.length()) / stepLength);
        byte[] bytes = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            int start = offset + (int) (i * stepLength);
            if (i != 0 && !delimiter.isEmpty()) {
                // check delimiter
                if (!string.subSequence(start - delimiter.length(), start).toString().equals(delimiter)) {
                    throw new IllegalArgumentException(("found: \"" + string.subSequence(start - delimiter.length(), start) + "\", expected: \"" + delimiter + "\", index: " + start).replace("\r", "\\r").replace("\n", "\\n"));
                }
            }
            // check prefix
            if (!prefix.isEmpty() && !string.subSequence(start, start + prefix.length()).toString().equals(prefix)) {
                throw new IllegalArgumentException(("found: \"" + string.subSequence(start, start + prefix.length()) + "\", expected: \"" + prefix + "\", index: " + start).replace("\r", "\\r").replace("\n", "\\n"));
            }
            // read byte
            int high = fromHexDigit(string.charAt(start + prefix.length()));
            int low = fromHexDigit(string.charAt(start + prefix.length() + 1));
            int value = (high << 4) | low;
            bytes[i] = (byte) value;
            // check suffix
            if (!suffix.isEmpty() && !string.subSequence(start + prefix.length() + 2, (int) (start + valueLength)).toString().equals(suffix)) {
                throw new IllegalArgumentException(("found: \"" + string.subSequence(start + prefix.length() + 2, (int) (start + valueLength)) + "\", expected: \"" + suffix + "\", index: " + start).replace("\r", "\\r").replace("\n", "\\n"));
            }
        }
        return bytes;
    }

    public byte[] parseHex(char[] chars, int offset, int toIndex) {
        Objects.requireNonNull(chars, "chars");
        Objects.checkFromToIndex(offset, toIndex, chars.length);
        if (offset == toIndex) {
            return new byte[0];
        }
        return parseHex(CharBuffer.wrap(chars, offset, toIndex - offset));
    }

    public char toLowHexDigit(int value) {
        return digits[value & 0xF];
    }

    public char toHighHexDigit(int value) {
        return digits[(value >> 4) & 0xF];
    }

    public <A extends Appendable> A toHexDigits(A out, byte value) {
        try {
            out.append(digits[(value >> 4) & 0xF]);
            out.append(digits[value & 0xF]);
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
        return out;
    }

    public String toHexDigits(byte value) {
        return new String(new char[]{
            digits[(value >> 4) & 0xF],
            digits[value & 0xF]
        });
    }

    public String toHexDigits(char value) {
        return toHexDigits((short) value);
    }

    public String toHexDigits(short value) {
        return new String(new char[]{
                digits[(value >> 12) & 0xF],
                digits[(value >> 8) & 0xF],
                digits[(value >> 4) & 0xF],
                digits[value & 0xF]
        });
    }

    public String toHexDigits(int value) {
        return new String(new char[]{
                digits[(value >> 28) & 0xF],
                digits[(value >> 24) & 0xF],
                digits[(value >> 20) & 0xF],
                digits[(value >> 16) & 0xF],
                digits[(value >> 12) & 0xF],
                digits[(value >> 8) & 0xF],
                digits[(value >> 4) & 0xF],
                digits[value & 0xF]
        });
    }

    public String toHexDigits(long value) {
        return new String(new char[]{
                digits[(int) ((value >> 60) & 0xF)],
                digits[(int) ((value >> 56) & 0xF)],
                digits[(int) ((value >> 52) & 0xF)],
                digits[(int) ((value >> 48) & 0xF)],
                digits[(int) ((value >> 44) & 0xF)],
                digits[(int) ((value >> 40) & 0xF)],
                digits[(int) ((value >> 36) & 0xF)],
                digits[(int) ((value >> 32) & 0xF)],
                digits[(int) ((value >> 28) & 0xF)],
                digits[(int) ((value >> 24) & 0xF)],
                digits[(int) ((value >> 20) & 0xF)],
                digits[(int) ((value >> 16) & 0xF)],
                digits[(int) ((value >> 12) & 0xF)],
                digits[(int) ((value >> 8) & 0xF)],
                digits[(int) ((value >> 4) & 0xF)],
                digits[(int) (value & 0xF)]
        });
    }

    public String toHexDigits(long value, int digits) {
        if (digits < 0 || digits > 16) {
            throw new IllegalArgumentException("number of digits: " + digits);
        }
        if (digits == 0) {
            return "";
        }
        char[] chars = new char[digits];
        for (int i = digits - 1; i >= 0; i--) {
            chars[i] = this.digits[(int) (value & 0xF)];
            value >>= 4;
        }
        return new String(chars);
    }

    public static boolean isHexDigit(int ch) {
        return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
    }

    public static int fromHexDigit(int ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }
        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }
        throw new IllegalArgumentException("not a hexadecimal digit: " + ch);
    }

    public static int fromHexDigits(CharSequence string) {
        return fromHexDigits(string, 0, string.length());
    }

    public static int fromHexDigits(CharSequence string, int offset, int toIndex) {
        Objects.requireNonNull(string, "string");
        Objects.checkFromToIndex(offset, toIndex, string.length());
        if (offset == toIndex) {
            throw new IllegalArgumentException("empty string");
        }
        int value = 0;
        for (int i = offset; i < toIndex; i++) {
            value <<= 4;
            value |= fromHexDigit(string.charAt(i));
        }
        return value;
    }

    public static long fromHexDigitsToLong(CharSequence string) {
        return fromHexDigitsToLong(string, 0, string.length());
    }

    public static long fromHexDigitsToLong(CharSequence string, int offset, int toIndex) {
        Objects.requireNonNull(string, "string");
        Objects.checkFromToIndex(offset, toIndex, string.length());
        if (offset == toIndex) {
            throw new IllegalArgumentException("empty string");
        }
        long value = 0;
        for (int i = offset; i < toIndex; i++) {
            value <<= 4;
            value |= fromHexDigit(string.charAt(i));
        }
        return value;
    }

}
