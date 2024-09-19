package xyz.wagyourtail.downgradetest;


import java.util.function.Function;
import java.util.function.Supplier;

public class TestLambda {

    public static String test9 = "test9";
    public String test11 = "test11";

    public static void main(String[] args) {
        Supplier<String> provider = () -> "test";
        System.out.println(provider.get());
        Supplier<String> provider2 = TestLambda::test2;
        System.out.println(provider2.get());
        TestLambda test = new TestLambda();
        Supplier<String> provider3 = test::test3;
        System.out.println(provider3.get());
        Supplier<String> provider4 = new TestLambda2();
        System.out.println(provider4.get());
        String test5 = "test5";
        testSupplier(() -> test5);
        Function<String, String> test6 = (s) -> s + test5;
        System.out.println(test6.apply("test6"));
        Function<String, String> test7 = (s) -> s;
        System.out.println(test7.apply("test7"));
        Function<String, String> test8 = new Function<>() {
            @Override
            public String apply(String s) {
                return s + test5 + test9;
            }
        };
        System.out.println(test8.apply("test8"));

        TestInterface<String, String> at = (a) -> a + ".5";
        TestInterface<String, Double> bt = Double::parseDouble;
        TestInterface<String, Double> ct = at.andThen(bt);
        System.out.println(ct.get("1"));
    }

    public static String test2() {
        return "test2";
    }

    public static void testSupplier(Supplier<String> supplier) {
        System.out.println(supplier.get());
    }

    public void test10() {
        Function<String, String> test10 = new Function<>() {
            @Override
            public String apply(String s) {
                return s + test9 + test11;
            }
        };
        System.out.println(test10.apply("test10"));
        Function<String, String> test12 = (s) -> s + test9 + test11;
        System.out.println(test12.apply("test12"));

        Function<String, String> test13 = test10::apply;
        System.out.println(test13.apply("test20"));
    }

    public String test3() {
        return "test3";
    }

    public interface TestInterface<A, B> {

        B get(A a);

        default <C> TestInterface<A, C> andThen(TestInterface<B, C> after) {
            return (a) -> after.get(get(a));
        }

    }

    public static class TestLambda2 implements Supplier<String> {
        public String get() {
            return "test4";
        }

    }

}
