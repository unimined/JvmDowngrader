package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex semantics: named groups, appendReplacement dollar/group escaping,
 * replaceFirst literal handling.
 */
public class Regex {

    public static void main(String[] args) {
        Support.run(Regex.class);
    }

    @FeatureTest
    static void replacement() {
        // appendReplacement with $104 group-index + ${named} refs; replaceFirst
        // on an anchored pattern — mid-iteration Matcher state
        String a = "wad (aaa), (bb$), (ccd) dddd";
        Matcher m = Pattern.compile("\\(([^)]+)(?<last>[^)])\\)").matcher(a);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, "$104[${last}]");
        }
        m.appendTail(sb);
        System.out.println(sb);

        Matcher m2 = Pattern.compile("\\^").matcher(a);
        System.out.println(m2.replaceFirst("^^"));
    }

    @FeatureTest
    static void literals() {
        // $ and \ in replacement must round-trip through quote/appendReplacement
        Matcher m = Pattern.compile("x").matcher("x y x");
        System.out.println(m.replaceAll(Matcher.quoteReplacement("$\\")));
        System.out.println("a$b".replaceAll("\\$", "\\\\\\$"));
    }
}
