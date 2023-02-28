package xyz.wagyourtail.jvmdg.internal.mods.stub._11;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

public class J_L_String {

    @Stub(JavaVersion.VERSION_11)
    public static String stripTrailing(String str) {
        if (str.isBlank()) {
            return "";
        }
        int len = str.length();
        char[] val = str.toCharArray();
        while (len > 0 && Character.isWhitespace(val[len - 1])) {
            len--;
        }
        return new String(val, 0, len);
    }

    @Stub(JavaVersion.VERSION_11)
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
}
