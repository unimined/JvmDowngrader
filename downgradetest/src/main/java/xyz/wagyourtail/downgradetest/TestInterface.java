package xyz.wagyourtail.downgradetest;

import java.util.function.Function;

public interface TestInterface {
    String test5 = "test";


    static void main(String[] args) {
        new TestInterface() {
        }.test2();
        System.out.println(TestInterface.test5);
        test3();
    }

    static void test3() {
        System.out.println("test3" + 4);

        Function<Long, IndexOutOfBoundsException> IOOBE = IndexOutOfBoundsException::new;
        IOOBE.apply(0L);

        switch (TestSwitch.TestEnum.A) {
            case A:
                System.out.println("A");
                break;
            case B:
                System.out.println("B");
                break;
            case TestSwitch.TestEnum ignored:
                System.out.println("ignored");
                break;
        }
    }

    default void test2() {
        test();
    }

    private void test() {
        System.out.println("test");
    }

}
