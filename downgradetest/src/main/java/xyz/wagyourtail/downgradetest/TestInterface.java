package xyz.wagyourtail.downgradetest;

public interface TestInterface {

    private void test() {
        System.out.println("test");
    }

    default void test2() {
        test();
    }

    static void main(String[] args) {
        new TestInterface() {
        }.test2();
    }
}
