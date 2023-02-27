package xyz.wagyourtail.downgradetest;

import java.util.Arrays;

public class TestString {

    public static void main(String[] args) {
        String a = "1";
        float b = 2.000001f;
        String c = null;
        String withConstants = a + b + c + "wasd" + Arrays.asList(args);
        System.out.println(withConstants);
    }
}
