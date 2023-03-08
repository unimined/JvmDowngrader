package xyz.wagyourtail.downgradetest;

public class TestClass {

    public static void main(String[] args) {
        System.out.println(String.class.descriptorString());
        System.out.println(int.class.descriptorString());
        System.out.println(Object[][][].class.componentType());
        System.out.println(Object[].class.arrayType());
    }

}
