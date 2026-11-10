package jvmdg.stubtests.manual;

import jvmdg.stubtests.StubTest;
import jvmdg.stubtests.Support;

/**
 * Runtime parity for the nest-transform path: javac emits real
 * {@code NestHost}/{@code NestMembers} attributes for {@code NestOuter}/
 * {@code NestInner}; on pre-11 targets {@code Java11Downgrader.fixNests}
 * converts them into {@code jvmdg.j11.Nest*} annotations that the
 * {@code J_L_Class} stubs read back. JDK-class receivers (the generated
 * {@code getNestMembers} probes) can't exercise this — you can't annotate
 * {@code java.lang.String} — so the members are covered but the annotation
 * path itself has no parity coverage elsewhere. Unclaimed: {@code getNest*}/
 * {@code isNestmateOf} are already generated-covered members.
 */
public final class NestTests {

    private NestTests() {
    }

    @StubTest
    public static void nestHost(String id) {
        Support.print(id, NestOuter.NestInner.class.getNestHost() == NestOuter.class);
        Support.print(id + "b", NestOuter.class.getNestHost() == NestOuter.class);
        Support.print(id + "c", int.class.getNestHost() == int.class);
    }

    @StubTest
    public static void nestMembers(String id) {
        // native returns host + attribute list; the stub returns host +
        // annotation values — same two-class set either way
        Support.print(id, NestOuter.class.getNestMembers().length);
        Support.print(id + "b", NestOuter.NestInner.class.getNestMembers().length);
        boolean found = false;
        for (Class<?> c : NestOuter.class.getNestMembers()) {
            if (c == NestOuter.NestInner.class) {
                found = true;
            }
        }
        Support.print(id + "c", found);
    }

    @StubTest
    public static void isNestmate(String id) {
        Support.print(id, NestOuter.class.isNestmateOf(NestOuter.NestInner.class));
        Support.print(id + "b", NestOuter.NestInner.class.isNestmateOf(NestOuter.class));
        Support.print(id + "c", NestOuter.class.isNestmateOf(String.class));
    }

    /** Cross-nestmate private access routes through the generated
     * {@code jvmdowngrader$nest$} accessor on pre-11 targets — the runtime
     * half of the accessor rewrite TestOtherTransforms checks at bytecode
     * level. */
    @StubTest
    public static void nestPrivateAccess(String id) {
        Support.print(id, NestOuter.NestInner.readHidden());
    }
}

/** Top-level nest host — a nest host is always the top-level enclosing
 * class, so this pair can't live inside {@link NestTests}; the inner class
 * must be nested to be a member of Outer's nest. */
class NestOuter {
    private static int hidden = 7;

    static class NestInner {
        static int readHidden() {
            return hidden;
        }
    }
}
