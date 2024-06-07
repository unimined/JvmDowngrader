package xyz.wagyourtail.jvmdg.j12.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class J_L_String {

    @Stub
    public static String indent(String input, int n) {
        if (input.isEmpty()) {
            return "";
        }
        Stream<String> stream = input.lines();
        if (n > 0) {
            final String spaces = " ".repeat(n);
            stream = stream.map(s -> spaces + s);
        } else if (n == Integer.MIN_VALUE) {
            stream = stream.map(String::stripLeading);
        } else if (n < 0) {
            stream = stream.map(s -> s.substring(Math.min(-n, indexOfNonWhitespace(s))));
        }
        return stream.collect(Collectors.joining("\n", "", "\n"));
    }

    private static int indexOfNonWhitespace(String s) {
        int i = 0;
        int len = s.length();
        while (i < len && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }


    @Stub
    public static <R> R transform(String input, Function<? super String, ? extends R> func) {
        return func.apply(input);
    }

}
