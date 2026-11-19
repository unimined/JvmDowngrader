package xyz.wagyourtail.jvmdg.stub.runtime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The runtime membership oracle behind retro-supertype emission —
 * {@code instanceof}/{@code checkcast} on a type that only exists on newer
 * JDKs routes here after the target-side remap.
 */
@Execution(ExecutionMode.SAME_THREAD) // mutates RetroTypes' shared static tables
public class TestRetroTypes {

    /** a fake retro interface — stands in for e.g. java/util/SequencedCollection */
    private static final String RETRO = "jvmdg/test/RetroCollection";

    /** a fake retro type membership only reachable through a marker interface */
    private static final String MARKER_RETRO = "jvmdg/test/RetroMarked";

    /** stands in for the generated jvmdg marker a downgraded user class gets */
    private interface TestMarker {
    }

    private static class MarkerImpl implements TestMarker {
    }

    /** declared as implementing a retro type in a second registration round */
    private static class LateImpl extends TreeSet<String> {
    }

    /** a class-stubbed member's stand-in — {@code J_N_H_HttpClient} shape */
    private static class StubStandIn {
        private interface Marker {
        }
    }

    /** its impl — the class closure names the stand-in, never the member */
    private static class StubImpl extends StubStandIn {
    }

    @BeforeAll
    public static void register() {
        // the shape a java-api build emits for SequencedCollection
        RetroTypes.register(RETRO, Arrays.asList(
            "java/util/List",
            "java/util/Deque",
            "java/util/LinkedHashSet",
            "java/util/SortedSet",
            "java/util/LinkedHashMap$LinkedKeySet",
            "java/util/LinkedHashMap$LinkedValues"));
        RetroTypes.register(MARKER_RETRO,
            Collections.singletonList("xyz/wagyourtail/jvmdg/stub/runtime/TestRetroTypes$TestMarker"));
    }

    @Test
    public void isInstanceFollowsInterfaceClosure() {
        assertTrue(RetroTypes.isInstance(new ArrayList<>(), RETRO));   // implements List
        assertTrue(RetroTypes.isInstance(new LinkedList<>(), RETRO));  // implements Deque
        assertTrue(RetroTypes.isInstance(new LinkedHashSet<>(), RETRO));
        assertTrue(RetroTypes.isInstance(new TreeSet<>(), RETRO));     // implements SortedSet
        assertTrue(RetroTypes.isInstance(new ArrayDeque<>(), RETRO));  // implements Deque
    }

    @Test
    public void isInstanceRejectsNonMembers() {
        assertFalse(RetroTypes.isInstance(new HashSet<>(), RETRO));    // Set, not sequenced
        assertFalse(RetroTypes.isInstance(new Object(), RETRO));
        assertFalse(RetroTypes.isInstance(new HashMap<>(), RETRO));
    }

    @Test
    public void isInstanceHandlesNullAndUnknownTypes() {
        assertFalse(RetroTypes.isInstance(null, RETRO));
        assertFalse(RetroTypes.isInstance(new ArrayList<>(), "jvmdg/test/NeverRegistered"));
    }

    @Test
    public void isInstanceCoversPackagePrivateImplsAndMarkers() {
        // LinkedHashMap's key set is package-private — named directly, no reflection
        assertTrue(RetroTypes.isInstance(new LinkedHashMap<>().keySet(), RETRO));
        assertTrue(RetroTypes.isInstance(new LinkedHashMap<>().values(), RETRO));
        // user code gets membership through the generated marker interface
        assertTrue(RetroTypes.isInstance(new MarkerImpl(), MARKER_RETRO));
        assertFalse(RetroTypes.isInstance(new Object(), MARKER_RETRO));
    }

