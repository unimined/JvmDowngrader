package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.io.IOException;
import java.util.function.Function;

/**
 * Core language constructs that compile to distinctive bytecode shapes:
 * exception forms, try-with-resources suppressed tables, multi-catch, arrays,
 * casts, boxing, varargs, synchronization, labels, bridge methods, init order,
 * anonymous/local/inner class capture.
 */
public class LangCore {

    public static void main(String[] args) {
        Support.run(LangCore.class);
    }

    private static class TestResource implements AutoCloseable {
        private final String name;
        private final boolean throwing;

        TestResource(String name, boolean throwing) {
            this.name = name;
            this.throwing = throwing;
            System.out.println("open " + name);
        }

        @Override
        public void close() {
            System.out.println("close " + name);
            if (throwing) {
                throw new IllegalStateException("close " + name);
            }
        }
    }

    @FeatureTest
    static void twr() {
        // order: open a, open b, body, close b, close a
        try (TestResource a = new TestResource("a", false);
             TestResource b = new TestResource("b", false)) {
            System.out.println("body");
        }
    }

    @FeatureTest
    static void twrSuppressed() {
        // body throws + close throws -> close exception is suppressed
        try (TestResource a = new TestResource("a", true)) {
            throw new IllegalArgumentException("body");
        } catch (Throwable t) {
            System.out.println(t.getClass().getSimpleName() + ":" + t.getMessage());
            for (Throwable s : t.getSuppressed()) {
                System.out.println("suppressed:" + s.getClass().getSimpleName() + ":" + s.getMessage());
            }
        }
    }

    @FeatureTest
    static void twrEffectivelyFinal() {
        // j9 try-with-resources on an already-declared effectively-final var
        TestResource r = new TestResource("ef", false);
        try (r) {
            System.out.println("efBody");
        }
    }

    @FeatureTest
    static void patternInstanceof() {
        // j16 pattern instanceof: binding var, negated pattern, array pattern
        Object o = "str";
        if (o instanceof String s) {
            System.out.println("pat:" + s);
        }
        if (!(o instanceof Integer n)) {
            System.out.println("patNeg");
        }
        Object arr = new String[1];
        System.out.println(arr instanceof String[] sa && sa.length == 1);
    }

    @FeatureTest
    static void unnamedVars() {
        // j22 unnamed variables: catch param, for-each var, lambda param —
        // the `_` local must not appear in the LocalVariableTable of users
        try {
            throw new IllegalStateException("ignored");
        } catch (Exception _) {
            System.out.println("caughtUnnamed");
        }
        int sum = 0;
        for (String _ : new String[]{"a", "b", "c"}) {
            sum++;
        }
        System.out.println("loopSum:" + sum);
        java.util.function.BiFunction<String, String, String> f = (x, _) -> x;
        System.out.println(f.apply("keep", "drop"));
    }

    @FeatureTest
    static void multiCatch() {
        // multi-catch with unchecked siblings, checked alternatives, and
        // ArrayStoreException — exception-table union types
        try {
            throw new IllegalArgumentException("x");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("caught " + e.getClass().getSimpleName());
        }
        try {
            throwDataFormat();
        } catch (java.text.ParseException | java.util.zip.DataFormatException e) {
            System.out.println("caughtChecked " + e.getClass().getSimpleName());
        }
        try {
            int[] a = new int[2];
            Object o = a;
            ((String[]) o)[0] = "x";
        } catch (ArrayStoreException | ClassCastException e) {
            System.out.println("store " + e.getClass().getSimpleName());
        }
    }

    private static void throwDataFormat() throws java.text.ParseException, java.util.zip.DataFormatException {
        throw new java.util.zip.DataFormatException("checked");
    }

    @FeatureTest
    static void exceptions() {
        // exception ctor overloads, Throwable subclasses, initCause chaining,
        // anonymous VirtualMachineError subclass — stack depth is not diffed
        new Exception(new IndexOutOfBoundsException(1L).getMessage());
        try {
            throw new IndexOutOfBoundsException(1L);
        } catch (IndexOutOfBoundsException e) {
            System.out.println(e);
        }
        try {
            throw new VirtualMachineError("test", new IndexOutOfBoundsException(1L)) {
            };
        } catch (VirtualMachineError e) {
            System.out.println(e);
        }
        Function<Long, IndexOutOfBoundsException> ioobe = IndexOutOfBoundsException::new;
        ioobe.apply(0L);
        try {
            throw new InternalError("test", new IndexOutOfBoundsException(1L));
        } catch (InternalError e) {
            System.out.println(e);
        }
        // cause/initCause chain
        Exception chain = (Exception) new Exception("outer").initCause(new IllegalStateException("inner"));
        System.out.println(chain.getCause().getClass().getSimpleName() + ":" + chain.getCause().getMessage());
        // stack depth is JVM-shape-dependent; line numbers are not — compare count
        try {
            throw new IllegalStateException("depth");
        } catch (IllegalStateException e) {
            System.out.println("traceDepth:" + (e.getStackTrace().length > 0));
        }
    }

    @FeatureTest
    static void asserts() {
        // exercises the $assertionsDisabled synthetic field both sides
        assert true;
        try {
            assert false : "assertMsg";
            System.out.println("assertsOff");
        } catch (AssertionError e) {
            System.out.println("assert:" + e.getMessage());
        }
    }

