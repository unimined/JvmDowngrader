package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Lambda/method-ref shapes: captures, method references (static, bound,
 * unbound, ctor, array-ctor, super, private), intersection-cast SAMs,
 * serializable lambdas, generic SAMs, nested lambdas.
 */
public class Lambdas {

    public static String test9 = "test9";
    public String test11 = "test11";

    public static void main(String[] args) {
        Support.run(Lambdas.class);
    }

    @FeatureTest
    static void forms() {
        // lambda vs static/instance method-ref vs anonymous-class impl —
        // every SAM materialization form javac can emit
        Supplier<String> provider = () -> "test";
        System.out.println(provider.get());
        Supplier<String> provider2 = Lambdas::staticMethod;
        System.out.println(provider2.get());
        Lambdas test = new Lambdas();
        Supplier<String> provider3 = test::instanceMethod;
        System.out.println(provider3.get());
        Supplier<String> provider4 = new Lambdas2();
        System.out.println(provider4.get());
    }

    @FeatureTest
    static void captures() {
        // effectively-final local + static-field capture through lambdas and
        // an anonymous impl — indy captured-arg shapes
        String test5 = "test5";
        testSupplier(() -> test5);
        Function<String, String> test6 = (s) -> s + test5;
        System.out.println(test6.apply("test6"));
        Function<String, String> test7 = (s) -> s;
        System.out.println(test7.apply("test7"));
        Function<String, String> test8 = new Function<>() {
            @Override
            public String apply(String s) {
                return s + test5 + test9;
            }
        };
        System.out.println(test8.apply("test8"));
    }

    @FeatureTest
    static void methodRefs() {
        // ctor ref, array-ctor ref, unbound instance ref, private refs through
        // nest accessors, super:: ref — the hard method-handle shapes
        // constructor ref
        Supplier<Lambdas> ctor = Lambdas::new;
        System.out.println(ctor.get().getClass().getSimpleName());
        // array ctor ref
        Function<Integer, String[]> arr = String[]::new;
        System.out.println(arr.apply(3).length);
        // unbound instance method ref
        Function<String, Integer> len = String::length;
        System.out.println(len.apply("abcde"));
        // private method ref — javac emits a handle to a private method of this
        // nest; downgraded nests must route it through accessors
        Supplier<String> priv = Lambdas::privateStatic;
        System.out.println(priv.get());
        Supplier<String> privInst = new Lambdas()::privateInstance;
        System.out.println(privInst.get());
        // super::method ref
        Sub sub = new Sub();
        System.out.println(sub.viaSuper());
    }

    @FeatureTest
    static void intersectionAndSerializable() {
        // intersection cast to two SAM-ish types — indy has extra bsm args
        Runnable r = (Runnable & Serializable) () -> System.out.println("inter");
        r.run();
        System.out.println(r instanceof Serializable);
    }

    @FeatureTest
    static void nestedLambdas() {
        // lambda returning lambda — inner indy captures the outer's argument
        Function<String, Function<String, String>> nested = a -> b -> a + b;
        System.out.println(nested.apply("x").apply("y"));
        Supplier<Supplier<String>> sup = () -> () -> "deep";
        System.out.println(sup.get().get());
    }

    @FeatureTest
    static void genericSam() {
        // generic SAM + default andThen — Signature attribute + bridge shapes
        TestInterface<String, String> at = (a) -> a + ".5";
        TestInterface<String, Double> bt = Double::parseDouble;
        TestInterface<String, Double> ct = at.andThen(bt);
        System.out.println(ct.get("1"));
    }

    @FeatureTest
    void instanceCaptures() {
        // this-capturing forms: instance+static fields via anonymous impl,
        // lambda, and method-ref — runs as an instance probe
        Function<String, String> test10 = new Function<>() {
            @Override
            public String apply(String s) {
                return s + test9 + test11;
            }
        };
        System.out.println(test10.apply("test10"));
        Function<String, String> test12 = (s) -> s + test9 + test11;
        System.out.println(test12.apply("test12"));

        Function<String, String> test13 = test10::apply;
        System.out.println(test13.apply("test20"));
    }

    public static String staticMethod() {
        return "test2";
    }

    public static void testSupplier(Supplier<String> supplier) {
        System.out.println(supplier.get());
    }

    private static String privateStatic() {
        return "priv";
    }

    private String privateInstance() {
        return "privInst";
    }

    public String instanceMethod() {
        return "test3";
    }

    interface TestInterface<A, B> {

        B get(A a);

        default <C> TestInterface<A, C> andThen(TestInterface<B, C> after) {
            return (a) -> after.get(get(a));
        }

    }

    static class Lambdas2 implements Supplier<String> {
        public String get() {
            return "test4";
        }
    }

    static class Base {
        String superCall() {
            return "base";
        }
    }

    static class Sub extends Base {
        String superCall() {
            return "sub";
        }

        String viaSuper() {
            Supplier<String> s = super::superCall;
            return s.get() + ":" + superCall();
        }
    }
}
