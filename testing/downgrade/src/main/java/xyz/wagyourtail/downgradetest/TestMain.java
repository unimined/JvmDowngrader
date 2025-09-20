package xyz.wagyourtail.downgradetest;

public class TestMain {

    TestMain() {
        System.out.println("TestMainConstructor");
        super();
    }

    void main() {
        IO.println(this.getClass().getSimpleName());
    }

    @xyz.wagyourtail.TestMain
    public static class InheritedMain extends TestMain {

    }

    public static class TestMain2 {

        void main(String[] args) {
            IO.println(this.getClass().getSimpleName());
        }

    }

    @xyz.wagyourtail.TestMain
    public static class InheritedMain2 extends TestMain2 {

        @Override
        void main(String[] args) {
            super.main(args);
            IO.println("inherited");
        }

    }

}
