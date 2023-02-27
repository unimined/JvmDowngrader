package xyz.wagyourtail.downgradetest;

public sealed class TestSeal permits TestSeal.TestSeal2, TestSeal.TestSeal3 {

    public void test() {
        System.out.println("test");
    }

    public static void main(String[] args) {
        TestSeal testSeal = new TestSeal2();
        testSeal.test();
        TestSeal testSeal2 = new TestSeal3();
        testSeal2.test();
        // get permitted subclasses with reflection
        for (Class<?> permittedSubclass : TestSeal.class.getPermittedSubclasses()) {
            System.out.println(permittedSubclass);
        }
        System.out.println(TestSeal.class.isSealed());
        System.out.println(TestSeal2.class.isSealed());
    }

    public static final class TestSeal2 extends TestSeal {

    }

    public static final class TestSeal3 extends TestSeal {
        @Override
        public void test() {
            System.out.println("test2");
        }

    }
}
