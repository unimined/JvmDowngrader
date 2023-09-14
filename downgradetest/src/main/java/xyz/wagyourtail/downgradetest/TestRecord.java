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
//        System.out.println(Record.class);
        RecordComponent[] components = TestRecord.class.getRecordComponents();
        System.out.println(components[0].getName());
        System.out.println(components[1].getGenericSignature());
        System.out.println(components[2].getType());
        System.out.println(components[3].getGenericType().getTypeName());
        System.out.println(components[0].toString());
        System.out.println(components[3].getGenericSignature());
        System.out.println(components[3].getAnnotatedType().getType());
        System.out.println(components[3].getAccessor());
    }

}
