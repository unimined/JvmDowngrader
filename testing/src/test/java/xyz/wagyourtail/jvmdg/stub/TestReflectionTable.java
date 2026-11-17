package xyz.wagyourtail.jvmdg.stub;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import xyz.wagyourtail.jvmdg.stub.all.GainedInterfaces;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ASM-free runtime reader behind the rewritten {@code Class.forName}
 * sites: a {@code T} row applies only inside its
 * {@code (runningJvm, authored]} window — inside, the callsite resolves the
 * api stand-in through the callsite's own loader; outside, the original name
 * passes through to a native lookup (real class when present,
 * {@link ClassNotFoundException} when not).
 */
@Execution(ExecutionMode.SAME_THREAD) // mutates ReflectionTable's shared type table
public class TestReflectionTable {

    /** stands in for a JDK type the running JVM doesn't have */
    private static final String FUTURE = "jvmdg/test/FutureType";
    private static final String FUTURE_BIN = "jvmdg.test.FutureType";

    /** a stubbed type already real on the running JVM — its row can never apply */
    private static final String PAST = "jvmdg/test/AlreadyReal";

    /** the api class a {@code T} row resolves to */
    public static class FakeAdapter {
    }

    private static final String ADAPTER = "xyz/wagyourtail/jvmdg/stub/TestReflectionTable$FakeAdapter";
    private static final String ADAPTER_BIN = "xyz.wagyourtail.jvmdg.stub.TestReflectionTable$FakeAdapter";

    /** the JDK owner a member row hangs on — a real class, so the native fallback is reachable */
    public static class FakeSuper {
    }

    /** subclasses inherit {@code getMethod} but not {@code getDeclaredMethod} rows */
    public static class FakeOwner extends FakeSuper {
    }

    /** interface-owner rows upgrade findVirtual's REF kind to invokeInterface */
    public interface FakeItf {
    }

    private static final String OWNER = "xyz/wagyourtail/jvmdg/stub/TestReflectionTable$FakeOwner";
    private static final String ITF = "xyz/wagyourtail/jvmdg/stub/TestReflectionTable$FakeItf";
    private static final String SUPER = "xyz/wagyourtail/jvmdg/stub/TestReflectionTable$FakeSuper";
    private static final String IMPL = "xyz/wagyourtail/jvmdg/stub/TestReflectionTable";

    /** receiver-in-arg0 impl, mirroring a generated instance handler */
    public static String fakeLines(Object self) {
        return "impl:" + self.getClass().getSimpleName();
    }

    /** static-shaped impl */
    public static int fakeStatic(int x, int y) {
        return x + y;
    }

    /** impl whose param carries a stubbed type — matched through a T row */
    public static String fakeTake(Object adapter) {
        return "took:" + adapter.getClass().getSimpleName();
    }

    /** generated-marker stand-ins — the names carry the suffix/infix the interface filter matches */
    public interface Fake$Marker {
    }

    public interface Fake$Ov$member {
    }

    /**
     * {@code getInterfaces} fixture: {@code Runnable} is source-declared and
     * stays; {@code AutoCloseable} stands in for a transform-injected gained
     * interface (recorded via {@link GainedInterfaces}); the two markers
     * stand in for generated dispatch interfaces.
     */
    @GainedInterfaces({"java.lang.AutoCloseable"})
    public static class IfaceFixture implements Runnable, AutoCloseable, Fake$Marker, Fake$Ov$member {
        @Override
        public void run() {
        }

        @Override
        public void close() {
        }
    }

    /** parameterized-type gained entry — exercises the generic-view raw-type check */
    @GainedInterfaces({"java.util.Comparator"})
    public static class GenericFixture implements java.util.Comparator<GenericFixture>, Runnable {
        @Override
        public int compare(GenericFixture a, GenericFixture b) {
            return 0;
        }

        @Override
        public void run() {
        }
    }

    /** impl whose signature names a stubbed type — resolved through the T-row remap */
    public static String fakeStubSig(FakeAdapter a, String s) {
        return "sig:" + s;
    }

    /** impl carrying the callerClass + downgradeVersion tail of a forName-shaped stub */
    public static String fakeTail(String s, Class<?> caller, int origVersion) {
        return s + ":" + caller.getSimpleName() + ":" + origVersion;
    }

    /**
     * the JDK owner a {@code C} row hangs on — its {@code (String)} and
     * {@code (String, int)} ctors play the retargets; {@link #PRE} lets the
     * void pre-hook record ordering without a static race
     */
    public static class FakeCtor {
        public final Object[] seen;
        public String marked;
        public static final ThreadLocal<String> PRE = new ThreadLocal<>();

        public FakeCtor(String s) {
            this.seen = new Object[]{s};
        }

        public FakeCtor(String s, int i) {
            this.seen = new Object[]{s, i};
        }
    }

    private static final String CTOR = "xyz/wagyourtail/jvmdg/stub/TestReflectionTable$FakeCtor";

    /** {@code @InitArg}-shaped converter — an orig arg produces a to-desc slot */
    public static String intToStr(int i) {
        return "s" + i;
    }

    /** converter for the serialized {@code C}-row round trip */
    public static String charToStr(char c) {
        return "c" + c;
    }

    /** {@code @InitPre}-shaped hook — records via ThreadLocal (parallel-safe) */
    public static void markPre(int i) {
        FakeCtor.PRE.set("pre" + i);
    }

