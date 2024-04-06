package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.regex.Pattern;

public class J_L_String {

    @Stub
    public static int indexOf(String self, int ch, int fromIndex, int toIndex) {
        int idx = self.indexOf(ch, fromIndex);
        return idx == -1 || idx >= toIndex ? -1 : idx;
    }

    @Stub
    public static int indexOf(String self, String str, int fromIndex, int toIndex) {
        int idx = self.indexOf(str, fromIndex);
        return idx == -1 || idx > (toIndex - str.length()) ? -1 : idx;
    }

    @Stub
    public static String[] splitWithDelimiters(String self, String regex, int limit) {
        return J_U_R_Pattern.splitWithDelimiters(Pattern.compile(regex), self, limit);
    }

}