    /**
     * Gained-supertype membership: a (member, interface) binding is only
     * visible to sites authored at or above the release it appeared — the
     * ceiling argument is the calling class's authored class-file version.
     */
    @Test
    public void gainedMemberHonorsVersionCeiling() {
        RetroTypes.gainedRegister("jvmdg/test/GainedCloseable", "java/util/TreeSet", 63);
        TreeSet<String> ts = new TreeSet<>();
        assertTrue(RetroTypes.gainedMember("jvmdg/test/GainedCloseable", ts, 63));
        assertTrue(RetroTypes.gainedMember("jvmdg/test/GainedCloseable", ts, 70));
        assertFalse(RetroTypes.gainedMember("jvmdg/test/GainedCloseable", ts, 62),
            "a binding that appeared at 63 must be invisible to a V62 site");
        assertFalse(RetroTypes.gainedMember("jvmdg/test/GainedCloseable", new HashSet<>(), 70));
        assertFalse(RetroTypes.gainedMember("jvmdg/test/GainedCloseable", null, 70));
        assertFalse(RetroTypes.gainedMember("jvmdg/test/NeverRegistered", ts, 70));
        // subclasses inherit the membership through the name-closure walk
        assertTrue(RetroTypes.gainedMember("jvmdg/test/GainedCloseable", new LateImpl(), 70));
    }

    /**
     * {@code java/net/http/HttpClient} implements {@code AutoCloseable} only
     * since 65 — and the runtime receiver is the stand-in's impl, whose
     * class closure names {@code J_N_H_HttpClient}, never the member. The
     * membership reverse-index must bridge it or the gained row misses.
     */
    @Test
    public void gainedMemberResolvesStandInReceivers() {
        RetroTypes.register("jvmdg/test/StubbedMember", Arrays.asList(
            "jvmdg/test/StubbedMember",
            "xyz/wagyourtail/jvmdg/stub/runtime/TestRetroTypes$StubStandIn",
            "xyz/wagyourtail/jvmdg/stub/runtime/TestRetroTypes$StubStandIn$Marker"));
        RetroTypes.gainedRegister("jvmdg/test/GainedCloseable", "jvmdg/test/StubbedMember", 65);
        assertTrue(RetroTypes.gainedMember("jvmdg/test/GainedCloseable", new StubImpl(), 65));
        assertTrue(RetroTypes.gainedMember("jvmdg/test/GainedCloseable", new StubStandIn(), 65));
        assertFalse(RetroTypes.gainedMember("jvmdg/test/GainedCloseable", new StubImpl(), 64),
            "a binding that appeared at 65 must be invisible to a V64 site");
        assertFalse(RetroTypes.gainedMember("jvmdg/test/GainedCloseable", new MarkerImpl(), 70),
            "unrelated retro members must not alias");
    }

    /** gained member whose {@code close()} the declarer path invokes reflectively */
    public static class GainedTarget {
        public int closed;
        public void close() {
            closed++;
        }
    }

    /** subclass — member-name closure resolves through the superclass chain */
    public static class GainedTargetChild extends GainedTarget {
    }

    /** a self-first static stub site — the reflective fallback the table carries */
    public static class GainedStub {
        public static int calls;
        public static void closeTarget(GainedTarget self) {
            calls++;
        }
    }

    /** gained member whose declared member throws — the failure must surface, not mask */
    public static class ExplodingTarget {
        public void close() {
            throw new IllegalStateException("real member failure");
        }
    }

    /** receiver without the member — dispatch lands on the impl site */
    public static class DispatchBase {
    }

    /** receiver declaring the member — its own declaration wins at dispatch */
    public static class DispatchReceiver extends DispatchBase {
        public int calls;
        public void mark() {
            calls++;
        }
    }

    /** the self-first impl a {@code D:} site names */
    public static class DispatchImpl {
        public static int calls;
        public static void mark(DispatchBase self) {
            calls++;
        }
    }

    /**
     * {@link DispatchRuntime#dispatchStub} — the reflective half of
     * override-preserving dispatch: the receiver's own public member wins,
     * otherwise the impl site is invoked self-first.
     */
    @Test
    public void dispatchStubOverrideWinsOverImpl() throws Throwable {
        String impl = org.objectweb.asm.Type.getInternalName(DispatchImpl.class)
            + ".mark(L" + org.objectweb.asm.Type.getInternalName(DispatchBase.class) + ";)V";
        ClassLoader cl = getClass().getClassLoader();
        DispatchImpl.calls = 0;
        DispatchReceiver r = new DispatchReceiver();
        DispatchRuntime.dispatchStub("mark ()V", impl, r, new Object[0], cl);
        assertEquals(1, r.calls, "the receiver's declared member wins");
        assertEquals(0, DispatchImpl.calls, "the impl is only the fallback");
        DispatchRuntime.dispatchStub("mark ()V", impl, new DispatchBase(), new Object[0], cl);
        assertEquals(1, DispatchImpl.calls, "a receiver without the member lands on the impl");
    }