    /** {@code @InitPost}-shaped hook — receiver is owner-exact */
    public static void markPost(FakeCtor self, int i) {
        self.marked = "post" + i;
    }

    /** {@code @InitPost} with the {@code downgradeVersion} tail */
    public static void markPostVer(FakeCtor self, int i, int ver) {
        self.marked = "postV" + i + ":" + ver;
    }

    /** {@code @InitPost} whose receiver widens past the owner — exercises the asType receiver adaptation */
    public static void markPostObj(Object self, boolean z) {
        ((FakeCtor) self).marked = "postO" + z;
    }

    /** {@code @InitFactory}-shaped handler */
    public static FakeCtor make(String s, double d) {
        return new FakeCtor(s + ":" + d);
    }

    private static final int CURRENT = Utils.getCurrentClassVersion();

    @BeforeAll
    public static void register() {
        // window opens one release after the running JVM
        ReflectionTable.register(FUTURE, ADAPTER, CURRENT + 1);
        // window already closed — the type is real at the running version
        ReflectionTable.register(PAST, ADAPTER, CURRENT);
        ReflectionTable.registerMember(OWNER, "lines", "()Ljava/lang/String;", true,
            IMPL, "fakeLines", "(Ljava/lang/Object;)Ljava/lang/String;",
            CURRENT + 1, Modifier.PUBLIC);
        ReflectionTable.registerMember(OWNER, "sum", "(II)I", false,
            IMPL, "fakeStatic", "(II)I",
            CURRENT + 1, Modifier.PUBLIC | Modifier.STATIC);
        ReflectionTable.registerMember(OWNER, "take", "(L" + FUTURE + ";)V", false,
            IMPL, "fakeTake", "(Ljava/lang/Object;)V",
            CURRENT + 1, Modifier.PUBLIC);
        // inherited-lookup coverage: the row hangs on the supertype
        ReflectionTable.registerMember(SUPER, "inherited", "()I", false,
            IMPL, "fakeStatic", "(II)I",
            CURRENT + 1, Modifier.PUBLIC | Modifier.STATIC);
        // a row the window keeps inert — behaves like a member real at runtime
        ReflectionTable.registerMember(OWNER, "past", "()V", false,
            IMPL, "fakeStatic", "(II)I",
            CURRENT, Modifier.PUBLIC);
        // impl descs stay source-shaped in the table — the runtime api jar
        // holds the remapped stand-in signature, so resolution remaps
        ReflectionTable.registerMember(OWNER, "stubSig", "(L" + FUTURE + ";Ljava/lang/String;)V", false,
            IMPL, "fakeStubSig", "(L" + FUTURE + ";Ljava/lang/String;)Ljava/lang/String;",
            CURRENT + 1, Modifier.PUBLIC);
        // forName-shaped: transform-injected tail (callerClass + version)
        // the callsite's ldcs would have carried, fabricated at lookup
        ReflectionTable.registerMember(OWNER, "tail", "(Ljava/lang/String;)V", false,
            IMPL, "fakeTail", "(Ljava/lang/String;Ljava/lang/Class;I)V",
            CURRENT + 1, Modifier.PUBLIC | Modifier.STATIC);
        // interface-owner row — revealDirect reports REF_invokeInterface
        ReflectionTable.registerMember(ITF, "itfM", "()Ljava/lang/String;", true,
            IMPL, "fakeLines", "(Ljava/lang/Object;)Ljava/lang/String;",
            CURRENT + 1, Modifier.PUBLIC);
        // C rows — the @InitRewrite shapes: converter slot, pure permutation,
        // pre/post hooks, the version tail, and an @InitFactory
        ReflectionTable.registerCtor(CTOR, "(I)V", false, null, "(Ljava/lang/String;)V",
            new int[]{-1}, new ReflectionTable.Spec[]{
                new ReflectionTable.Spec(IMPL, "intToStr", "(I)Ljava/lang/String;", new int[]{0}, false)},
            null, null, CURRENT + 1, Modifier.PUBLIC);
        ReflectionTable.registerCtor(CTOR, "(Ljava/lang/String;ILjava/lang/Object;)V", false, null,
            "(Ljava/lang/String;I)V", new int[]{0, 1}, null, null, null,
            CURRENT + 1, Modifier.PUBLIC);
        ReflectionTable.registerCtor(CTOR, "(Ljava/lang/String;ILjava/lang/String;)V", false, null,
            "(Ljava/lang/String;)V", new int[]{0}, null,
            new ReflectionTable.Spec(IMPL, "markPre", "(I)V", new int[]{1}, false),
            new ReflectionTable.Spec(IMPL, "markPost", "(L" + CTOR + ";I)V", new int[]{1}, false),
            CURRENT + 1, Modifier.PUBLIC);
        ReflectionTable.registerCtor(CTOR, "(Ljava/lang/String;ID)V", false, null, "(Ljava/lang/String;)V",
            new int[]{0}, null, null,
            new ReflectionTable.Spec(IMPL, "markPostVer", "(L" + CTOR + ";II)V", new int[]{1}, true),
            CURRENT + 1, Modifier.PUBLIC);
        ReflectionTable.registerCtor(CTOR, "(Ljava/lang/String;Z)V", false, null, "(Ljava/lang/String;)V",
            new int[]{0}, null, null,
            new ReflectionTable.Spec(IMPL, "markPostObj", "(Ljava/lang/Object;Z)V", new int[]{1}, false),
            CURRENT + 1, Modifier.PUBLIC);
        ReflectionTable.registerCtor(CTOR, "(Ljava/lang/String;D)V", true,
            new ReflectionTable.Spec(IMPL, "make", "(Ljava/lang/String;D)L" + CTOR + ";", new int[0], false),
            null, null, null, null, null, CURRENT + 1, Modifier.PUBLIC);
        // an inert row — since ≤ running, behaves like a native member
        ReflectionTable.registerCtor(CTOR, "(Ljava/lang/String;Ljava/lang/String;)V", false, null,
            "(Ljava/lang/String;)V", new int[]{0}, null, null, null,
            CURRENT, Modifier.PUBLIC);
        // non-public source member — getConstructor's public gate refuses it
        ReflectionTable.registerCtor(CTOR, "(Ljava/lang/String;J)V", false, null, "(Ljava/lang/String;)V",
            new int[]{0}, null, null, null, CURRENT + 1, Modifier.PRIVATE);
    }

