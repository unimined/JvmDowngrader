package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.lang.reflect.InvocationTargetException;

/**
 * StackWalker frame inspection. Frames are filtered to this package so
 * launcher/driver frames (which differ between direct-run, BatchMain, and
 * cli.Main bootstrap) never leak into the diff.
 */
public class StackWalking {

    public static void main(String[] args) {
        Support.run(StackWalking.class);
    }

    @FeatureTest
    static void walk() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        // forEach/walk/getCallerClass — frames filtered to this package so
        // launcher-dependant frames never enter the diff
        test();
        TestInner.test();
    }

    public static void walkAccept(StackWalker.StackFrame frame) {
        // only the corpus's own frames — driver frames differ per launch mode
        if (frame.getClassName().startsWith("jvmdg.featuretests.manual.")) {
            System.out.println(frame);
        }
    }

    public static void test() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        StackWalker walker = StackWalker.getInstance();
        walker.forEach(StackWalking::walkAccept);
        StackWalking.class.getDeclaredMethod("test2").invoke(null);
    }

    public static void test2() {
        StackWalker walker = StackWalker.getInstance();
        walker.walk(e -> {
            e.forEach(StackWalking::walkAccept);
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
