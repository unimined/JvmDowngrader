package xyz.wagyourtail.downgradetest;

import java.util.Arrays;

public class TestString {

    public static void main(String[] args) {
        String a = "1";
        float b = 2.000001f;
        String c = null;
        String withConstants = a + b + c + "wasd" + Arrays.asList(args);
        System.out.println(withConstants);

        System.out.println("""
                This is a test
              \t \tof the new multiline
                string literal
            """.stripIndent());

        System.out.println("this is a \\t test \\\" \\n ".translateEscapes());
        System.out.println("test %s".formatted("test2"));

        System.out.println("was".compareTo("wasd"));
        System.out.println(new StringBuffer("wasd").compareTo(new StringBuffer("was")));
    }
}
