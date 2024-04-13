package xyz.wagyourtail.downgradetest;

public class TestSwitch {


    public static void main(String[] args) {
        switch (TestEnum.C) {
            case TestEnum.A -> System.out.println("A");
            case TestEnum.C -> System.out.println("C");
            case TestEnum b -> System.out.println("B");
        }

        switch (TestEnum.B) {
            case A -> System.out.println("A");
            case C -> System.out.println("C");
            default -> System.out.println("B");
        }

        switch ("test") {
            case "a" -> System.out.println("A");
            case "test" -> System.out.println("B");
            case "c" -> System.out.println("C");
            case "d" -> System.out.println("D");
            case String s -> System.out.println(s);
        }

        switch ("test") {
            case "a" -> System.out.println("A");
            case "test" -> System.out.println("B");
            case "c" -> System.out.println("C");
            case "d" -> System.out.println("D");
        }

        switch ((TestInterface) TestEnum.B) {
            case TestEnum.A -> System.out.println("A");
            case TestEnum ignored -> System.out.println("B");
            case TestClass tc -> System.out.println("TestClass");
            default -> System.out.println("default");
        }

        switch ((Character) '\u0167') {
            case 1000 -> System.out.println("10");
            case 'c' -> System.out.println("c");
            default -> System.out.println("default");
        }
    }

    enum TestEnum implements TestInterface {
        A,
        B,
        C
    }

    sealed interface TestInterface permits TestClass, TestEnum {

    }

    final static class TestClass implements TestInterface {

    }

}