    /**
     * The {@code D:}-prefixed stub column end-to-end: a gained-call row
     * whose declarer is absent falls to the dispatch site, which keeps the
     * override-wins contract instead of invoking the impl unconditionally.
     */
    @Test
    public void gainedCallDispatchSiteKeepsOverride() throws Throwable {
        String t = org.objectweb.asm.Type.getInternalName(DispatchBase.class);
        String impl = "D:" + org.objectweb.asm.Type.getInternalName(DispatchImpl.class)
            + ".mark(L" + t + ";)V";
        RetroTypes.gainedCallRegister("jvmdg/test/GainedDI", "mark ()V", t,
            new int[]{63}, new String[]{null}, new String[]{impl});
        DispatchImpl.calls = 0;
        DispatchReceiver r = new DispatchReceiver();
        RetroTypes.gainedCall("jvmdg/test/GainedDI", "mark ()V", r, new Object[0], 63);
        assertEquals(1, r.calls, "the D: site resolves the receiver's own member");
        assertEquals(0, DispatchImpl.calls);
        RetroTypes.gainedCall("jvmdg/test/GainedDI", "mark ()V", new DispatchBase(), new Object[0], 63);
        assertEquals(1, DispatchImpl.calls, "no member → the impl half of the site runs");
    }

    /**
     * The callsite's dispatch chain as a bare handle (what {@code findVirtual}
     * on a stubbed member returns): the per-receiver-class PIC resolves the
     * declared member, unresolvable classes get the impl.
     */
    @Test
    public void dispatchHandleResolvesPerReceiverClass() throws Throwable {
        java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup();
        java.lang.invoke.MethodHandle impl = lookup.findStatic(DispatchImpl.class, "mark",
            java.lang.invoke.MethodType.methodType(void.class, DispatchBase.class));
        java.lang.invoke.MethodHandle h = DispatchRuntime.dispatchHandle(lookup, "mark",
            java.lang.invoke.MethodType.methodType(void.class),
            java.lang.invoke.MethodType.methodType(void.class, DispatchBase.class),
            impl);
        DispatchImpl.calls = 0;
        DispatchReceiver r = new DispatchReceiver();
        h.invoke(r);
        assertEquals(1, r.calls, "the PIC resolves the receiver's member");
        assertEquals(0, DispatchImpl.calls);
        h.invoke(new DispatchBase());
        assertEquals(1, DispatchImpl.calls, "a receiver without the member lands on the impl");
    }

    /**
     * {@link RetroTypes#gainedCall} resolves the member T through the
     * receiver's own hierarchy, invokes the declarer reflectively, and
     * honors the ceiling — a binding that appeared at 63 is unresolvable
     * to a V62 site.
     */
    @Test
    public void gainedCallResolvesDeclarerAtCeiling() throws Throwable {
        String t = org.objectweb.asm.Type.getInternalName(GainedTarget.class);
        RetroTypes.gainedCallRegister("jvmdg/test/GainedCallI", "close ()V", t,
            new int[]{63}, new String[]{t}, new String[]{null});
        GainedTarget target = new GainedTarget();
        assertNull(RetroTypes.gainedCall("jvmdg/test/GainedCallI", "close ()V",
            target, new Object[0], 65));
        assertEquals(1, target.closed, "the declared member must be invoked reflectively");
        // the member-name closure walks the superclass chain
        GainedTargetChild child = new GainedTargetChild();
        RetroTypes.gainedCall("jvmdg/test/GainedCallI", "close ()V", child, new Object[0], 70);
        assertEquals(1, child.closed);
        // a V62-authored site predates the binding — nothing resolves
        assertThrows(AbstractMethodError.class,
            () -> RetroTypes.gainedCall("jvmdg/test/GainedCallI", "close ()V",
                new GainedTarget(), new Object[0], 62));
        // an unregistered (I, member) pair resolves to nothing
        assertThrows(AbstractMethodError.class,
            () -> RetroTypes.gainedCall("jvmdg/test/NeverRegistered", "close ()V",
                new GainedTarget(), new Object[0], 70));
    }

