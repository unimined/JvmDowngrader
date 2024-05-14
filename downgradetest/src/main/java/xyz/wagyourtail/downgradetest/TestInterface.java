package xyz.wagyourtail.downgradetest;

public interface TestInterface {
    String test5 = "test";


    static void main(String[] args) {
        new TestInterface() {
        }.test2();
        System.out.println(TestInterface.test5);
        test3();
    }

    default void test2() {
        test();
    }

    private void test() {
        System.out.println("test");
    }

    static void test3() {
        System.out.println("test3");
    }

}
