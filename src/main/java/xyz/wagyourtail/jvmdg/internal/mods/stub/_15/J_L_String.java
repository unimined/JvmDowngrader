package xyz.wagyourtail.jvmdg.internal.mods.stub._15;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_String {

    @Stub(value = JavaVersion.VERSION_15)
    public static String stripIndent(String str) {
        int len = str.length();
        if (len == 0) {
            return "";
        }
        // split into lines
        String[] lines = str.split("\r\n|\r|\n", -1);
        int minIndent = Integer.MAX_VALUE;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // skip empty lines, except for last
            if (line.isEmpty() && i < lines.length - 1) {
                continue;
            }
            // find whitespace count on left
            int indent = 0;
            while (indent < line.length() && Character.isWhitespace(line.charAt(indent))) {
                indent++;
            }
            if (indent < minIndent) {
                minIndent = indent;
            }
        }
        // remove whitespace from left
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty() && i < lines.length - 1) {
                sb.append("\n");
            } else {
                // rstrip whitespace as well
                line = line.stripTrailing();
                sb.append(line.substring(minIndent));
                if (i < lines.length - 1) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    @Stub(value = JavaVersion.VERSION_15)
    public static String translateEscapes(String str) {
        if (str.isEmpty()) {
            return str;
        }
        char[] chars = str.toCharArray();
        int j = 0;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '\\') {
                if (i + 1 < chars.length) {
                    char d = chars[++i];
                    switch (d) {
                        case 'b' -> c = '\b';
                        case 'f' -> c = '\f';
                        case 'n' -> c = '\n';
                        case 'r' -> c = '\r';
                        case 's' -> c = ' ';
                        case 't' -> c = '\t';
                        case '\'', '\"', '\\', '\n', '\r' -> c = d;
                        case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                            int limit = Math.min(d < '4' ? 2 : 1, chars.length);
                            int code = d - '0';
                            for (int k = 1; k < limit; k++) {
                                char e = chars[i + 1];
                                if (e >= '0' && e <= '9') {
                                    code = code * 10 + e - '0';
                                    i++;
                                }
                            }
                            c = (char) code;
                        }
                        default -> throw new IllegalArgumentException(String.format("Invalid escape sequence: \\%c \\\\u%04X", d, (int) d));
                    }
                } else {
                    throw new IllegalArgumentException("Invalid escape sequence: \\EOS");
                }
            }
            chars[j++] = c;
        }
        return new String(chars, 0, j);
    }

    @Stub(value = JavaVersion.VERSION_15)
    public static String formatted(String str, Object... args) {
        return String.format(str, args);
    }
}
