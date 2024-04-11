package xyz.wagyourtail.jvmdg.j21.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class J_U_R_Pattern {

    @Stub
    public static String[] splitWithDelimiters(Pattern pattern, CharSequence str, int limit) {
        List<String> list = new ArrayList<>();
        int count = 0;
        int start = 0;
        Matcher m = pattern.matcher(str);
        while (m.find()) {
            count++;
            if (count == limit) {
                break;
            }
            list.add(str.subSequence(start, m.start()).toString());
            list.add(str.subSequence(m.start(), m.end()).toString());
            start = m.end();
        }
        if (start != str.length() && limit != 0) {
            list.add(str.subSequence(start, str.length()).toString());
        }
        return list.toArray(new String[0]);
    }

}