    /**
     * A {@code -} declarer (or a declarer whose member is absent) falls
     * through to the registered stub site — the same self-first static
     * {@code MemberStubTransform} would emit for a direct call.
     */
    @Test
    public void gainedCallFallsBackToStubSite() throws Throwable {
        String t = org.objectweb.asm.Type.getInternalName(GainedTarget.class);
        String stub = org.objectweb.asm.Type.getInternalName(GainedStub.class)
            + ".closeTarget(L" + t + ";)V";
        RetroTypes.gainedCallRegister("jvmdg/test/GainedStubI", "close ()V", t,
            new int[]{63}, new String[]{null}, new String[]{stub});
        int before = GainedStub.calls;
        GainedTarget target = new GainedTarget();
        RetroTypes.gainedCall("jvmdg/test/GainedStubI", "close ()V",
            target, new Object[0], 63);
        assertEquals(before + 1, GainedStub.calls,
            "the stub site receives self-first static invocation");
        assertEquals(0, target.closed);
        // declarer present and invocable → it wins over the stub
        RetroTypes.gainedCallRegister("jvmdg/test/GainedBothI", "close ()V", t,
            new int[]{63}, new String[]{t}, new String[]{stub});
        RetroTypes.gainedCall("jvmdg/test/GainedBothI", "close ()V",
            target, new Object[0], 63);
        assertEquals(before + 1, GainedStub.calls, "the stub is only the fallback");
        assertEquals(1, target.closed, "the real declarer wins over the stub");
        // the context overload resolves the stub site through the context's loader
        RetroTypes.gainedCallRegister("jvmdg/test/GainedCtxI", "close ()V", t,
            new int[]{63}, new String[]{null}, new String[]{stub});
        GainedTarget target2 = new GainedTarget();
        RetroTypes.gainedCall("jvmdg/test/GainedCtxI", "close ()V",
            target2, new Object[0], 63, TestRetroTypes.class);
        assertEquals(before + 2, GainedStub.calls,
            "the context overload reaches the stub site through the context's loader");
    }

    /**
     * A member that itself throws must propagate that exception — resolution
     * succeeded, so there is no stub to fall back to. (Regression: the declarer
     * path used to swallow the member's own failure and report a misleading
     * {@link AbstractMethodError} instead.)
     */
    @Test
    public void gainedCallPropagatesMemberExceptions() {
        String t = org.objectweb.asm.Type.getInternalName(ExplodingTarget.class);
        RetroTypes.gainedCallRegister("jvmdg/test/GainedThrowI", "close ()V", t,
            new int[]{63}, new String[]{t}, new String[]{null});
        IllegalStateException ise = assertThrows(IllegalStateException.class,
            () -> RetroTypes.gainedCall("jvmdg/test/GainedThrowI", "close ()V",
                new ExplodingTarget(), new Object[0], 65));
        assertEquals("real member failure", ise.getMessage());
    }

    /** a declarer absent on the running JVM still resolves to the registered stub site */
    @Test
    public void gainedCallMissingDeclarerFallsToStub() throws Throwable {
        String t = org.objectweb.asm.Type.getInternalName(GainedTarget.class);
        String stub = org.objectweb.asm.Type.getInternalName(GainedStub.class)
            + ".closeTarget(L" + t + ";)V";
        RetroTypes.gainedCallRegister("jvmdg/test/GainedAbsentI", "close ()V", t,
            new int[]{63}, new String[]{"jvmdg/absent/MissingDeclarer"}, new String[]{stub});
        int before = GainedStub.calls;
        RetroTypes.gainedCall("jvmdg/test/GainedAbsentI", "close ()V",
            new GainedTarget(), new Object[0], 63);
        assertEquals(before + 1, GainedStub.calls,
            "an unresolvable declarer falls through to the stub site");
    }

    @Test
    public void gainedCallUnresolvableIsAbstractMethodError() {
        String t = org.objectweb.asm.Type.getInternalName(GainedTarget.class);
        RetroTypes.gainedCallRegister("jvmdg/test/GainedDeadI", "close ()V", t,
            new int[]{63}, new String[]{null}, new String[]{null});
        assertThrows(AbstractMethodError.class,
            () -> RetroTypes.gainedCall("jvmdg/test/GainedDeadI", "close ()V",
                new GainedTarget(), new Object[0], 65));
    }

