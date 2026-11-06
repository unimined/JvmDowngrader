package xyz.wagyourtail.downgradetest;

import java.lang.reflect.RecordComponent;
import java.util.function.Consumer;

record TestRecord2(int a, String b, char c) {
}

record TestRecordFloat(float x) {
}

record TestRecordDouble(double x) {
}

record TestRecordByte(byte x) {
}

record TestRecordShort(short x) {
}

record TestRecordChar(char x, boolean z, long l) {
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
        TestRecord2 tr = new TestRecord2(1, "2,=", '3');
        System.out.println(tr);
        System.out.println(or.equals(tr));
        TestRecord2 tr2 = new TestRecord2(1, "2,=", '4');
        System.out.println(tr2);
        System.out.println(or.equals(tr2));

        // Floating components should Float/Double.compare semantics
        System.out.println(new TestRecordFloat(Float.NaN).equals(new TestRecordFloat(Float.NaN)));
        System.out.println(new TestRecordFloat(0.0f).equals(new TestRecordFloat(-0.0f)));
        System.out.println(new TestRecordFloat(1.0f).equals(new TestRecordFloat(1.0f)));
        System.out.println(new TestRecordDouble(Double.NaN).equals(new TestRecordDouble(Double.NaN)));
        System.out.println(new TestRecordDouble(0.0d).equals(new TestRecordDouble(-0.0d)));
        System.out.println(new TestRecordDouble(1.0d).equals(new TestRecordDouble(1.0d)));

        // Printing byte/short/char records should all work
        System.out.println(new TestRecordByte((byte) 7));
        System.out.println(new TestRecordShort((short) 9));
        System.out.println(new TestRecordChar('x', true, 5L));
    }

}
