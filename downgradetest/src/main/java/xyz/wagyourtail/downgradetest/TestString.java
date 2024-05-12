package xyz.wagyourtail.downgradetest;

import java.util.Arrays;

public class TestString {

    public static void main(String[] args) {
        String a = "1";
        double b = 2.000001d;
        String c = null;
        String withConstants = a + b + c + "wasd" + Arrays.asList(args);
        System.out.println(withConstants);

        System.out.println("aaa" + b + "bbb" + Arrays.asList(args) + a);

        System.out.println(a + b + "ccc");

        System.out.println("""
                This is a test
              \t \tof the new multiline
                string literal
            """.stripIndent());

        System.out.println("this is a \\t test \\\" \\n ".translateEscapes());
        System.out.println("test %s".formatted("test2"));

        System.out.println("was".compareTo("wasd"));
        System.out.println(new StringBuffer("wasd").compareTo(new StringBuffer("was")));
        var chars = "wasd".chars();
        for (var i : chars.toArray()) {
            System.out.print(Character.toChars(i));
        }
        System.out.println();

        System.out.println(Character.isEmoji(0x1F600));
    }

}
