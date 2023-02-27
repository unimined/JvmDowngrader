package xyz.wagyourtail.downgradetest;

import java.lang.reflect.RecordComponent;
import java.util.function.Consumer;

record TestRecord2(int a, String b, char c) {
}

public record TestRecord(int a, String b, char c, Consumer<Integer> d) {


    public static void main(String[] args) {
        TestRecord2 or = new TestRecord2(1, "2,=", '3');
        System.out.println(or);
        // get record components
        RecordComponent[] components = TestRecord.class.getRecordComponents();
        System.out.println(components[0].getName());
        System.out.println(components[1].getGenericSignature());
        System.out.println(components[2].getType());
        System.out.println(components[3].getGenericType());
        System.out.println(components[0].toString());
    }
}
