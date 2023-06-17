package xyz.wagyourtail.downgradetest;

public class TestNests {

    private static String test = "test";
    private String test2 = "test2";


    public static void main(String[] args) {
        System.out.println(test);
        Nest1 nest1 = new Nest1();
        nest1.test();
        TestNests test = new TestNests();
        test.test2();
    }

    public void test2() {
        System.out.println(test2);
        Nest2 nest2 = new Nest2();
        nest2.test();
    }

    public static class Nest1 {
        public void test() {
            System.out.println(test);
        }
    }

    public class Nest2 {
        public void test() {
            System.out.println(test2);
        }
    }

}
