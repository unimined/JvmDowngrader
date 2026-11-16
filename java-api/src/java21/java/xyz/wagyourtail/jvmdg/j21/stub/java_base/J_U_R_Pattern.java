package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class J_U_R_Pattern {

    @Stub
    public static String[] splitWithDelimiters(Pattern pattern, CharSequence str, int limit) {
        // empty input produces an empty result (like String.split)
        if (str.length() == 0) {
            return new String[0];
        }
        List<String> list = new ArrayList<>();
        // limit bounds the number of non-delimiter segments
        int segments = 0;
        int start = 0;
        Matcher m = pattern.matcher(str);
        while (m.find()) {
            if (start == 0 && m.start() == 0 && m.end() == 0) {
                // a zero-width match at the very start produces no leading
                // empty segment and no delimiter
                continue;
            }
            if (limit > 0 && segments + 1 == limit) {
                break;
            }
            list.add(str.subSequence(start, m.start()).toString());
            list.add(str.subSequence(m.start(), m.end()).toString());
            start = m.end();
            segments++;
        }
        if (start != str.length() || !list.isEmpty()) {
            // a delimiter at the end still contributes a trailing empty
            // segment when limit != 0
            list.add(str.subSequence(start, str.length()).toString());
        }
        if (limit == 0) {
            // like split(): trailing empty strings are removed
            for (int i = list.size() - 1; i >= 0 && list.get(i).isEmpty(); i--) {
                list.remove(i);
            }
        }
        return list.toArray(new String[0]);
    }

}
