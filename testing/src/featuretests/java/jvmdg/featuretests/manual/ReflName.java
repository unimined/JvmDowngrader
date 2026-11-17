package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.util.ArrayList;

/**
 * Table-driven {@code Class.forName}: rewritten sites carry the containing
 * class (for the callsite's own loader) and the authoring version; a stubbed
 * JDK type inside the {@code (running, authored]} window resolves to its
 * registered runtime stand-in — an adapter for {@code OWN_INTERFACE} stubs
 * or the existing supertype for {@code SUPERTYPE} — so the observable Class
 * shape is identical on native and downgraded runs. Types outside the window
 * fall to native {@code forName} and {@code ClassNotFoundException} the way
 * the authored JDK would.
 *
 * <p>Probes here assert only what's stable across all three run shapes
 * (native, downgrade, shade): supertype stand-ins resolve natively, and
 * everything unstubbed passes through. Api-adapter remaps are exercised by
 * the {@code ReflectionTable} unit tests — a pruned shade table can't carry
 * them (a dynamic site's reach isn't a bytecode edge, so the adapter isn't
 * in the closure) — {@code forName} of one degrades to the honest CNFE per
 * the shade model's "miss = native behavior" tier.</p>
 */
public class ReflName {

    public static void main(String[] args) {
        Support.run(ReflName.class);
    }

    /** SUPERTYPE stand-in: SequencedCollection → Collection — still an interface, still assigns ArrayList */
    @FeatureTest
    static void seqCollIsInterface(String id) throws Exception {
        Support.print(id, Class.forName("java.util.SequencedCollection").isInterface());
    }

    @FeatureTest
    static void seqCollAssignable(String id) throws Exception {
        Support.print(id, Class.forName("java.util.SequencedCollection").isAssignableFrom(ArrayList.class));
    }

    @FeatureTest
    static void seqCollElemIsInterface(String id) throws Exception {
        Support.print(id, Class.forName("[Ljava.util.SequencedCollection;").getComponentType().isInterface());
    }

    @FeatureTest
    static void stringIdentity(String id) throws Exception {
        Support.print(id, Class.forName("java.lang.String") == String.class);
    }

    /** callsite loader: resolving the corpus's own class through the caller's loader */
    @FeatureTest
    static void selfResolves(String id) throws Exception {
        Support.print(id, Class.forName("jvmdg.featuretests.manual.ReflName").getName());
    }

    @FeatureTest
    static void primArrayIdentity(String id) throws Exception {
        Support.print(id, Class.forName("[I") == int[].class);
    }

    @FeatureTest
    static void objArrayElemIdentity(String id) throws Exception {
        Support.print(id, Class.forName("[Ljava.lang.String;").getComponentType() == String.class);
    }

    /** an always-present stubbed type: the row exists but never applies — native result both ways */
    @FeatureTest
    static void inertRowIsInterface(String id) throws Exception {
        Support.print(id, Class.forName("java.lang.Deprecated").isInterface());
    }

    @FeatureTest
    static void unknownClass(String id) {
        try {
            Class.forName("com.doesnot.Exist");
            Support.print(id, false);
        } catch (ClassNotFoundException e) {
            Support.print(id, true);
        }
    }
}
