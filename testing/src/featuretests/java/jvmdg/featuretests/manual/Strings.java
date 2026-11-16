package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * String compile-time/runtime forms: StringConcatFactory indy shapes (mixed
 * constants/args/null), text blocks + stripIndent/translateEscapes, static
 * init concat, chars() iteration, compareTo, formatted.
 */
public class Strings {

    static {
        String a = "aaa";
        System.out.println("testa " + a);
        System.out.println("testb " + a);
    }

    public static void main(String[] args) {
        Support.run(Strings.class);
    }

    @FeatureTest
    static void concat(String id) {
        // StringConcatFactory indy shapes: mixed constant/arg recipes, a null
        // arg, and non-String args — each recipe is a distinct indy site
        String a = "1";
        double b = 2.000001d;
        String c = null;
        String withConstants = a + b + c + "wasd" + Arrays.asList(new String[0]);
        System.out.println(withConstants);

        String withConstantsToo = a + b + c + "asdf" + Arrays.asList(new String[0]);
        System.out.println(withConstantsToo);

        System.out.println("aaa" + b + "bbb" + Arrays.asList(new String[0]) + a);

        System.out.println(a + b + "ccc");
    }

    @FeatureTest
    static void textBlocks() {
        // text-block constant + stripIndent, translateEscapes on an escaped
        // literal, formatted — j15/j13 String members via indy'd constants
        System.out.println("""
                This is a test
              \t \tof the new multiline
                string literal
            """.stripIndent());

        System.out.println("this is a \\t test \\\" \\n ".translateEscapes());
        System.out.println("test %s".formatted("test2"));
    }

    @FeatureTest
    static void misc() {
        // compareTo on String vs StringBuffer, chars() iteration, repeat —
        // a few j9+ String members mixed with native ones as a smoke baseline
        System.out.println("was".compareTo("wasd"));
        System.out.println(new StringBuffer("wasd").compareTo(new StringBuffer("was")));
        var chars = "wasd".chars();
        for (var i : chars.toArray()) {
            System.out.print(Character.toChars(i));
        }
        System.out.println();
        System.out.println("x".repeat(3));
        System.out.println(String.join("-", "a", "b"));
        System.out.println(Charset.defaultCharset().name().length() > 0);
    }
}