    @Test
    public void castMatchesCheckcastSemantics() {
        Object list = new ArrayList<>();
        assertSame(list, RetroTypes.cast(list, RETRO));
        assertNull(RetroTypes.cast(null, RETRO));
        ClassCastException cce = assertThrows(ClassCastException.class,
            () -> RetroTypes.cast(new HashSet<>(), RETRO));
        assertTrue(cce.getMessage().contains("java.util.HashSet"));
        assertTrue(cce.getMessage().contains("jvmdg.test.RetroCollection"));
    }

    @Test
    public void isArrayInstanceChecksComponentMembership() {
        assertTrue(RetroTypes.isArrayInstance(new ArrayList[0], RETRO));
        assertTrue(RetroTypes.isArrayInstance(new ArrayList[2][3], RETRO));
        assertFalse(RetroTypes.isArrayInstance(new HashSet[0], RETRO));
        assertFalse(RetroTypes.isArrayInstance(new int[0], RETRO));
        assertFalse(RetroTypes.isArrayInstance(new Object(), RETRO));
        assertFalse(RetroTypes.isArrayInstance(null, RETRO));
    }

    @Test
    public void castArrayMatchesCheckcastSemantics() {
        Object lists = new ArrayList[0];
        assertSame(lists, RetroTypes.castArray(lists, RETRO));
        assertNull(RetroTypes.castArray(null, RETRO));
        assertThrows(ClassCastException.class,
            () -> RetroTypes.castArray(new HashSet[0], RETRO));
    }

    @Test
    public void registrationsMergeAndStayOrderIndependent() {
        // second registration round for the same retro type merges
        RetroTypes.register(RETRO, Collections.singletonList("java/util/TreeMap$Values"));
        assertTrue(RetroTypes.isInstance(new ArrayList<>(), RETRO),
            "earlier registrations survive a later merge");

        // a class already queried (and so already name-cached) must still
        // resolve against a retro type registered afterwards
        String fresh = "jvmdg/test/LateRetro";
        assertFalse(RetroTypes.isInstance(new LateImpl(), fresh));
        RetroTypes.register(fresh, Collections.singletonList("java/util/SortedSet"));
        assertTrue(RetroTypes.isInstance(new LateImpl(), fresh),
            "late registration resolves for already-visited classes");
    }

    @Test
    public void isRetroTypeReportsRowExistence() {
        assertTrue(RetroTypes.isRetroType(RETRO));
        assertTrue(RetroTypes.isRetroType(MARKER_RETRO));
        assertFalse(RetroTypes.isRetroType("java/lang/String"));
        // an empty registration still counts — the row exists
        RetroTypes.register("jvmdg/test/EmptyRetro", Collections.<String>emptyList());
        assertTrue(RetroTypes.isRetroType("jvmdg/test/EmptyRetro"));
    }

    @Test
    public void isAssignableFromFollowsClassSemantics() {
        // X.class.isAssignableFrom(X.class) — identity holds even though the
        // retro name itself is never in its own row
        assertTrue(RetroTypes.isAssignableFrom(LinkedHashSet.class, RETRO, 0));
        assertFalse(RetroTypes.isAssignableFrom(HashSet.class, RETRO, 0),
            "Set is not a row member");
        assertTrue(RetroTypes.isAssignableFrom(LinkedList.class, RETRO, 0));
        assertFalse(RetroTypes.isAssignableFrom(Object.class, RETRO, 0));
        assertThrows(NullPointerException.class,
            () -> RetroTypes.isAssignableFrom(null, RETRO, 0));
    }

    // -- wrap(): real api-jar rows (META-INF/jvmdg/wraps) ------------------

    private static final String RNG = "java/util/random/RandomGenerator";

    private static final Class<?> RNG_ADAPTER =
        xyz.wagyourtail.jvmdg.j17.stub.java_base.J_U_R_RandomGenerator.class;

