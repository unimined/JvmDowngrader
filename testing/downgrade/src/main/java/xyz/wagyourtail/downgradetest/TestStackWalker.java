package xyz.wagyourtail.downgradetest;

import java.lang.reflect.InvocationTargetException;

public class TestStackWalker {

    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        test();
        TestInner.test();
    }

    public static void walkAccept(StackWalker.StackFrame frame) {
        if (!frame.getClassName().startsWith("xyz.wagyourtail.jvmdg")) {
            System.out.println(frame);
        }
    }

    public static void test() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        StackWalker walker = StackWalker.getInstance();
        walker.forEach(TestStackWalker::walkAccept);
        TestStackWalker.class.getDeclaredMethod("test2").invoke(null);
    }

    public static void test2() {
        StackWalker walker = StackWalker.getInstance();
        walker.walk(e -> {
            e.forEach(TestStackWalker::walkAccept);
            return null;
        });
    }

    public static class TestInner {

        public static void test() {
            StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            System.out.println(walker.getCallerClass());
        }

    }

}
