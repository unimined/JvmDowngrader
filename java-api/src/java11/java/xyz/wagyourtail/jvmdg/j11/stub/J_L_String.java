package xyz.wagyourtail.jvmdg.j11.stub;


import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.jvmdg.stub.Stub;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.stream.Stream;

public class J_L_String {

    @Stub(opcVers = Opcodes.V11)
    public static String strip(String str) {
        if (str.isEmpty()) {
            return "";
        }
        int len = str.length();
        char[] val = str.toCharArray();
        int st = 0;
        int ed = len;
        while (st < len && Character.isWhitespace(val[st])) {
            st++;
        }
        while (ed > st && Character.isWhitespace(val[ed - 1])) {
            ed--;
        }
        return ((st > 0) || (ed < len)) ? new String(val, st, ed - st) : str;
    }

    @Stub(opcVers = Opcodes.V11)
    public static String stripLeading(String str) {
        if (str.isEmpty()) {
            return "";
        }
        int len = str.length();
        char[] val = str.toCharArray();
        int st = 0;
        while (st < len && Character.isWhitespace(val[st])) {
            st++;
        }
        return (st > 0) ? new String(val, st, len - st) : str;
    }

    @Stub(opcVers = Opcodes.V11)
    public static String stripTrailing(String str) {
        if (str.isEmpty()) {
            return "";
        }
        int len = str.length();
        char[] val = str.toCharArray();
        int ed = len;
        while (ed > 0 && Character.isWhitespace(val[ed - 1])) {
            ed--;
        }
        return (ed < len) ? new String(val, 0, ed) : str;
    }

    @Stub(opcVers = Opcodes.V11)
    public static boolean isBlank(String str) {
        int len = str.length();
        if (len == 0) {
            return true;
        }
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Stub(opcVers = Opcodes.V11)
    public static Stream<String> lines(String str) {
        return new BufferedReader(new StringReader(str)).lines();
    }

    @Stub(opcVers = Opcodes.V11)
    public static String repeat(String str, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count is negative: " + count);
        }
        if (count == 1) {
            return str;
        }
        int len = str.length();
        if (len == 0 || count == 0) {
            return "";
        }
        if (Integer.MAX_VALUE / count < len) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        char[] buf = new char[len * count];
        for (int i = 0; i < count; i++) {
            str.getChars(0, len, buf, i * len);
        }
        return new String(buf);
    }

}
