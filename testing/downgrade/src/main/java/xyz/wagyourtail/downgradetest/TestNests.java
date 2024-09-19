package xyz.wagyourtail.downgradetest;

import java.util.Arrays;

public class TestNests {

    private static String test = "test";
    private String test2 = "test2";


    public static void main(String[] args) {
        System.out.println(test);
        Nest1 nest1 = new Nest1();
        nest1.test();
        TestNests test = new TestNests();
        test.test2();

        System.out.println(Nest1.class.getNestHost());
        System.out.println(Arrays.toString(Nest2.class.getNestMembers()));
        new NestChild().run3();
    }

    public void test2() {
        System.out.println(test2);
        Nest2 nest2 = new Nest2();
        nest2.test();
    }

    public void test3() {
        String test3 = "test3";
        Nest2.Nest3 nest3 = new Nest2().new Nest3();
        nest3.test();
        System.out.println(new Object() {

            @Override
            public String toString() {
                return test + test2 + test3;
            }

        });
    }

    private void test4() {
        System.out.println(test);
    }

    private void test5() {
        System.out.println(test2);
    }

    public static class Nest1 {
        private static String test5 = "test";

        private static String test11() {
            return test5;
        }

        public void test() {
            System.out.println(test);
        }

        private String test10() {
            return test5;
        }

    }

    public static class NestSuper1 {
        private String test = "testsuper";

        public void run() {
            new NestInner().run();
        }

        private void test() {
            System.out.println(test);
        }

        public class NestInner {
            private void run() {
                test();
                System.out.println(test);
            }

        }

    }

    public static class NestChild extends NestSuper1 {
        private String test = "testchild";

        public void run2() {
            new NestInner2().run();
        }

        public void run3() {
            super.run();
            run2();
        }

        private void test() {
            System.out.println(test);
        }

        public class NestInner2 {
            private void run() {
                test();
                System.out.println(test);
            }

        }

    }

    public class Nest2 {
        private static String test4 = "test";

        public void test() {
            System.out.println(test2 + Nest1.test5);

            Nest3 nest3 = new Nest3();
            nest3.test();
        }

        public class Nest3 {
            public void test() {
                System.out.println(test2 + Nest1.test5 + test4);
                Nest1 nest1 = new Nest1();
                System.out.println(nest1.test10());
                System.out.println(Nest1.test11());
                System.out.println(TestNests.this.test2);
            }

        }

    }

}
