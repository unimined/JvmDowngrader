package jvmdg.stubtests.manual;

import jvmdg.stubtests.StubTest;
import jvmdg.stubtests.Support;
import xyz.wagyourtail.jvmdg.version.Ref;

/**
 * Parity tests for the invokedynamic bootstrap members that have no
 * synthesizable call site: their {@code @Modify} handlers fire on
 * {@code invokedynamic} instructions <em>inside corpus classes</em>, so
 * coverage comes from emitting the right source-level constructs (or
 * {@code stubtests21} fixtures for shapes javac won't emit) and
 * diffing the results. A skipped rewrite leaves a dead
 * {@code java/lang/invoke/*} or {@code java/lang/runtime/*} reference on
 * the old JVM — output parity alone proves the transform ran.
 */
public final class BootstrapTests {

    private BootstrapTests() {
    }

    /** javac's default concat indy — j9 @Modify rewrites it on Java 8. */
    @StubTest(member = @Ref(value = "java/lang/invoke/StringConcatFactory",
        member = "makeConcatWithConstants",
        desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
            + "Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)"
            + "Ljava/lang/invoke/CallSite;"))
    public static void concatWithConstants(String id) {
        String a = "v";
        int i = 42;
        Support.print(id, a + "-" + i + "!");
    }

    /** Plain makeConcat needs -XDstringConcat=indy — lives in stubtests21. */
    @StubTest(member = @Ref(value = "java/lang/invoke/StringConcatFactory",
        member = "makeConcat",
        desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
            + "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"))
    public static void concatPlain(String id) {
        Support.print(id, jvmdg.stubtests.input.Concats.cat("a", "b"));
        Support.print(id + "b", jvmdg.stubtests.input.Concats.catInt("n", 7));
        Support.print(id + "c",
            jvmdg.stubtests.input.Concats.catPrimitives(3L, 'x', true));
        Support.print(id + "d", jvmdg.stubtests.input.Concats.catNull("n", null));
    }

    record Pt(String s, int i) {
    }

    record Single(int x) {
    }

    /** Record toString/equals/hashCode indies — j16 @Modify replaces
     * ObjectMethods.bootstrap with generated jvmdowngrader$ methods. */
    @StubTest(member = @Ref(value = "java/lang/runtime/ObjectMethods",
        member = "bootstrap",
        desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
            + "Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;"
            + "[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;"))
    public static void recordObjectMethods(String id) {
        Pt a = new Pt("s", 1);
        Pt b = new Pt("s", 1);
        Pt c = new Pt("t", 2);
        Support.print(id, a.toString());
        Support.print(id + "b",
            a.equals(b) + "," + a.equals(c) + "," + a.equals(null) + "," + a.equals("x"));
        Support.print(id + "c", a.hashCode() == b.hashCode());
        Support.print(id + "d", new Single(5).toString());
        Support.print(id + "e", new Single(5).hashCode() == new Single(5).hashCode());
    }

    /** enumSwitch indy only comes from guarded or case-null enum switches —
     * a plain enum switch compiles to $SwitchMap + lookupswitch. */
    private static String enumLabel(Thread.State s) {
        return switch (s) {
            case null -> "null";
            case NEW -> "new";
            case Thread.State st when st == Thread.State.RUNNABLE -> "runnable-guard";
            default -> "other";
        };
    }

    @StubTest(member = @Ref(value = "java/lang/runtime/SwitchBootstraps",
        member = "enumSwitch",
        desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
            + "Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void enumSwitch(String id) {
        Support.print(id, enumLabel(null));
        Support.print(id + "b", enumLabel(Thread.State.NEW));
        Support.print(id + "c", enumLabel(Thread.State.RUNNABLE));
        Support.print(id + "d", enumLabel(Thread.State.TERMINATED));
    }

    /** Pattern switch over Object — typeSwitch indy at V71 input hits the
     * j23 provider's impl first. */
    private static String typeLabel(Object o) {
        return switch (o) {
            case String s -> "str";
            case Integer i -> "int";
            case null -> "null";
            default -> "other";
        };
    }

    @StubTest(member = @Ref(value = "java/lang/runtime/SwitchBootstraps",
        member = "typeSwitch",
        desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
            + "Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void typeSwitch(String id) {
        Support.print(id, typeLabel("s"));
        Support.print(id + "b", typeLabel(1));
        Support.print(id + "c", typeLabel(null));
        Support.print(id + "d", typeLabel(1.5));
    }

    /** The same member is also registered by Java21Downgrader — reached only
     * when the indy's class-file version is below 23. These V65 fixtures
     * (stubtests21) enter the chain at Java21Downgrader. */
    @StubTest(member = @Ref(value = "java/lang/runtime/SwitchBootstraps",
        member = "typeSwitch",
        desc = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
            + "Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
    public static void typeSwitchJ21(String id) {
        Support.print(id, jvmdg.stubtests.input.TypeSwitches.describe("s"));
        Support.print(id + "b", jvmdg.stubtests.input.TypeSwitches.describe(1));
        Support.print(id + "c", jvmdg.stubtests.input.TypeSwitches.describe(null));
        Support.print(id + "d",
            jvmdg.stubtests.input.TypeSwitches.describeSealed(
                new jvmdg.stubtests.input.TypeSwitches.Circle(2.0)));
        Support.print(id + "e",
            jvmdg.stubtests.input.TypeSwitches.describeSealed(
                new jvmdg.stubtests.input.TypeSwitches.Box(1.0, 2.0)));
    }

    /** Serializable lambdas emit altMetafactory indies. On the tested matrix
     * (all targets are Java 8+) the bootstrap stays native and must pass
     * through untouched — the stub impl only activates below Java 8 and is
     * tracked in deferred.txt. Not a member claim. */
    @StubTest
    public static void serializableLambdaPassthrough(String id) {
        Runnable r = (Runnable & java.io.Serializable) () -> {
        };
        r.run();
        Support.print(id, r instanceof java.io.Serializable);
    }
}
