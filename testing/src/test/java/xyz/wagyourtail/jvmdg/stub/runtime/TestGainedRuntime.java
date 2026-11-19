package xyz.wagyourtail.jvmdg.stub.runtime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link GainedRuntime#bootstrap} — the indy replacement for generated
 * {@code gen/gained/<I>} dispatcher member calls. Registers fake
 * gained-call rows against test-local receivers and exercises every
 * resolution path the PIC caches: declarer/native member, plain and
 * {@code D:}-prefixed stub sites, ceiling gating, the unresolvable
 * {@link AbstractMethodError} tail, and the {@code instanceof I} guard
 * when the interface exists on the running JVM.
 */
@Execution(ExecutionMode.SAME_THREAD) // mutates RetroTypes' shared gained tables
public class TestGainedRuntime {

    private static final MethodHandles.Lookup L = MethodHandles.lookup();
    private static final String I = "jvmdg/test/GainedI";

    /** a "member" receiver — declares {@code op(int)} natively like a JDK class would */
    public static class Member {
        public int op(int x) {
            return x + 1;
        }
    }

    /** a member whose {@code op} is only served through a registered stub impl */
    public static class StubMember {
    }

    /** the stub impl for {@link StubMember} — self-first like a real api impl */
    public static class StubImpl {
        public static int op(Object self, int x) {
            return x + 40;
        }
    }

    /** a member carrying an overridable {@code op} — the {@code D:} site's own-member path */
    public static class DMember {
        public int op(int x) {
            return x + 100;
        }
    }

    /** an empty receiver — rows exist but its closure names no member type */
    public static class Stranger {
    }

    private static final String MEMBER = "xyz/wagyourtail/jvmdg/stub/runtime/TestGainedRuntime$Member";
    private static final String STUB_MEMBER = "xyz/wagyourtail/jvmdg/stub/runtime/TestGainedRuntime$StubMember";
    private static final String DMEMBER = "xyz/wagyourtail/jvmdg/stub/runtime/TestGainedRuntime$DMember";
    private static final String STUB_IMPL = "xyz/wagyourtail/jvmdg/stub/runtime/TestGainedRuntime$StubImpl";

    @BeforeAll
    public static void register() {
        // declarer row — Member's own op(int), bound at ceiling 61
        RetroTypes.gainedCallRegister(I, "op (I)I", MEMBER,
            new int[]{61}, new String[]{MEMBER}, new String[]{null});
        // stub row — StubMember has no op; the impl serves it
        RetroTypes.gainedCallRegister(I, "op (I)I", STUB_MEMBER,
            new int[]{61}, new String[]{null},
            new String[]{STUB_IMPL + ".op(Ljava/lang/Object;I)I"});
        // D: row — dispatchable stub, the receiver's own op wins
        RetroTypes.gainedCallRegister(I, "op (I)I", DMEMBER,
            new int[]{61}, new String[]{null},
            new String[]{"D:" + STUB_IMPL + ".op(Ljava/lang/Object;I)I"});
        // ceiling-gated row — only resolvable at ceiling 99+
        RetroTypes.gainedCallRegister("jvmdg/test/HighCeiling", "op (I)I", MEMBER,
            new int[]{99}, new String[]{MEMBER}, new String[]{null});
        // native-I rows for the guard path — Comparator exists on this JVM
        RetroTypes.gainedCallRegister("java/util/Comparator",
            "compare (Ljava/lang/Object;Ljava/lang/Object;)I",
            "xyz/wagyourtail/jvmdg/stub/runtime/TestGainedRuntime$Cmp",
            new int[]{61}, new String[]{"java/util/Comparator"}, new String[]{null});
    }

    private static CallSite site(String iName, String member, String desc) throws Exception {
        return GainedRuntime.bootstrap(L, member.substring(0, member.indexOf(' ')),
            MethodType.fromMethodDescriptorString(desc, L.lookupClass().getClassLoader()),
            iName, member, 61);
    }

    @Test
    public void declarerPath() throws Throwable {
        CallSite cs = site(I, "op (I)I", "(Ljava/lang/Object;I)I");
        assertEquals(4, (int) cs.getTarget().invoke(new Member(), 3));
        // second invoke hits the PIC cache — same answer
        assertEquals(6, (int) cs.getTarget().invoke(new Member(), 5));
    }

    @Test
    public void stubPath() throws Throwable {
        CallSite cs = site(I, "op (I)I", "(Ljava/lang/Object;I)I");
        assertEquals(43, (int) cs.getTarget().invoke(new StubMember(), 3));
    }

    @Test
    public void dispatchRowOwnMemberWins() throws Throwable {
        CallSite cs = site(I, "op (I)I", "(Ljava/lang/Object;I)I");
        // DMember declares op → override wins over the D: impl's +40
        assertEquals(103, (int) cs.getTarget().invoke(new DMember(), 3));
    }

    @Test
    public void unresolvableThrowsAme() throws Throwable {
        CallSite cs = site("jvmdg/test/NoSuchI", "nope ()V", "(Ljava/lang/Object;)V");
        AbstractMethodError ame = assertThrows(AbstractMethodError.class,
            () -> cs.getTarget().invoke(new Stranger()));
        assertTrue(ame.getMessage().contains("nope ()V"));
    }

    @Test
    public void emptyReceiverThrowsAme() throws Throwable {
        CallSite cs = site(I, "op (I)I", "(Ljava/lang/Object;I)I");
        assertThrows(AbstractMethodError.class,
            () -> cs.getTarget().invoke(new Stranger(), 1));
    }

    @Test
    public void ceilingGatesRow() throws Throwable {
        // the row only binds at vers<=99; a 61-ceiling site must not see it
        CallSite cs = GainedRuntime.bootstrap(L, "op",
            MethodType.methodType(int.class, Object.class, int.class),
            "jvmdg/test/HighCeiling", "op (I)I", 61);
        assertThrows(AbstractMethodError.class,
            () -> cs.getTarget().invoke(new Member(), 1));
    }

    public static class Cmp implements Comparator<Object> {
        @Override
        public int compare(Object a, Object b) {
            return a.hashCode() - b.hashCode();
        }
    }

    @Test
    public void nativeInterfaceGuard() throws Throwable {
        CallSite cs = site("java/util/Comparator",
            "compare (Ljava/lang/Object;Ljava/lang/Object;)I",
            "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)I");
        // receiver implements the real interface — the guard routes to
        // findVirtual(Comparator.compare) without touching the gained rows
        Cmp cmp = new Cmp();
        assertEquals(cmp.compare("a", "b"), cs.getTarget().invoke(cmp, "a", "b"));
    }

    @Test
    public void picCachesPerClass() throws Throwable {
        CallSite cs = site(I, "op (I)I", "(Ljava/lang/Object;I)I");
        // mix receiver classes through one site — the PIC resolves each
        assertEquals(2, (int) cs.getTarget().invoke(new Member(), 1));
        assertEquals(41, (int) cs.getTarget().invoke(new StubMember(), 1));
        assertEquals(3, (int) cs.getTarget().invoke(new Member(), 2));
        assertEquals(101, (int) cs.getTarget().invoke(new DMember(), 1));
    }
}
