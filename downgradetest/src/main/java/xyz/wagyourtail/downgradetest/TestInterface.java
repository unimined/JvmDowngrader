package xyz.wagyourtail.downgradetest;

public interface TestInterface {

    static void main(String[] args) {
        new TestInterface() {
        }.test2();
    }

    default void test2() {
        test();
    }

    private void test() {
        System.out.println("test");
    }

}