    @Test
    public void rowAppliesInsideWindow() throws Exception {
        assertEquals(ADAPTER_BIN, ReflectionTable.remap(FUTURE_BIN, CURRENT + 1));
        assertSame(FakeAdapter.class,
            ReflectionTable.forName(FUTURE_BIN, TestReflectionTable.class, CURRENT + 1),
            "an in-window T row resolves to the api stand-in through the caller's loader");
    }

    @Test
    public void rowInertOutsideWindow() {
        // authored below since — the authoring JVM couldn't have loaded it either
        assertEquals(FUTURE_BIN, ReflectionTable.remap(FUTURE_BIN, CURRENT));
        // inert at every version — the source is real on the running JVM
        assertEquals("jvmdg.test.AlreadyReal", ReflectionTable.remap("jvmdg.test.AlreadyReal", CURRENT + 9));
    }

    @Test
    public void outOfWindowForNameIsNativeMiss() {
        assertThrows(ClassNotFoundException.class,
            () -> ReflectionTable.forName(FUTURE_BIN, TestReflectionTable.class, CURRENT),
            "outside the window the original name falls through to a native lookup");
    }

    @Test
    public void arrayElementRemaps() throws Exception {
        assertEquals("[L" + ADAPTER_BIN + ";",
            ReflectionTable.remap("[L" + FUTURE_BIN + ";", CURRENT + 1));
        assertEquals("[[L" + ADAPTER_BIN + ";",
            ReflectionTable.remap("[[L" + FUTURE_BIN + ";", CURRENT + 1));
        Class<?> arr = ReflectionTable.forName("[L" + FUTURE_BIN + ";", TestReflectionTable.class, CURRENT + 1);
        assertSame(FakeAdapter.class, arr.getComponentType());
        // an out-of-window element name leaves the whole array name alone
        assertThrows(ClassNotFoundException.class,
            () -> ReflectionTable.forName("[L" + FUTURE_BIN + ";", TestReflectionTable.class, CURRENT));
    }

    @Test
    public void nonObjectNamesPassThrough() throws Exception {
        assertSame(int[].class, ReflectionTable.forName("[I", TestReflectionTable.class, CURRENT + 1));
        assertSame(int[][].class, ReflectionTable.forName("[[I", TestReflectionTable.class, CURRENT + 1));
        assertSame(String[].class,
            ReflectionTable.forName("[Ljava.lang.String;", TestReflectionTable.class, CURRENT + 1));
        assertEquals("com.doesnot.Exist", ReflectionTable.remap("com.doesnot.Exist", CURRENT + 1));
        assertThrows(ClassNotFoundException.class,
            () -> ReflectionTable.forName("com.doesnot.Exist", TestReflectionTable.class, CURRENT + 1));
    }

    @Test
    public void unstubbedNamesPreserveIdentity() throws Exception {
        assertSame(String.class,
            ReflectionTable.forName("java.lang.String", TestReflectionTable.class, CURRENT + 1));
        assertSame(TestReflectionTable.class,
            ReflectionTable.forName("xyz.wagyourtail.jvmdg.stub.TestReflectionTable",
                TestReflectionTable.class, CURRENT + 1));
    }

    @Test
    public void threeArgFormHonorsSuppliedLoader() throws Exception {
        ClassLoader cl = TestReflectionTable.class.getClassLoader();
        assertSame(String.class, ReflectionTable.forName("java.lang.String", false, cl, CURRENT + 1));
        assertSame(FakeAdapter.class, ReflectionTable.forName(FUTURE_BIN, false, cl, CURRENT + 1));
    }

    @Test
    public void firstRegistrationWins() {
        // the resource merge and register() share first-wins semantics
        ReflectionTable.register("jvmdg/test/Dup", "jvmdg/test/First", CURRENT + 1);
        ReflectionTable.register("jvmdg/test/Dup", "jvmdg/test/Second", CURRENT + 1);
        assertEquals("jvmdg.test.First", ReflectionTable.remap("jvmdg.test.Dup", CURRENT + 1));
    }

    @Test
    public void getMethodsReturnsRealMembersOnly() {
        assertArrayEquals(FakeAdapter.class.getMethods(),
            ReflectionTable.getMethods(FakeAdapter.class, CURRENT + 1),
            "enumeration must stay honest — no stub impls injected");
    }

