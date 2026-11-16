package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.lang.reflect.RecordComponent;
import java.util.function.Consumer;

/**
 * Records: ACC_RECORD + Record attribute are stripped on <=j15 and serialized
 * into a {@code @RecordComponents} annotation — reflection must keep answering
 * isRecord()/getRecordComponents(). Covers float/double equals semantics
 * (NaN, -0.0), small-primitive accessor types, generic signatures, compact
 * and canonical ctors.
 */
public class Records {

    public static void main(String[] args) {
        Support.run(Records.class);
    }

    record TestRecord(int a, String b, char c, Consumer<Integer> d) {
    }

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

    record Compact(int a) {
        Compact {
            a = a * 2;
        }
    }

    record Canon(int a) {
        Canon(int a) {
            this.a = a + 1;
        }
    }

    @FeatureTest
    static void basics() {
        // equals/toString/hashCode — toString output must match the JDK's
        // simple-name formatting (regression: binary names leaked earlier)
        TestRecord2 or = new TestRecord2(1, "2,=", '3');
        System.out.println(or);
        TestRecord2 tr = new TestRecord2(1, "2,=", '3');
        System.out.println(tr);
        System.out.println(or.equals(tr));
        TestRecord2 tr2 = new TestRecord2(1, "2,=", '4');
        System.out.println(tr2);
        System.out.println(or.equals(tr2));
    }

    @FeatureTest
    static void smallTypes() {
        // float/double component equals semantics (NaN equal, -0.0 != 0.0)
        // + byte/short/char/long accessor types — J_L_R_ObjectMethods paths
        System.out.println(new TestRecordFloat(Float.NaN).equals(new TestRecordFloat(Float.NaN)));
        System.out.println(new TestRecordFloat(0.0f).equals(new TestRecordFloat(-0.0f)));
        System.out.println(new TestRecordFloat(1.0f).equals(new TestRecordFloat(1.0f)));
        System.out.println(new TestRecordDouble(Double.NaN).equals(new TestRecordDouble(Double.NaN)));
        System.out.println(new TestRecordDouble(0.0d).equals(new TestRecordDouble(-0.0d)));
        System.out.println(new TestRecordDouble(1.0d).equals(new TestRecordDouble(1.0d)));

        System.out.println(new TestRecordByte((byte) 7));
        System.out.println(new TestRecordShort((short) 9));
        System.out.println(new TestRecordChar('x', true, 5L));
    }

    @FeatureTest
    static void reflection() {
        // isRecord/getRecordComponents incl. generic signature, annotated
        // type, and accessor handle of a Consumer component
        System.out.println(TestRecord.class.isRecord());
        System.out.println(String.class.isRecord());
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

    @FeatureTest
    static void ctors() {
        // compact ctor (implicit param assign) vs explicit canonical ctor
        System.out.println(new Compact(5).a());
        System.out.println(new Canon(5).a());
    }

    @FeatureTest
    static void accessors() {
        // accessors + hashCode — hashCode ignores lambda identity, so two
        // equal-component records must hash identically
        TestRecord r = new TestRecord(1, "two", '3', i -> {
        });
        System.out.println(r.a() + ":" + r.b() + ":" + r.c());
        System.out.println(r.hashCode() == new TestRecord(1, "two", '3', i -> {
        }).hashCode());
    }
}
