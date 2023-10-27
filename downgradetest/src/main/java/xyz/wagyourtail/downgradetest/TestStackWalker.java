package xyz.wagyourtail.downgradetest;

import java.lang.reflect.InvocationTargetException;

public class TestStackWalker {

    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        test();
    }



    public static void test() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        StackWalker walker = StackWalker.getInstance();
        walker.forEach(System.out::println);
        TestStackWalker.class.getDeclaredMethod("test2").invoke(null);
    }

    public static void test2() {
        StackWalker walker = StackWalker.getInstance();
        walker.walk(e -> {
            e.forEach(System.out::println);
            return null;
        });
    }

}