    @Test
    public void getMethodReturnsImplInsideWindow() throws Exception {
        Method m = ReflectionTable.getMethod(FakeOwner.class, "lines",
            new Class<?>[0], CURRENT + 1);
        assertEquals("fakeLines", m.getName(),
            "the table hands out the impl's own Method — the view carries the source name");
        // the facade answers what the source API would have
        assertEquals("lines", StubMethodView.getName(m));
        assertSame(FakeOwner.class, StubMethodView.getDeclaringClass(m),
            "unstubbed owners present themselves — no stand-in needed");
        assertEquals(0, StubMethodView.getParameterCount(m));
        assertArrayEquals(new Class<?>[0], StubMethodView.getParameterTypes(m));
        assertEquals(Modifier.PUBLIC, StubMethodView.getModifiers(m));
    }

    @Test
    public void instanceInvokeShiftsReceiver() throws Exception {
        Method m = ReflectionTable.getMethod(FakeOwner.class, "lines",
            new Class<?>[0], CURRENT + 1);
        assertEquals("impl:FakeOwner",
            StubMethodView.invoke(m, new FakeOwner(), null),
            "the receiver moves back into the impl's arg0");
    }

    @Test
    public void staticInvokePassesThrough() throws Exception {
        Method m = ReflectionTable.getMethod(FakeOwner.class, "sum",
            new Class<?>[]{int.class, int.class}, CURRENT + 1);
        assertEquals(5, StubMethodView.invoke(m, new FakeOwner(), new Object[]{2, 3}),
            "a static impl ignores the receiver entirely");
        assertEquals(2, StubMethodView.getParameterCount(m));
        assertTrue((StubMethodView.getModifiers(m) & Modifier.STATIC) != 0);
    }

    @Test
    public void inheritedRowAnswersGetMethodOnly() throws Exception {
        assertNotNull(ReflectionTable.getMethod(FakeOwner.class, "inherited",
            new Class<?>[0], CURRENT + 1),
            "getMethod searches supertypes — the supertype's row resolves");
        assertThrows(NoSuchMethodException.class,
            () -> ReflectionTable.getDeclaredMethod(FakeOwner.class, "inherited",
                new Class<?>[0], CURRENT + 1),
            "getDeclaredMethod stays on the class itself");
    }

    @Test
    public void stubbedParamMatchesThroughTypeRow() throws Exception {
        // authored code holds the stand-in Class where the source signature
        // names the stubbed type — the T row's reverse closes the gap
        Method m = ReflectionTable.getMethod(FakeOwner.class, "take",
            new Class<?>[]{FakeAdapter.class}, CURRENT + 1);
        assertEquals("took:FakeAdapter",
            StubMethodView.invoke(m, null, new Object[]{new FakeAdapter()}));
    }

    @Test
    public void memberRowInertOutsideWindow() {
        assertThrows(NoSuchMethodException.class,
            () -> ReflectionTable.getMethod(FakeOwner.class, "lines",
                new Class<?>[0], CURRENT),
            "below the row's since the lookup defers to the native getMethod");
        assertThrows(NoSuchMethodException.class,
            () -> ReflectionTable.getMethod(FakeOwner.class, "past",
                new Class<?>[0], CURRENT + 9),
            "a since <= running row never applies");
    }

    @Test
    public void unregisteredMembersStayNative() throws Exception {
        Method m = ReflectionTable.getMethod(FakeOwner.class, "toString",
            new Class<?>[0], CURRENT + 1);
        assertEquals(FakeOwner.class.getMethod("toString"), m,
            "no row → the real Method comes back untouched");
        // and the facade passes ordinary Methods straight through
        assertEquals("toString", StubMethodView.getName(m));
        assertSame(Object.class, StubMethodView.getDeclaringClass(m));
        m.invoke(new FakeOwner());
    }

    @Test
    public void implDescRemapsStubbedParams() throws Exception {
        // the row's impl desc names the stubbed source type — the shipped
        // api jar's signature carries the stand-in instead, so impl
        // resolution must remap through the T rows
        Method m = ReflectionTable.getMethod(FakeOwner.class, "stubSig",
            new Class<?>[]{FakeAdapter.class, String.class}, CURRENT + 1);
        assertEquals(FakeAdapter.class, m.getParameterTypes()[0],
            "the impl's real signature holds the runtime stand-in");
        assertEquals("sig:x",
            StubMethodView.invoke(m, null, new Object[]{new FakeAdapter(), "x"}));
    }

    @Test
    public void transformTailFabricatesCallsiteArgs() throws Exception {
        // a forName-shaped impl takes (member-args..., callerClass, version)
        // — the lookup fabricates the same values the callsite's ldcs carry
        Method m = ReflectionTable.getMethod(FakeOwner.class, "tail",
            new Class<?>[]{String.class}, CURRENT + 1);
        assertEquals("n:FakeOwner:" + (CURRENT + 1),
            StubMethodView.invoke(m, null, new Object[]{"n"}));
        assertEquals(1, StubMethodView.getParameterCount(m),
            "the view reports the source arity, not the impl's");
        assertArrayEquals(new Class<?>[]{String.class}, StubMethodView.getParameterTypes(m));
    }

