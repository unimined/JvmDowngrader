package jvmdg.stubtests.input;

/**
 * {@code invokedynamic typeSwitch} fixtures at class-file version 65
 * (release 21). The downgrade chain enters per class version, so these
 * hit the <em>Java21Downgrader</em> {@code SwitchBootstraps.typeSwitch}
 * {@code @Modify} — the same member a release-27 corpus class would route
 * through the j23 impl instead. {@code BootstrapTests.typeSwitchJ21} calls
 * these and prints results.
 */
public final class TypeSwitches {

    private TypeSwitches() {
    }

    /** Pattern switch over {@code Object} — emits the {@code typeSwitch} indy. */
    public static String describe(Object o) {
        return switch (o) {
            case String s -> "str:" + s;
            case Integer i -> "int:" + i;
            case null -> "null";
            default -> "other:" + o.getClass().getSimpleName();
        };
    }

    public sealed interface Shape permits Circle, Box {
    }

    public record Circle(double r) implements Shape {
    }

    public record Box(double w, double h) implements Shape {
    }

    /**
     * Exhaustive switch over a sealed hierarchy — {@code typeSwitch} indy with
     * sealed-permitted-subclasses bootstrap args.
     */
    public static String describeSealed(Shape s) {
        return switch (s) {
            case Circle c -> "circle:" + c.r();
            case Box b -> "box:" + b.w() + "x" + b.h();
        };
    }

}