    @Test
    public void wrapDispatchesDeclaredImpls() {
        Object wrapped = RetroTypes.wrap(new java.util.Random(), RNG, RNG_ADAPTER);
        assertTrue(wrapped instanceof xyz.wagyourtail.jvmdg.j17.stub.java_base.J_U_R_RandomGenerator);
        assertEquals("xyz.wagyourtail.jvmdg.j17.impl.random.BasicRandomGeneratorImpl",
            wrapped.getClass().getName());
        // pulled-up child impl — declaration order puts it before Random's
        wrapped = RetroTypes.wrap(new java.util.SplittableRandom(), RNG, RNG_ADAPTER);
        assertEquals("xyz.wagyourtail.jvmdg.j17.impl.random.SplittableRandomGeneratorImpl",
            wrapped.getClass().getName());
        // Random subclasses hit the Random pair, not the earlier Splittable one
        wrapped = RetroTypes.wrap(java.util.concurrent.ThreadLocalRandom.current(), RNG, RNG_ADAPTER);
        assertEquals("xyz.wagyourtail.jvmdg.j17.impl.random.BasicRandomGeneratorImpl",
            wrapped.getClass().getName());
    }

    @Test
    public void wrapPullsChildAdapterImplsUpward() {
        // StreamableGenerator declares no impls — the Splittable impl pulls up
        Object wrapped = RetroTypes.wrap(new java.util.SplittableRandom(),
            "java/util/random/RandomGenerator$StreamableGenerator",
            xyz.wagyourtail.jvmdg.j17.stub.java_base
                .J_U_R_RandomGenerator.StreamableGenerator.class);
        assertTrue(wrapped instanceof xyz.wagyourtail.jvmdg.j17.stub.java_base
            .J_U_R_RandomGenerator.StreamableGenerator);
        // …but not Random (it is no StreamableGenerator member)
        assertThrows(ClassCastException.class, () ->
            RetroTypes.wrap(new java.util.Random(),
                "java/util/random/RandomGenerator$StreamableGenerator", RNG_ADAPTER));
    }

    @Test
    public void wrapPulledImplsComposeAcrossAdapterTrees() {
        // TypeDescriptor declares nothing — ClassConstable/MethodTypeConstable
        // arrive via the OfField/OfMethod child adapters
        Class<?> td = xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_I_TypeDescriptor.class;
        Object wrapped = RetroTypes.wrap(String.class, "java/lang/invoke/TypeDescriptor", td);
        assertTrue(wrapped instanceof xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_I_TypeDescriptor);
        wrapped = RetroTypes.wrap(java.lang.invoke.MethodType.methodType(void.class),
            "java/lang/invoke/TypeDescriptor", td);
        assertTrue(wrapped instanceof xyz.wagyourtail.jvmdg.j12.stub.java_base.J_L_I_TypeDescriptor);
        assertThrows(ClassCastException.class, () ->
            RetroTypes.wrap("not a member", "java/lang/invoke/TypeDescriptor", td));
    }

    @Test
    public void wrapPassesThroughAdapterTypedObjects() {
        Object impl = RetroTypes.wrap(new java.util.Random(), RNG, RNG_ADAPTER);
        assertSame(impl, RetroTypes.wrap(impl, RNG, RNG_ADAPTER),
            "already-adapted objects must not re-wrap");
    }

    @Test
    public void wrapNullAndNonMemberBehavior() {
        assertNull(RetroTypes.wrap(null, RNG, RNG_ADAPTER));
        assertThrows(ClassCastException.class, () -> RetroTypes.wrap(new Object(), RNG, RNG_ADAPTER));
        // unknown retro type: no row, no membership — plain CCE
        assertThrows(ClassCastException.class, () -> RetroTypes.wrap(new Object(), "jvmdg/test/NoRow", RNG_ADAPTER));
    }

    @Test
    public void isAssignableFromArrayFormsCheckDimensions() {
        // X[].class.isAssignableFrom(member[].class)
        assertTrue(RetroTypes.isAssignableFrom(ArrayList[].class, RETRO, 1));
        // dim mismatch: S[][] is not an X[]
        assertFalse(RetroTypes.isAssignableFrom(ArrayList[][].class, RETRO, 1));
        assertFalse(RetroTypes.isAssignableFrom(ArrayList.class, RETRO, 1));
        // scalar receiver: an array argument is never assignable to X
        assertFalse(RetroTypes.isAssignableFrom(ArrayList[].class, RETRO, 0));
        assertFalse(RetroTypes.isAssignableFrom(int[].class, RETRO, 1));
        assertTrue(RetroTypes.isAssignableFrom(LinkedList[].class, RETRO, 1));
    }

    // -- generated <stub>$jvmdg$retro dispatchers --------------------------