    @Test
    public void resourceRowCodecDecodesMembers() throws Exception {
        // the shape StubIndexWriter emits — decoded through the same path
        // the resource merge takes
        ReflectionTable.applyRow(new String[]{"M", OWNER, "viaRow", "(I)I", "s",
            IMPL, "fakeStatic", "(II)I",
            String.valueOf(CURRENT + 1), "9"});
        Method m = ReflectionTable.getMethod(FakeOwner.class, "viaRow",
            new Class<?>[]{int.class}, CURRENT + 1);
        assertEquals("viaRow", StubMethodView.getName(m));
        assertTrue((StubMethodView.getModifiers(m) & Modifier.STATIC) != 0);
        ReflectionTable.applyRow(new String[]{"M", OWNER, "bad", "()V", "s",
            IMPL, "fakeStatic", "(II)I", "notanint", "1"});
        assertThrows(NoSuchMethodException.class,
            () -> ReflectionTable.getMethod(FakeOwner.class, "bad",
                new Class<?>[0], CURRENT + 1),
            "malformed window/mods columns drop the row");
    }

    @Test
    public void prunedMemberDegradesToNativeMiss() {
        assertThrows(NoSuchMethodException.class,
            () -> ReflectionTable.getMethod(FakeOwner.class, "noSuch",
                new Class<?>[0], CURRENT + 1),
            "no row anywhere → honest native NoSuchMethodException");
    }

    @Test
    public void findClassRemapsThroughLookupLoader() throws Exception {
        Class<?> c = ReflectionTable.findClass(MethodHandles.lookup(), FUTURE_BIN, CURRENT + 1);
        assertSame(FakeAdapter.class, c);
        assertFalse(c.isAnnotation(), "a plain class assertion keeps the probe honest");
        assertThrows(ClassNotFoundException.class,
            () -> ReflectionTable.findClass(MethodHandles.lookup(), FUTURE_BIN, CURRENT),
            "out of window → honest CNFE, same as forName");
    }

    @Test
    public void findVirtualResolvesReceiverFirstImpl() throws Throwable {
        MethodHandle h = ReflectionTable.findVirtual(MethodHandles.lookup(), FakeOwner.class,
            "lines", MethodType.methodType(String.class), CURRENT + 1);
        assertEquals(MethodType.methodType(String.class, Object.class), h.type(),
            "the impl handle is receiver-first — identical type() to the source member");
        assertEquals("impl:FakeOwner", h.invoke(new FakeOwner()));
    }

    @Test
    public void findStaticResolvesStaticImpl() throws Throwable {
        MethodHandle h = ReflectionTable.findStatic(MethodHandles.lookup(), FakeOwner.class,
            "sum", MethodType.methodType(int.class, int.class, int.class), CURRENT + 1);
        assertEquals(5, h.invoke(2, 3));
    }

    @Test
    public void findSpecialGoesToImplNotDispatch() throws Throwable {
        // invokespecial needs private access to specialCaller — a
        // privateLookupIn'd lookup on FakeOwner makes caller == lookupClass;
        // FakeOwner lacks "lines" natively → the table's impl
        MethodHandle h = ReflectionTable.findSpecial(
            MethodHandles.privateLookupIn(FakeOwner.class, MethodHandles.lookup()),
            FakeOwner.class, "lines", MethodType.methodType(String.class), FakeOwner.class, CURRENT + 1);
        assertEquals("impl:FakeOwner", h.invoke(new FakeOwner()),
            "special = the impl directly, never the override marker");
    }

    @Test
    public void findSpecialPropagatesAccessFailure() {
        // present-but-inaccessible answers IAE, not a table row — the
        // member exists natively (toString), so the miss never reaches it
        assertThrows(IllegalAccessException.class,
            () -> ReflectionTable.findSpecial(MethodHandles.lookup(), FakeOwner.class,
                "toString", MethodType.methodType(String.class), FakeOwner.class, CURRENT + 1));
    }

    @Test
    public void findRespectsKindAndWindow() {
        assertThrows(NoSuchMethodException.class,
            () -> ReflectionTable.findStatic(MethodHandles.lookup(), FakeOwner.class,
                "lines", MethodType.methodType(String.class), CURRENT + 1),
            "an instance row can't answer findStatic — honest native miss");
        assertThrows(NoSuchMethodException.class,
            () -> ReflectionTable.findVirtual(MethodHandles.lookup(), FakeOwner.class,
                "lines", MethodType.methodType(String.class), CURRENT),
            "below the row's since the lookup defers to native");
    }

    @Test
    public void unreflectOnViewMethodIsReceiverFirst() throws Throwable {
        // the Method the table hands out IS the impl's own: unreflect
        // natively produces the receiver-first signature — the stub's job
        // is registering the handle's source view for revealDirect
        Method m = ReflectionTable.getMethod(FakeOwner.class, "lines",
            new Class<?>[0], CURRENT + 1);
        MethodHandle h = ReflectionTable.unreflect(MethodHandles.lookup(), m);
        assertEquals("impl:FakeOwner", h.invoke(new FakeOwner()));
        MethodHandleInfo info = ReflectionTable.revealDirect(MethodHandles.lookup(), h);
        assertEquals(MethodHandleInfo.REF_invokeVirtual, info.getReferenceKind(),
            "an instance-row view reports the virtual kind the source member had");
        assertEquals(FakeOwner.class, info.getDeclaringClass());
        assertEquals("lines", info.getName());
        MethodHandle hs = ReflectionTable.unreflectSpecial(MethodHandles.lookup(), m, FakeOwner.class);
        assertEquals("impl:FakeOwner", hs.invoke(new FakeOwner()),
            "unreflectSpecial on a view Method routes around the static restriction");
        assertEquals(MethodHandleInfo.REF_invokeSpecial,
            ReflectionTable.revealDirect(MethodHandles.lookup(), hs).getReferenceKind());
    }

