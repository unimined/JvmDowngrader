package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

/**
 * Table-driven {@code Lookup.find*}: rewritten sites carry the authoring
 * version; a stubbed member inside the {@code (running, authored]} window
 * resolves to a receiver-first {@code findStatic} on the impl — the
 * {@code $jvmdg$dispatch} sibling for {@code findVirtual} (override-
 * preserving), the impl handler for {@code findSpecial}. {@code findClass}
 * takes the {@code forName} type-remap path scoped to the lookup loader.
 *
 * <p>All member lookups are <b>native-first</b>: a member the running JVM
 * resolves (even declared on a subtype, like {@code String.isEmpty} vs
 * the j15 {@code CharSequence.isEmpty} row) answers with the real handle;
 * the table only answers genuine {@code NoSuchMethodException} misses —
 * and an access failure propagates the honest {@code
 * IllegalAccessException} rather than slipping the impl past it.
 * {@code revealDirect} on a produced handle presents the
 * {@code StubHandleView} facade (source owner/name/signature/{@code
 * REF_*} kind); {@code ConstantDesc} resolution funnels through the same
 * {@code find*} sites inside the api jar.</p>
 *
 * <p>Probes assert only what's stable across all three run shapes (native,
 * downgrade, shade): real members and {@code SUPERTYPE} table rows answer
 * identically everywhere; a stubbed-member impl pruned out of a shade jar
 * degrades to the native miss. The stubbed-member handle path —
 * {@code findVirtual(ExecutorService, "close")} dispatching to the impl —
 * is verified on unshaded downgrade and by {@code TestReflectionTable}
 * ({@code usedOwners} closure roots are CG-3e).</p>
 */
public class ReflHandles {

    /** carries {@code forName}'s checked exception so the methodref compiles */
    private interface ClsFn {
        Class<?> apply(String s) throws ClassNotFoundException;
    }

    public static void main(String[] args) {
        Support.run(ReflHandles.class);
    }

    /** findVirtual on a real member — native handle on every run shape */
    @FeatureTest
    static void findVirtualReal(String id) throws Throwable {
        MethodHandle h = MethodHandles.lookup().findVirtual(String.class, "length",
            MethodType.methodType(int.class));
        Support.print(id, h.invoke("hello"));
    }

    /** findStatic on a real member */
    @FeatureTest
    static void findStaticReal(String id) throws Throwable {
        MethodHandle h = MethodHandles.lookup().findStatic(String.class, "valueOf",
            MethodType.methodType(String.class, int.class));
        Support.print(id, h.invoke(42));
    }

    /**
     * {@code Class::forName} as a constant methodref — the constant-pool
     * {@code Handle} can't carry the callsite's version/caller ldcs, so the
     * transform emits a wrapper method supplying them; the ref then links
     * the remapping impl rather than falling back to the native member.
     * Assignability keeps the probe name-stable: native resolves
     * {@code SequencedCollection}, the j8 side its {@code Collection} stand-in.
     */
    @FeatureTest
    static void methodRefForName(String id) throws Exception {
        ClsFn fn = Class::forName;
        Class<?> c = fn.apply("java.util.SequencedCollection");
        Support.print(id, c.isAssignableFrom(List.class) + ":" + c.isInterface());
    }

    /** findClass on a stubbed type — the SUPERTYPE row resolves on every shape */
    @FeatureTest
    static void findClassStandin(String id) throws Exception {
        Class<?> c = MethodHandles.lookup().findClass("java.util.SequencedCollection");
        Support.print(id, c.isAssignableFrom(List.class) + ":" + c.isInterface());
    }

    /** findClass on a real class is identity */
    @FeatureTest
    static void findClassReal(String id) throws Exception {
        Support.print(id, MethodHandles.lookup().findClass("java.util.ArrayList") == java.util.ArrayList.class);
    }

    /** unreflect on a real method — receiver-first handle, native path */
    @FeatureTest
    static void unreflectReal(String id) throws Throwable {
        MethodHandle h = MethodHandles.lookup().unreflect(String.class.getMethod("hashCode"));
        Support.print(id, h.invoke("hello"));
    }

    /**
     * {@code revealDirect} on a real handle — the table registers views
     * only for produced handles, so this reports the native info
     * ({@code REF_invokeVirtual} = 5, {@code java.lang.String}) on every
     * run shape.
     */
    @FeatureTest
    static void revealDirectReal(String id) throws Throwable {
        MethodHandle h = MethodHandles.lookup().findVirtual(String.class, "isEmpty",
            MethodType.methodType(boolean.class));
        MethodHandleInfo info = MethodHandles.lookup().revealDirect(h);
        Support.print(id, info.getDeclaringClass().getSimpleName() + ":" + info.getReferenceKind());
    }

    /**
     * The {@code ConstantDesc} route: {@code DirectMethodHandleDesc.
     * resolveConstantDesc} funnels through {@code lookup.find*} — on the
     * downgraded side those api-internal sites hit the table like any
     * callsite. A real member's VIRTUAL desc resolves identically
     * everywhere.
     */
    @FeatureTest
    static void constDescVirtual(String id) throws Throwable {
        MethodHandleDesc d = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.VIRTUAL,
            ClassDesc.of("java.lang.String"), "isEmpty",
            MethodTypeDesc.ofDescriptor("()Z"));
        MethodHandle h = (MethodHandle) d.resolveConstantDesc(MethodHandles.lookup());
        Support.print(id, h.invoke("x"));
    }

    /** a real STATIC ConstantDesc through the same funnel */
    @FeatureTest
    static void constDescStatic(String id) throws Throwable {
        MethodHandleDesc d = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC,
            ClassDesc.of("java.lang.String"), "valueOf",
            MethodTypeDesc.of(ClassDesc.of("java.lang.String"), ConstantDescs.CD_int));
        MethodHandle h = (MethodHandle) d.resolveConstantDesc(MethodHandles.lookup());
        Support.print(id, h.invoke(7));
    }

    /**
     * the sibling covariant redeclarations — JDK 21 gave ClassDesc and
     * MethodTypeDesc their own (Lookup)T shapes, so on 56–64 these callsites
     * route through the j21 ConstantDesc stubs the same way MethodHandleDesc's
     * does
     */
    @FeatureTest
    static void constDescTypes(String id) throws Throwable {
        Class<?> c = ClassDesc.of("java.lang.String").resolveConstantDesc(MethodHandles.lookup());
        MethodType mt = MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)I")
            .resolveConstantDesc(MethodHandles.lookup());
        Support.print(id, c.getSimpleName() + ":" + mt);
    }

    /** a member that exists nowhere → honest NoSuchMethodException */
    @FeatureTest
    static void findVirtualMiss(String id) {
        try {
            MethodHandles.lookup().findVirtual(String.class, "noSuchMember",
                MethodType.methodType(void.class));
            Support.print(id, false);
        } catch (NoSuchMethodException e) {
            Support.print(id, true);
        } catch (IllegalAccessException e) {
            Support.print(id, false);
        }
    }
}
