package xyz.wagyourtail.downgradetest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestMatch {

    public static void main(String[] args) {
        String a = "wad (aaa), (bb$), (ccd) dddd";
        Matcher m = Pattern.compile("\\(([^)]+)(?<last>[^)])\\)").matcher(a);
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        while (m.find()) {
            m.appendReplacement(sb, "$104[${last}]");
        }
        m.appendTail(sb);
        System.out.println(sb);

        Matcher m2 = Pattern.compile("\\^").matcher(a);
        System.out.println(m2.replaceFirst("^^"));
    }
}