    @Test
    public void revealDirectPresentsSourceView() throws Throwable {
        MethodHandle h = ReflectionTable.findVirtual(MethodHandles.lookup(), FakeOwner.class,
            "lines", MethodType.methodType(String.class), CURRENT + 1);
        MethodHandleInfo info = ReflectionTable.revealDirect(MethodHandles.lookup(), h);
        assertEquals(MethodHandleInfo.REF_invokeVirtual, info.getReferenceKind());
        assertEquals(FakeOwner.class, info.getDeclaringClass(),
            "the facade reports the source owner, not the impl class");
        assertEquals("lines", info.getName());
        assertEquals(MethodType.methodType(String.class), info.getMethodType(),
            "the facade reports the source signature, not the receiver-first impl");
        assertEquals(Modifier.PUBLIC, info.getModifiers());
        Method impl = info.reflectAs(Method.class, MethodHandles.lookup());
        assertEquals("fakeLines", impl.getName(),
            "reflectAs answers the impl's own Method — identity-consistent with getMethod");
        assertThrows(ClassCastException.class,
            () -> info.reflectAs(Constructor.class, MethodHandles.lookup()));
    }

    @Test
    public void revealDirectOnInterfaceOwnerReportsInterface() throws Throwable {
        MethodHandle h = ReflectionTable.findVirtual(MethodHandles.lookup(), FakeItf.class,
            "itfM", MethodType.methodType(String.class), CURRENT + 1);
        assertEquals(MethodHandleInfo.REF_invokeInterface,
            ReflectionTable.revealDirect(MethodHandles.lookup(), h).getReferenceKind());
    }

    @Test
    public void revealDirectPassthroughForRealHandles() throws Throwable {
        MethodHandle h = MethodHandles.lookup().findVirtual(String.class, "isEmpty",
            MethodType.methodType(boolean.class));
        MethodHandleInfo info = ReflectionTable.revealDirect(MethodHandles.lookup(), h);
        assertEquals(String.class, info.getDeclaringClass(),
            "unregistered handles resolve natively");
    }

    @Test
    public void unreflectSpecialPassthroughForRealMethods() throws Throwable {
        // a non-view method keeps native behavior — the special-caller
        // handle invokes the declaring class's impl on the receiver
        Method m = Object.class.getDeclaredMethod("toString");
        MethodHandle h = ReflectionTable.unreflectSpecial(MethodHandles.lookup(), m, TestReflectionTable.class);
        assertEquals(toString(), h.invoke(this));
    }

    // ---- ctor rows (@InitRewrite / @InitFactory) ----

    @Test
    public void findConstructorConvProducesTargetArg() throws Throwable {
        // (I)V → (String)V via the intToStr converter — converter slot filled
        // by a filter, then the signature permuted onto the orig args
        MethodHandle h = ReflectionTable.findConstructor(MethodHandles.lookup(), FakeCtor.class,
            MethodType.methodType(void.class, int.class), CURRENT + 1);
        Object o = h.invoke(5);
        assertTrue(o instanceof FakeCtor);
        assertEquals("s5", ((FakeCtor) o).seen[0]);
    }

    @Test
    public void findConstructorPermutesAndDrops() throws Throwable {
        // (String,int,Object)V → (String,int)V — pure permutation: arg 2 dropped
        MethodHandle h = ReflectionTable.findConstructor(MethodHandles.lookup(), FakeCtor.class,
            MethodType.methodType(void.class, String.class, int.class, Object.class), CURRENT + 1);
        FakeCtor o = (FakeCtor) h.invoke("a", 2, new Object());
        assertArrayEquals(new Object[]{"a", 2}, o.seen);
    }

    @Test
    public void findConstructorRunsPreThenPost() throws Throwable {
        // (String,int,String)V → (String)V with markPre before and markPost
        // after — the orig desc must NOT be a real ctor or native-first wins
        MethodHandle h = ReflectionTable.findConstructor(MethodHandles.lookup(), FakeCtor.class,
            MethodType.methodType(void.class, String.class, int.class, String.class), CURRENT + 1);
        FakeCtor.PRE.set(null);
        FakeCtor o = (FakeCtor) h.invoke("x", 3, "extra");
        assertEquals("x", o.seen[0]);
        assertEquals("pre3", FakeCtor.PRE.get(),
            "the pre hook ran before the ctor with the orig arg subset");
        assertEquals("post3", o.marked,
            "the post hook ran on the constructed object with the orig arg subset");
    }

    @Test
    public void findConstructorPostReceivesAuthoredVersion() throws Throwable {
        // (String,int,double)V → (String)V with a version-taking post — the
        // trailing int is the origVersion the callsite would have ldc'd
        MethodHandle h = ReflectionTable.findConstructor(MethodHandles.lookup(), FakeCtor.class,
            MethodType.methodType(void.class, String.class, int.class, double.class), CURRENT + 1);
        FakeCtor o = (FakeCtor) h.invoke("y", 7, 1.5);
        assertEquals("postV7:" + (CURRENT + 1), o.marked);
    }

    @Test
    public void findConstructorPostWidenedReceiver() throws Throwable {
        // post declared against Object — receiver adapts via asType
        MethodHandle h = ReflectionTable.findConstructor(MethodHandles.lookup(), FakeCtor.class,
            MethodType.methodType(void.class, String.class, boolean.class), CURRENT + 1);
        FakeCtor o = (FakeCtor) h.invoke("w", true);
        assertEquals("postOtrue", o.marked);
    }

