package xyz.wagyourtail.downgradetest;

public interface TestInterface {
    String test5 = "test";


    static void main(String[] args) {
        new TestInterface() {
        }.test2();
        System.out.println(TestInterface.test5);
    }

    default void test2() {
        test();
    }

    private void test() {
        System.out.println("test");
    }

}
