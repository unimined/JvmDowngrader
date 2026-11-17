package xyz.wagyourtail.jvmdg.benchmarks.indy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import xyz.wagyourtail.jvmdg.stub.RetroTypes;

/**
 * Runtime-dispatch benchmark — each JVM mechanism a dispatch site could
 * emit, measured apples-to-apples on identical receiver sets:
 *
 * <ul>
 *   <li>{@code *Disp*} — the generated-dispatcher shape (invokestatic →
 *       instanceof fast path → reflective/table tail) as emitted by
 *       {@code GainedDispatchGenerator}/{@code RetroDispatchGenerator}</li>
 *   <li>{@code *Indy*} — the M-2 candidate shape (indy → per-receiver-class
 *       PIC via {@code DispatchRuntime.invokePic})</li>
 *   <li>{@code *Noindy*} — the {@code jvmdg$indy$} lowering shape (CallSite
 *       field + {@code getTarget().invokeExact}) — quantifies the
 *       {@code --noIndy} penalty</li>
 *   <li>direct/table/reflective controls — upper bounds and tail costs</li>
 * </ul>
 *
 * <p>Fixture classes are ASM-woven at setup (javac can't emit these shapes —
 * phantom interfaces, indy sites, callsite fields). The old-JVM gained case
 * is faithful: {@code DispGainedMissing}'s {@code instanceof} operand never
 * resolves, so every call throws and catches NCDFE before the reflective
 * tail — exactly what happens on the real target.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class IndyDispatchBenchmark {

    /** receiver distribution: 1, 4, or 16 distinct classes */
    @Param({"mono", "poly", "mega"})
    public String dist;

    private Weave.Out fx;
    /** round-robin receiver sets matching {@link #dist} */
    private Object[] recv;
    private Object[] recvNew;
    private Object[] recvRetro;
    private Object[] recvEmpty;
    private Object[] recvMiss;
    private int idx;

    // dispatch variants — each is a Fix.Site/BoolSite impl
    private Fix.Site dispGainedMissing;
    private Fix.Site dispGainedReal;
    private Fix.Site indyGained;
    private Fix.Site noindyGained;
    private Fix.BoolSite dispRetroIs;
    private Fix.BoolSite indyRetroIs;
    private Fix.BoolSite noindyRetroIs;
    private Fix.Site dispRetroWrap;
    private Fix.Site indyRetroWrap;
    private Fix.BoolSite dispXClass;
    private Fix.BoolSite indyXClass;
    private Fix.Site indyMember;
    private Fix.Site noindyMember;

    private Method reflectOp;
    private MethodHandle proxyCtor;
    private ClassLoader proxyLoader;

    @Setup
    public void setup() throws Exception {
        fx = Weave.emit();
        // register the fake gained/retro rows the reflective tails read —
        // fake "bench/…" names never collide with real api rows
        for (int i = 0; i < 16; i++) {
            String t = "bench/M" + i;
            RetroTypes.gainedCallRegister(Weave.GAINED_I, "op (I)I", t,
                new int[]{61}, new String[]{t}, new String[]{null});
        }
        RetroTypes.register(Weave.RETRO_SRC,
            java.util.Arrays.asList("bench/R0", "bench/R1", "bench/R2", "bench/R3"));
        RetroTypes.gainedRegister(Weave.GAINED_I, "bench/M0", 61);

        int n = "mono".equals(dist) ? 1 : "poly".equals(dist) ? 4 : 16;
        recv = new Object[n];
        recvNew = new Object[n];
        recvRetro = new Object[n];
        recvEmpty = new Object[n];
        recvMiss = new Object[n];
        for (int i = 0; i < n; i++) {
            recv[i] = fx.members[i].getDeclaredConstructor().newInstance();
            recvNew[i] = fx.membersNew[i].getDeclaredConstructor().newInstance();
            recvRetro[i] = fx.retroMembers[i].getDeclaredConstructor().newInstance();
            recvEmpty[i] = fx.empty[i].getDeclaredConstructor().newInstance();
            // receivers that implement NEITHER the retro adapter/marker nor a
            // listed impl — the table tail decides (non-member answers false)
            recvMiss[i] = new StringBuilder();
        }

        dispGainedMissing = (Fix.Site) fx.dispGainedMissing;
        dispGainedReal = (Fix.Site) fx.dispGainedReal;
        indyGained = (Fix.Site) fx.indyGained;
        noindyGained = (Fix.Site) fx.noindyGained;
        dispRetroIs = (Fix.BoolSite) fx.dispRetroIs;
        indyRetroIs = (Fix.BoolSite) fx.indyRetroIs;
        noindyRetroIs = (Fix.BoolSite) fx.noindyRetroIs;
        dispRetroWrap = (Fix.Site) fx.dispRetroWrap;
        indyRetroWrap = (Fix.Site) fx.indyRetroWrap;
        dispXClass = (Fix.BoolSite) fx.dispXClass;
        indyXClass = (Fix.BoolSite) fx.indyXClass;
        indyMember = (Fix.Site) fx.indyMember;
        noindyMember = (Fix.Site) fx.noindyMember;

        reflectOp = fx.members[0].getDeclaredMethod("op", int.class);
        proxyLoader = getClass().getClassLoader();
    }

    private Object next(Object[] set) {
        return set[(idx++) & (set.length - 1)];
    }

    // ==================== gained member dispatch ====================

    /** upper bound — real virtual call on the member's own interface */
    @Benchmark
    public int gained_direct() {
        return ((Fix.Member) next(recv)).op(7);
    }

    /**
     * OLD-JVM NORM: the dispatcher's {@code instanceof I} never resolves —
     * NCDFE allocated+caught per call, then the full reflective gainedCall
     * (hierarchy BFS + Class.forName + getDeclaredMethod + Method.invoke).
     */
    @Benchmark
    public Object gained_disp_missingI() throws Throwable {
        return dispGainedMissing.call(next(recv), 7, 61);
    }

    /** new-JVM fast path — receivers implement the real I */
    @Benchmark
    public Object gained_disp_presentI() throws Throwable {
        return dispGainedReal.call(next(recvNew), 7, 61);
    }

    /** real I on classpath but receivers don't implement → miss → reflective tail */
    @Benchmark
    public Object gained_disp_missI() throws Throwable {
        return dispGainedReal.call(next(recv), 7, 61);
    }

    /** M-2a candidate — indy PIC: per-rc resolved declarer handle */
    @Benchmark
    public Object gained_indy() throws Throwable {
        return indyGained.call(next(recv), 7, 61);
    }

    /** {@code --noIndy}-lowered version of the same PIC */
    @Benchmark
    public Object gained_noindy() throws Throwable {
        return noindyGained.call(next(recv), 7, 61);
    }

    /** the reflective tail alone — attribution for the dispatcher paths */
    @Benchmark
    public Object gained_reflect() throws Throwable {
        return RetroTypes.gainedCall(Weave.GAINED_I, "op (I)I", next(recv),
            new Object[]{7}, 61);
    }

    /** raw Method.invoke on a pre-resolved member — floor of any reflective path */
    @Benchmark
    public Object gained_methodInvoke() throws Throwable {
        return reflectOp.invoke(next(recv), 7);
    }

    // ==================== retro instanceof / isInstance ====================

    /** upper bound — raw instanceof on a real type */
    @Benchmark
    public boolean retro_direct() {
        return next(recvRetro) instanceof Fix.Marker;
    }

    /** generated dispatcher: adapter|marker|impl-chain instanceofs → table tail */
    @Benchmark
    public boolean retro_disp() {
        return dispRetroIs.test(next(recvRetro));
    }

    /** M-2b candidate — per-rc {@code constant(boolean)} handle */
    @Benchmark
    public boolean retro_indy() {
        return indyRetroIs.test(next(recvRetro));
    }

    @Benchmark
    public boolean retro_noindy() {
        return noindyRetroIs.test(next(recvRetro));
    }

    /** the name-keyed table alone — dispatcher's tail */
    @Benchmark
    public boolean retro_table() {
        return RetroTypes.isInstance(next(recvRetro), Weave.RETRO_SRC);
    }

    /** non-member receivers — every path must answer false through the tail */
    @Benchmark
    public boolean retro_disp_miss() {
        return dispRetroIs.test(next(recvMiss));
    }

    @Benchmark
    public boolean retro_indy_miss() {
        return indyRetroIs.test(next(recvMiss));
    }

    // ==================== retro wrap ====================

    /** generated dispatcher: adapter instanceof → impl instanceof/new chain */
    @Benchmark
    public Object wrap_disp() {
        try {
            return dispRetroWrap.call(next(recvRetro), 0, 61);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /** M-2b candidate — indy → per-rc impl-ctor handle */
    @Benchmark
    public Object wrap_indy() {
        try {
            return indyRetroWrap.call(next(recvRetro), 0, 61);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /** the $Wrap equivalent as direct allocation — codegen floor */
    @Benchmark
    public Object wrap_new() {
        return new Fix.WrapImpl(next(recvRetro));
    }

    /** Proxy-based wrap — allocation + invocation-handler per call */
    @Benchmark
    public Object wrap_proxy() {
        return Proxy.newProxyInstance(proxyLoader, new Class<?>[]{Fix.Adapter.class},
            (proxy, method, args) -> null);
    }

    // ==================== X.class queries ====================

    /** generated shape for {@code Adapter.class.isInstance(o)} */
    @Benchmark
    public boolean xclass_disp() {
        return dispXClass.test(next(recvRetro));
    }

    @Benchmark
    public boolean xclass_indy() {
        return indyXClass.test(next(recvRetro));
    }

    /** native upper bound — {@code isInstance} on the real marker */
    @Benchmark
    public boolean xclass_native() {
        return Fix.Marker.class.isInstance(next(recvRetro));
    }

    // ==================== instance-stub dispatch (today's indy — baseline) ====================

    /** today's indy PIC — own-member wins (the common case) */
    @Benchmark
    public Object member_indy() throws Throwable {
        return indyMember.call(next(recv), 7, 61);
    }

    /** today's indy PIC — impl fallback (receivers without the member) */
    @Benchmark
    public Object member_indy_impl() throws Throwable {
        return indyMember.call(next(recvEmpty), 7, 61);
    }

    /** {@code --noIndy}-lowered instance dispatch (generic jvmdg$indy$ form) */
    @Benchmark
    public Object member_noindy() throws Throwable {
        return noindyMember.call(next(recv), 7, 61);
    }

    /**
     * real {@code --noIndy} member path — {@code DispatchRuntimeDesugar}
     * lowers the dispatch indy to this reflective {@code dispatchStub} call
     * inline, so a member-carrying receiver pays {@code ownMember} +
     * {@code invokeResolved} per call
     */
    @Benchmark
    public Object member_dispatchStub() throws Throwable {
        Object self = next(recv);
        return xyz.wagyourtail.jvmdg.stub.DispatchRuntime.dispatchStub("op (I)I",
            Fix.Impl.class, "op (Ljava/lang/Object;I)I", self, new Object[]{7},
            self.getClass().getClassLoader());
    }
}