    @Test
    public void findConstructorFactory() throws Throwable {
        // an F row resolves to its handler — no ctor involvement
        MethodHandle h = ReflectionTable.findConstructor(MethodHandles.lookup(), FakeCtor.class,
            MethodType.methodType(void.class, String.class, double.class), CURRENT + 1);
        Object o = h.invoke("m", 2.5);
        assertTrue(o instanceof FakeCtor);
        assertEquals("m:2.5", ((FakeCtor) o).seen[0]);
    }

    @Test
    public void findConstructorNativeFirst() throws Throwable {
        // a real ctor resolves natively — no row synthesis
        MethodHandle h = ReflectionTable.findConstructor(MethodHandles.lookup(), FakeCtor.class,
            MethodType.methodType(void.class, String.class, int.class), CURRENT + 1);
        assertEquals(MethodType.methodType(FakeCtor.class, String.class, int.class), h.type());
        FakeCtor o = (FakeCtor) h.invoke("n", 9);
        assertArrayEquals(new Object[]{"n", 9}, o.seen);
    }

    @Test
    public void findConstructorMissStaysHonest() {
        // the inert row (since ≤ running) never answers — a member the JVM
        // lacks behaves exactly like a native miss
        assertThrows(NoSuchMethodException.class,
            () -> ReflectionTable.findConstructor(MethodHandles.lookup(), FakeCtor.class,
                MethodType.methodType(void.class, String.class, String.class), CURRENT + 1));
        // and a member no row covers misses too
        assertThrows(NoSuchMethodException.class,
            () -> ReflectionTable.findConstructor(MethodHandles.lookup(), FakeCtor.class,
                MethodType.methodType(void.class, String.class, int.class, int.class), CURRENT + 1));
    }

    @Test
    public void getDeclaredConstructorAnswersViewedTarget() throws Throwable {
        // the source ctor is missing natively — the row synthesizes a handle
        // and hands back the target ctor's own Constructor view-registered
        Constructor<?> c = ReflectionTable.getDeclaredConstructor(FakeCtor.class,
            new Class<?>[]{String.class, int.class, Object.class}, CURRENT + 1);
        assertEquals(FakeCtor.class.getDeclaredConstructor(String.class, int.class), c,
            "the handed-out object is the retarget ctor's own Constructor");
        assertArrayEquals(new Class<?>[]{String.class, int.class, Object.class},
            StubCtorView.getParameterTypes(c), "the view reports the source signature");
        assertEquals(3, StubCtorView.getParameterCount(c));
        assertEquals(Modifier.PUBLIC, StubCtorView.getModifiers(c));
        Object o = StubCtorView.newInstance(c, new Object[]{"v", 4, new Object()});
        assertArrayEquals(new Object[]{"v", 4}, ((FakeCtor) o).seen);
    }

    @Test
    public void getConstructorGatesOnSourceVisibility() {
        // the PRIVATE row can't answer a public-ctor lookup...
        assertThrows(NoSuchMethodException.class,
            () -> ReflectionTable.getConstructor(FakeCtor.class,
                new Class<?>[]{String.class, long.class}, CURRENT + 1));
        // ...but getDeclaredConstructor can
        assertDoesNotThrow(() -> ReflectionTable.getDeclaredConstructor(FakeCtor.class,
            new Class<?>[]{String.class, long.class}, CURRENT + 1));
    }

    @Test
    public void unreflectConstructorReturnsStoredHandle() throws Throwable {
        Constructor<?> c = ReflectionTable.getDeclaredConstructor(FakeCtor.class,
            new Class<?>[]{String.class, int.class, Object.class}, CURRENT + 1);
        MethodHandle h = ReflectionTable.unreflectConstructor(MethodHandles.lookup(), c);
        assertEquals(MethodType.methodType(FakeCtor.class, String.class, int.class, Object.class),
            h.type(), "the view's handle keeps the source arity, not the target's");
        FakeCtor o = (FakeCtor) h.invoke("u", 8, new Object());
        assertArrayEquals(new Object[]{"u", 8}, o.seen);
        // revealDirect sees the source view on that handle too
        MethodHandleInfo info = ReflectionTable.revealDirect(MethodHandles.lookup(), h);
        assertEquals(MethodHandleInfo.REF_newInvokeSpecial, info.getReferenceKind());
        assertEquals("<init>", info.getName());
        assertEquals(FakeCtor.class, info.getDeclaringClass());
        assertSame(c, info.reflectAs(Constructor.class, MethodHandles.lookup()),
            "reflectAs answers the same Constructor getConstructor handed out");
    }

    @Test
    public void unreflectConstructorPassthrough() throws Throwable {
        Constructor<FakeCtor> c = FakeCtor.class.getDeclaredConstructor(String.class);
        MethodHandle h = ReflectionTable.unreflectConstructor(MethodHandles.lookup(), c);
        assertEquals(MethodType.methodType(FakeCtor.class, String.class), h.type(),
            "unregistered ctors unreflect natively");
        assertEquals("z", ((FakeCtor) h.invoke("z")).seen[0]);
    }

    @Test
    public void ctorViewHandlesNonPublicTarget() throws Throwable {
        // getDeclaredConstructor on the private-source row still resolves —
        // the target ctor itself is public and reachable
        Constructor<?> c = ReflectionTable.getDeclaredConstructor(FakeCtor.class,
            new Class<?>[]{String.class, long.class}, CURRENT + 1);
        Object o = StubCtorView.newInstance(c, new Object[]{"p", 4L});
        assertEquals("p", ((FakeCtor) o).seen[0]);
    }

