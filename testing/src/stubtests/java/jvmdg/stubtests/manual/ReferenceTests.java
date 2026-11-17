package jvmdg.stubtests.manual;

import jvmdg.stubtests.StubTest;
import jvmdg.stubtests.Support;
import xyz.wagyourtail.jvmdg.version.Ref;

/**
 * Parity probe for {@code java.lang.ref.Reference.reachabilityFence} — the
 * stub's observable contract is "completes normally, keeps the argument
 * reachable"; the printed marker keeps the probe deterministic.
 */
public final class ReferenceTests {

    private ReferenceTests() {
    }

    @StubTest(member = @Ref(value = "java/lang/ref/Reference",
        member = "reachabilityFence", desc = "(Ljava/lang/Object;)V"))
    public static void reachabilityFence(String id) {
        Object o = new Object();
        java.lang.ref.Reference.reachabilityFence(o);
        Support.print(id, "fenced");
    }

}
