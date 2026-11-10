package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

/**
 * JDK25 compact main forms (JEP 512): instance {@code void main()},
 * {@code void main(String[])}, static {@code void main()}, non-public static
 * args main, and mains inherited from a superclass. On <=j24 targets the
 * transform synthesizes a traditional {@code public static void main(String[])}
 * (and renames instance mains to {@code jvmdg$instanceMain}); the batch driver
 * launches each nested class below as a real main so the synthesized entry
 * point is exercised end-to-end.
 */
public class Mains {

    public static void main(String[] args) {
        Support.run(Mains.class);
    }

    @FeatureTest
    static void directCall() {
        // instance main invoked as a plain method — same on every JVM
        new Base().main();
        new ArgsInst().main(new String[0]);
    }

    /**
     * No declared main of its own — inherits instance {@code void main()}.
     * Discovered via @xyz.wagyourtail.TestMain; launched by the batch driver.
     */
    @xyz.wagyourtail.TestMain
    public static class Inherited extends Base {

    }

    public static class Base {

        Base() {
            System.out.println("BaseConstructor");
            super();
        }

        void main() {
            IO.println(this.getClass().getSimpleName());
        }

    }

    public static class ArgsInst {

        void main(String[] args) {
            IO.println(this.getClass().getSimpleName());
        }

    }

    public static class Inherited2 extends ArgsInst {

        @Override
        void main(String[] args) {
            super.main(args);
            IO.println("inherited");
        }

    }

    public static class NoArgs {

        static void main() {
            IO.println("noArgsStatic");
        }

    }

    public static class StaticArgs {

        static void main(String[] args) {
            IO.println("staticArgs:" + args.length);
        }

    }

}