    @Test
    public void revealDirectOnCtorHandle() throws Throwable {
        // findConstructor's produced handle carries the source view —
        // REF_newInvokeSpecial + the orig signature
        MethodHandle h = ReflectionTable.findConstructor(MethodHandles.lookup(), FakeCtor.class,
            MethodType.methodType(void.class, int.class), CURRENT + 1);
        MethodHandleInfo info = ReflectionTable.revealDirect(MethodHandles.lookup(), h);
        assertEquals(MethodHandleInfo.REF_newInvokeSpecial, info.getReferenceKind());
        assertEquals("<init>", info.getName());
        assertEquals(FakeCtor.class, info.getDeclaringClass());
        assertEquals(MethodType.methodType(void.class, int.class), info.getMethodType());
        assertEquals(Modifier.PUBLIC, info.getModifiers());
        Constructor<?> c = info.reflectAs(Constructor.class, MethodHandles.lookup());
        assertEquals(FakeCtor.class.getDeclaredConstructor(String.class), c,
            "reflectAs answers the target ctor's own Constructor");
        assertThrows(ClassCastException.class,
            () -> info.reflectAs(Method.class, MethodHandles.lookup()));
    }

    @Test
    public void ctorRowFromResourceLine() throws Throwable {
        // the serialized C shape StubIndexWriter emits — decoded through the
        // same applyRow path the resource merge takes (converter spec must
        // keep its full desc: findImpl reads the return type from it)
        ReflectionTable.applyRow(new String[]{"C", CTOR, "(C)V", "R",
            "(Ljava/lang/String;)V", "-1",
            IMPL + ".charToStr(C)Ljava/lang/String;:0", "-", "-",
            String.valueOf(CURRENT + 1), "1"});
        Constructor<?> c = ReflectionTable.getDeclaredConstructor(FakeCtor.class,
            new Class<?>[]{char.class}, CURRENT + 1);
        assertNotNull(c, "the resource C row resolves to a viewed ctor");
        assertArrayEquals(new Class<?>[]{char.class},
            StubCtorView.getParameterTypes(c));
        // StubCtorView.newInstance is the view op a rewritten newInstance
        // callsite delegates to — invoked directly here
        FakeCtor o = (FakeCtor) StubCtorView.newInstance(c, new Object[]{'q'});
        assertEquals("cq", o.seen[0]);
        // a malformed spec column drops the row — decode failures stay honest
        ReflectionTable.applyRow(new String[]{"C", OWNER, "(F)V", "R",
            "(Ljava/lang/String;)V", "-1", "not-a-spec", "-", "-",
            String.valueOf(CURRENT + 1), "1"});
        assertThrows(NoSuchMethodException.class,
            () -> ReflectionTable.getDeclaredConstructor(FakeOwner.class,
                new Class<?>[]{float.class}, CURRENT + 1));
    }

    /**
     * {@code getInterfaces} drops transform artifacts — the recorded gained
     * interface and both generated-marker name shapes — while keeping the
     * source-declared entries in order.
     */
    @Test
    public void getInterfacesFiltersInjected() {
        assertArrayEquals(new Class<?>[]{Runnable.class},
            ReflectionTable.getInterfaces(IfaceFixture.class, TestReflectionTable.class, CURRENT));
        // a class the transform never touched answers natively unchanged
        assertArrayEquals(new Class<?>[]{FakeItf.class},
            ReflectionTable.getInterfaces(FakeItfImpl.class, TestReflectionTable.class, CURRENT));
        // an interface's own view is untouched
        assertArrayEquals(new Class<?>[0],
            ReflectionTable.getInterfaces(FakeItf.class, TestReflectionTable.class, CURRENT));
    }

    /** {@code getGenericInterfaces} filters the raw type of parameterized entries */
    @Test
    public void getGenericInterfacesFiltersInjected() {
        java.lang.reflect.Type[] out =
            ReflectionTable.getGenericInterfaces(GenericFixture.class, TestReflectionTable.class, CURRENT);
        assertEquals(1, out.length);
        assertSame(Runnable.class, out[0]);
        // signature-less classes fall back to their raw interface list —
        // the same artifacts get filtered there
        assertArrayEquals(new Class<?>[]{Runnable.class},
            ReflectionTable.getGenericInterfaces(IfaceFixture.class, TestReflectionTable.class, CURRENT));
    }

    /**
     * gained-interface stand-ins: a fixture the GAINED table lists keeps its
     * real interfaces and appends the gained ones when
     * {@code since ∈ (runningJvm, origVersion]} — names remap through the
     * T-rows, so a table row whose interface has no stand-in (or is out of
     * window) contributes nothing.
     */
    @Test
    public void getInterfacesAppendsGainedStandIns() {
        // running on a current JVM, any row's since is ≤ runningJvm → none apply
        assertArrayEquals(new Class<?>[]{Runnable.class},
            ReflectionTable.getInterfaces(IfaceFixture.class, TestReflectionTable.class, CURRENT));
        // same for a very old caller window
        assertArrayEquals(new Class<?>[]{Runnable.class},
            ReflectionTable.getInterfaces(IfaceFixture.class, TestReflectionTable.class, 45));
    }

    private static class FakeItfImpl implements FakeItf {
    }
}
