package xyz.wagyourtail.downgradetest;

import java.util.Arrays;

public class TestString {

    static {
        String a = "aaa";
        System.out.println("testa " + a);
        System.out.println("testb " + a);
    }

    public static void main(String[] args) {
        String a = "1";
        double b = 2.000001d;
        String c = null;
        String withConstants = a + b + c + "wasd" + Arrays.asList(args);
        System.out.println(withConstants);

        String withConstantsToo = a + b + c + "asdf" + Arrays.asList(args);
        System.out.println(withConstantsToo);

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

        byte by = 0b1010;
        short sh = 0b1010;
        int in = 0b1010;
        long lo = 0b1010;
        System.out.println(by + " short: " + sh + " int: " + in + " long: " + lo);
    }

}
