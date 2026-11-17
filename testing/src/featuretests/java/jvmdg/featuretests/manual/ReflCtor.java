package jvmdg.featuretests.manual;

import jvmdg.featuretests.FeatureTest;
import jvmdg.featuretests.Support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Table-driven {@code Class.getConstructor}/{@code getDeclaredConstructor}
 * and {@code Lookup.findConstructor}/{@code unreflectConstructor}: stubbed
 * {@code <init>} members inside the {@code (running, authored]} window
 * resolve through the reflection table's {@code C} rows — the rewrite spec
 * (keep-map, converters, pre/post hooks, version tail) is evaluated by
 * MethodHandle combinators — and the handed-out {@code Constructor} presents
 * the source-API signature through {@code StubCtorView}.
 *
 * <p>Probes here assert only what's stable across all three run shapes
 * (native, downgrade, shade): real ctors pass straight through the facade
 * and misses stay honest {@code NoSuchMethodException}s. The stubbed-ctor
 * path — {@code FileWriter.getConstructor(File, Charset)} resolving to the
 * j11 impl and running its post-hook — is verified on unshaded downgrade
 * and by {@code TestReflectionTable}; a pruned shade table can't answer it
 * since a dynamic site's impl reach isn't a bytecode edge (CG-3e's
 * {@code usedOwners} tier is where that lands).</p>
 */
public class ReflCtor {

    public static void main(String[] args) {
        Support.run(ReflCtor.class);
    }

    /** no row → native ctor, newInstance straight through */
    @FeatureTest
    static void realCtorPassthrough(String id) throws Exception {
        Constructor<String> c = String.class.getConstructor(byte[].class);
        Support.print(id, c.newInstance((Object) "hi".getBytes()));
    }

    /** getDeclaredConstructor reaches non-public members identically —
     *  a private ctor on our own class stays setAccessible everywhere */
    @FeatureTest
    static void realDeclaredCtor(String id) throws Exception {
        Constructor<ReflCtor> c = ReflCtor.class.getDeclaredConstructor(String.class);
        c.setAccessible(true);
        Support.print(id, c.newInstance("xy").seen);
    }

    final String seen;

    private ReflCtor(String seen) {
        this.seen = seen;
    }

    /** facade metadata on a real ctor passes through identically */
    @FeatureTest
    static void realCtorView(String id) throws Exception {
        Constructor<?> c = StringBuilder.class.getConstructor(String.class);
        Support.print(id, c.getName() + ":" + c.getDeclaringClass().getName()
            + ":" + c.getParameterCount() + ":" + Arrays.toString(c.getParameterTypes())
            + ":" + Modifier.toString(c.getModifiers()));
    }

    /** unknown signature → honest NoSuchMethodException everywhere */
    @FeatureTest
    static void ctorMiss(String id) {
        try {
            String.class.getConstructor(Integer.class);
            Support.print(id, false);
        } catch (NoSuchMethodException e) {
            Support.print(id, true);
        }
    }

    /** findConstructor on a real member answers the native handle */
    @FeatureTest
    static void findCtorReal(String id) throws Throwable {
        MethodHandle h = MethodHandles.lookup().findConstructor(StringBuilder.class,
            MethodType.methodType(void.class, int.class));
        Support.print(id, ((StringBuilder) h.invoke(16)).capacity());
    }

    /** unreflectConstructor on a real ctor answers the native handle */
    @FeatureTest
    static void unreflectCtorReal(String id) throws Throwable {
        Constructor<StringBuilder> c = StringBuilder.class.getConstructor(String.class);
        MethodHandle h = MethodHandles.lookup().unreflectConstructor(c);
        Support.print(id, h.invoke("abc"));
    }

    /** revealDirect on a real ctor handle: REF_newInvokeSpecial + reflectAs */
    @FeatureTest
    static void revealCtorReal(String id) throws Throwable {
        MethodHandle h = MethodHandles.lookup().findConstructor(String.class,
            MethodType.methodType(void.class, byte[].class));
        MethodHandleInfo info = MethodHandles.lookup().revealDirect(h);
        Support.print(id, info.getReferenceKind() + ":"
            + (info.getReferenceKind() == MethodHandleInfo.REF_newInvokeSpecial) + ":"
            + (info.reflectAs(Constructor.class, MethodHandles.lookup()).getParameterCount() == 1));
    }
}
