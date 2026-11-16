package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

/**
 * Sealed classes/interfaces: the PermittedSubclasses attribute is stripped on
 * <=j16 and serialized into a {@code @PermittedSubClasses} annotation —
 * isSealed()/getPermittedSubclasses() must keep working.
 */
public sealed class Sealed permits Sealed.Sealed2, Sealed.Sealed3, Sealed.NonSealed {

    public static void main(String[] args) {
        Support.run(Sealed.class);
    }

    @FeatureTest
    static void dispatch() {
        // virtual dispatch through final subclasses of a sealed hierarchy
        Sealed testSeal = new Sealed2();
        testSeal.test();
        Sealed testSeal2 = new Sealed3();
        testSeal2.test();
    }

    @FeatureTest
    static void sealedReflection() {
        // getPermittedSubclasses/isSealed — on <=j16 these read the
        // @PermittedSubClasses annotation the transform serializes
        for (Class<?> permittedSubclass : Sealed.class.getPermittedSubclasses()) {
            System.out.println(permittedSubclass.getSimpleName());
        }
        System.out.println(Sealed.class.isSealed());
        System.out.println(Sealed2.class.isSealed());
        System.out.println(NonSealed.class.isSealed());
    }

    @FeatureTest
    static void nonSealed() {
        // non-sealed subclass opens the hierarchy again
        NonSealed2 ok = new NonSealed2();
        System.out.println(ok instanceof Sealed);
    }

    public void test() {
        System.out.println("test");
    }

    public static final class Sealed2 extends Sealed {

    }

    public static final class Sealed3 extends Sealed {
        @Override
        public void test() {
            System.out.println("test2");
        }

    }

    public static non-sealed class NonSealed extends Sealed {

    }

    public static class NonSealed2 extends NonSealed {

    }

}