    private static Class<?> dispatcher(String adapterBinaryName) throws ClassNotFoundException {
        return Class.forName(adapterBinaryName + "$jvmdg$retro");
    }

    @Test
    public void generatedDispatchersExistWithModeGatedMethodSets() throws Exception {
        // OWN_INTERFACE: wrap + membership queries, no cast (a raw member
        // can never satisfy an adapter-typed cast without wrapping)
        Class<?> own = dispatcher(RNG_ADAPTER.getName());
        assertNotNull(own.getMethod("wrap", Object.class));
        assertNotNull(own.getMethod("isInstance", Object.class));
        assertNotNull(own.getMethod("isArrayInstance", Object.class));
        assertNotNull(own.getMethod("castArray", Object.class));
        assertNotNull(own.getMethod("isAssignableFrom", Class.class, int.class));
        assertThrows(NoSuchMethodException.class, () -> own.getMethod("cast", Object.class));

        // SUPERTYPE: cast + membership queries, no wrap (the remapped
        // target is a real supertype — no adaptation exists)
        Class<?> sup = dispatcher(
            "xyz.wagyourtail.jvmdg.j21.stub.java_base.J_U_SequencedCollection");
        assertNotNull(sup.getMethod("isInstance", Object.class));
        assertNotNull(sup.getMethod("cast", Object.class));
        assertNotNull(sup.getMethod("isArrayInstance", Object.class));
        assertNotNull(sup.getMethod("castArray", Object.class));
        assertNotNull(sup.getMethod("isAssignableFrom", Class.class, int.class));
        assertThrows(NoSuchMethodException.class, () -> sup.getMethod("wrap", Object.class));
    }

    @Test
    public void generatedDispatcherWrapsInPullUpOrder() throws Exception {
        Method wrap = dispatcher(RNG_ADAPTER.getName()).getMethod("wrap", Object.class);
        assertNull(wrap.invoke(null, new Object[]{null}));
        // child's impl precedes the ancestor's — declaration order is
        // flatten order, not instanceof specificity
        Object wrapped = wrap.invoke(null, new java.util.SplittableRandom());
        assertEquals("xyz.wagyourtail.jvmdg.j17.impl.random.SplittableRandomGeneratorImpl",
            wrapped.getClass().getName());
        wrapped = wrap.invoke(null, new java.util.Random());
        assertEquals("xyz.wagyourtail.jvmdg.j17.impl.random.BasicRandomGeneratorImpl",
            wrapped.getClass().getName());
        // adapter-typed objects pass through unwrapped
        assertSame(wrapped, wrap.invoke(null, wrapped));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void wrapRowSelectsRowMatchingContextStub() throws Exception {
        RetroTypes.isRetroType(RNG); // force resource load
        java.lang.reflect.Field wrapsField = RetroTypes.class.getDeclaredField("WRAPS");
        wrapsField.setAccessible(true);
        Map<String, List<Object>> wraps = (Map<String, List<Object>>) wrapsField.get(null);
        List<Object> rows = wraps.get(RNG);
        assertNotNull(rows, "api jar must ship a RandomGenerator wraps row");

        // simulate a second shaded jar whose prefixed row lands FIRST —
        // member=Object would match everything, impl name unresolvable
        Class<?> wrapRow = Class.forName("xyz.wagyourtail.jvmdg.stub.runtime.RetroTypes$WrapRow");
        Constructor<?> ctor = wrapRow.getDeclaredConstructor(String.class, String[].class, String[].class);
        ctor.setAccessible(true);
        Object foreign = ctor.newInstance("otherprefix/stub/J_U_R_RandomGenerator",
            new String[]{"java/lang/Object"}, new String[]{"otherprefix/impl/ForeignImpl"});
        rows.add(0, foreign);
        try {
            // context still selects the row whose stub column matches its
            // own name — first-row semantics would hit the foreign row and
            // fail resolving otherprefix/impl/ForeignImpl
            Object wrapped = RetroTypes.wrapImpl(new java.util.Random(), RNG, RNG_ADAPTER);
            assertEquals("xyz.wagyourtail.jvmdg.j17.impl.random.BasicRandomGeneratorImpl",
                wrapped.getClass().getName());
        } finally {
            rows.remove(foreign);
        }
    }

}