    @FeatureTest
    static void arrays() {
        // array covariance + ArrayStoreException, multi-dim, clone,
        // componentType — anewarray/multianewarray/aastore shapes
        String[] strs = {"a", "b"};
        Object[] objs = strs; // covariant
        System.out.println(objs instanceof String[]);
        int[][] multi = new int[2][3];
        multi[1][2] = 7;
        System.out.println(multi.length + ":" + multi[1].length + ":" + multi[1][2]);
        int[] clone = {1, 2, 3};
        int[] copy = clone.clone();
        copy[0] = 9;
        System.out.println(clone[0] + ":" + copy[0] + ":" + (clone != copy));
        try {
            objs[0] = 1;
        } catch (ArrayStoreException e) {
            System.out.println("ArrayStoreException");
        }
        System.out.println(new String[0].getClass().getComponentType().getSimpleName());
    }

    @FeatureTest
    static void castsAndBoxing() {
        // instanceof on boxed types, unboxing NPE, ternary numeric promotion,
        // compound-assignment implicit narrowing, Integer cache bounds
        Object n = Integer.valueOf(5);
        System.out.println(n instanceof Integer);
        System.out.println(n instanceof Number);
        // unboxing NPE
        try {
            Integer boxed = null;
            int unboxed = boxed;
            System.out.println(unboxed);
        } catch (NullPointerException e) {
            System.out.println("unboxNPE");
        }
        // ternary numeric promotion: (true ? int : double) widens to double
        Object t = true ? 1 : 2.0;
        System.out.println(t.getClass().getSimpleName() + ":" + t);
        // compound assignment implicit narrowing
        byte b = 1;
        b += 127; // would overflow without cast
        System.out.println(b);
        b = 1;
        b = (byte) (b + 127);
        System.out.println(b);
        // Integer cache bounds (JDK contract: -128..127)
        System.out.println(Integer.valueOf(100) == Integer.valueOf(100));
    }

    @FeatureTest
    static void varargs() {
        // varargs call shapes: spread args, empty (synthesized empty array),
        // and a pre-built array passed straight through
        System.out.println(fmt("a", "b", "c"));
        System.out.println(fmt());
        System.out.println(fmt(new String[]{"x"}));
    }

    private static String fmt(String... args) {
        return args.length + ":" + String.join(",", args);
    }

    @FeatureTest
    static void synchronization() {
        // synchronized method modifier (ACC_SYNCHRONIZED) vs synchronized
        // block (monitorenter/monitorexit)
        SyncTarget target = new SyncTarget();
        System.out.println(target.syncMethod());
        System.out.println(target.syncBlock());
    }

    private static class SyncTarget {
        synchronized String syncMethod() {
            return "syncMethod";
        }

        String syncBlock() {
            synchronized (this) {
                return "syncBlock";
            }
        }
    }

    @FeatureTest
    static void labels() {
        // labeled continue/break — nested-loop jump targets in bytecode
        outer:
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 2) continue outer;
                if (i == 2) break outer;
                System.out.println(i + "" + j);
            }
        }
    }

    @FeatureTest
    static void initOrder() {
        // clinit/init-block/ctor ordering across a two-level hierarchy —
        // the strict JVM-spec sequence must survive transforms
        new InitChild();
    }

    private static class InitParent {
        static {
            System.out.println("parentStatic");
        }

        {
            System.out.println("parentInit");
        }

        InitParent() {
            System.out.println("parentCtor");
        }
    }

    private static class InitChild extends InitParent {
        static {
            System.out.println("childStatic");
        }

        {
            System.out.println("childInit");
        }

        InitChild() {
            System.out.println("childCtor");
        }
    }

    @FeatureTest
    static void classForms() {
        // anonymous capture
        String captured = "cap";
        System.out.println(new Object() {
            @Override
            public String toString() {
                return "anon:" + captured;
            }
        });
        // local class
        class Local {
            String describe() {
                return "local:" + captured;
            }
        }
        System.out.println(new Local().describe());
        // inner class outer-this access
        Inner in = new LangCore().new Inner();
        System.out.println(in.outerValue());
    }

    private String field = "outer";

    private class Inner {
        String outerValue() {
            return field;
        }
    }

    @FeatureTest
    static void bridges() {
        // covariant return -> bridge method exists on the impl
        ChildString child = new ChildString();
        ParentIface<String> parent = child;
        System.out.println(child.get());
        System.out.println(parent.get());
        for (java.lang.reflect.Method m : ChildString.class.getDeclaredMethods()) {
            if (m.getName().equals("get")) {
                System.out.println("bridge:" + m.isBridge() + ":" + m.getReturnType().getSimpleName());
            }
        }
    }

    private interface ParentIface<T> {
        T get();
    }

    private static class ChildString implements ParentIface<String> {
        @Override
        public String get() {
            return "str";
        }
    }

    @FeatureTest
    static void divAndShift() {
        // int div-by-zero, signed vs unsigned shift, long shift-distance
        // masking, i++/++i evaluation order — arithmetic edge semantics
        try {
            int z = 1 / 0;
            System.out.println(z);
        } catch (ArithmeticException e) {
            System.out.println("div0");
        }
        System.out.println(-8 >> 1);
        System.out.println(-8 >>> 1);
        System.out.println(1L << 40);
        int i = 5;
        System.out.println("" + (i++) + i + (++i));
    }
}
