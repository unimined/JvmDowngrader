package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

/**
 * Enum forms beyond plain constants: constant-specific class bodies (javac
 * emits {@code Fancy$1}/{@code Fancy$2} synthetic subclasses), enum ctor
 * params/fields, interface implementation with per-constant method bodies,
 * and the core API ({@code values}/{@code valueOf}/{@code name}/{@code ordinal}/
 * {@code compareTo}/{@code getDeclaringClass}).
 */
public class Enums {

    public static void main(String[] args) {
        Support.run(Enums.class);
    }

    public enum Fancy {
        ALPHA("a", 1) {
            @Override
            String tag() {
                return "tagged-a";
            }
        },
        BETA("b", 2) {
            @Override
            String tag() {
                return "tagged-b";
            }
        },
        GAMMA("g", 3);

        final String id;
        final int rank;

        Fancy(String id, int rank) {
            this.id = id;
            this.rank = rank;
        }

        String tag() {
            return "plain-" + id;
        }
    }

    public interface BinaryOp {
        int apply(int a, int b);
    }

    public enum Ops implements BinaryOp {
        PLUS {
            @Override
            public int apply(int a, int b) {
                return a + b;
            }
        },
        MINUS {
            @Override
            public int apply(int a, int b) {
                return a - b;
            }
        },
        TIMES {
            @Override
            public int apply(int a, int b) {
                return a * b;
            }
        }
    }

    @FeatureTest
    static void constantBodies() {
        // Fancy$1/Fancy$2 synthetic subclasses — constant-specific bodies
        // dispatch through a different runtime class than body-less GAMMA
        System.out.println(Fancy.ALPHA.tag());
        System.out.println(Fancy.BETA.tag());
        System.out.println(Fancy.GAMMA.tag());
        System.out.println(Fancy.ALPHA.getClass() != Fancy.GAMMA.getClass());
        System.out.println(Fancy.ALPHA.getClass().getEnclosingClass().getSimpleName());
    }

    @FeatureTest
    static void interfaceImpl() {
        // enum implementing an interface with per-constant method bodies —
        // also exercises interface dispatch onto enum synthetics
        System.out.println(Ops.PLUS.apply(3, 4));
        System.out.println(Ops.MINUS.apply(3, 4));
        System.out.println(Ops.TIMES.apply(3, 4));
        BinaryOp op = Ops.valueOf("MINUS");
        System.out.println(op.apply(10, 4));
    }

    @FeatureTest
    static void fieldsAndCtor() {
        // enum ctor params + instance fields read through the synthetic
        // values() array — iteration order is declaration order
        for (Fancy f : Fancy.values()) {
            System.out.println(f.id + ":" + f.rank);
        }
    }

    @FeatureTest
    static void coreApi() {
        // the compiler-synthesized enum surface: values/valueOf/name/ordinal/
        // compareTo/getDeclaringClass
        System.out.println(Fancy.valueOf("BETA") == Fancy.BETA);
        System.out.println(Fancy.ALPHA.name());
        System.out.println(Fancy.GAMMA.ordinal());
        System.out.println(Fancy.ALPHA.compareTo(Fancy.GAMMA) < 0);
        System.out.println(Fancy.BETA.getDeclaringClass() == Fancy.class);
        System.out.println(Fancy.values().length);
        System.out.println(Fancy.valueOf(Fancy.class, "GAMMA"));
    }
}
