package xyz.wagyourtail.jvmdg.benchmarks.indy;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;
import xyz.wagyourtail.jvmdg.stub.runtime.DispatchRuntime;

/**
 * Candidate bootstraps — faithful replicas of what M-2's
 * {@code GainedRuntime}/{@code RetroRuntime} would ship in src/shared. They
 * exist so the woven indy fixtures exercise the real per-receiver-class PIC
 * machinery ({@link DispatchRuntime#invokePic}) instead of a mock.
 */
public final class Bsms {

    private Bsms() {
    }

    private static final MethodHandles.Lookup IMPL_LOOKUP = MethodHandles.lookup();
    private static final MethodType OP_MT = MethodType.methodType(int.class, int.class);

    /**
     * Gained member dispatch — {@code indy <name>(self,args…)ret},
     * bsmArgs {@code (iName, "name desc", ceiling)}. Per receiver class:
     * when I exists on the running JVM and the receiver implements it,
     * virtual-dispatch on I; otherwise the ceiling-resolved declarer —
     * resolved ONCE per class, where the generated dispatcher retries the
     * instanceof and reflective gainedCall every call.
     */
    public static CallSite gained(MethodHandles.Lookup caller, String name, MethodType site,
                                  String iName, String member, int ceiling) throws Exception {
        ClassLoader cl = caller.lookupClass().getClassLoader();
        int sp = member.indexOf(' ');
        final String mName = member.substring(0, sp);
        final MethodType mType =
            MethodType.fromMethodDescriptorString(member.substring(sp + 1), cl);
        ClassValue<MethodHandle> pic = new ClassValue<MethodHandle>() {
            @Override
            protected MethodHandle computeValue(Class<?> rc) {
                try {
                    Class<?> I = Class.forName(iName.replace('/', '.'), false, cl);
                    if (I.isAssignableFrom(rc)) {
                        return caller.findVirtual(I, mName, mType).asType(site);
                    }
                } catch (Throwable ignore) {
                }
                // gained path — I absent or unserved: the declarer/stub row
                // would come from the gained-calls table; the bench rows map
                // every member to its own native declaration
                try {
                    return caller.findVirtual(rc, mName, mType).asType(site);
                } catch (Throwable t) {
                    return MethodHandles.dropArguments(
                        MethodHandles.throwException(site.returnType(), AbstractMethodError.class)
                            .bindTo(new AbstractMethodError(
                                iName.replace('/', '.') + "." + member + " unresolved")),
                        0, site.parameterList());
                }
            }
        };
        return new ConstantCallSite(DispatchRuntime.invokePic(pic, site));
    }

    /**
     * Retro membership query — {@code indy isInstance(Object)Z},
     * bsmArgs {@code (srcName, adapter, marker)}. Membership per receiver
     * class is CONSTANT — the PIC folds each class's answer into a
     * {@code constant(boolean)} handle the JIT can fold outright, vs the
     * generated dispatcher's ordered instanceof chain + table tail.
     */
    public static CallSite retroIsInstance(MethodHandles.Lookup caller, String name,
                                           MethodType site, String srcName, Class<?> adapter,
                                           Class<?> marker, String memberClasses) throws Exception {
        ClassLoader cl = caller.lookupClass().getClassLoader();
        final Set<String> members =
            new java.util.HashSet<>(java.util.Arrays.asList(memberClasses.split(";")));
        ClassValue<MethodHandle> pic = new ClassValue<MethodHandle>() {
            @Override
            protected MethodHandle computeValue(Class<?> rc) {
                boolean res = adapter.isAssignableFrom(rc) || marker.isAssignableFrom(rc)
                    || members.contains(rc.getName().replace('.', '/'));
                return MethodHandles.dropArguments(
                    MethodHandles.constant(boolean.class, res), 0, site.parameterList());
            }
        };
        return new ConstantCallSite(DispatchRuntime.invokePic(pic, site));
    }

    /**
     * Retro wrap — {@code indy wrap(Object)Ljava/lang/Object;}. Adapter-
     * typed receivers pass through (identity); members get the impl ctor —
     * resolved once per receiver class instead of the dispatcher's ordered
     * instanceof-per-impl chain.
     */
    public static CallSite retroWrap(MethodHandles.Lookup caller, String name, MethodType site,
                                     Class<?> adapter, Class<?> marker, Class<?> impl)
            throws Exception {
        ClassValue<MethodHandle> pic = new ClassValue<MethodHandle>() {
            @Override
            protected MethodHandle computeValue(Class<?> rc) {
                try {
                    if (adapter.isAssignableFrom(rc)) {
                        return MethodHandles.identity(Object.class).asType(site);
                    }
                    // every non-adapter receiver is a wrap member — the impl
                    // ctor resolved once per class (the dispatcher does an
                    // instanceof-per-impl chain each call instead)
                    return IMPL_LOOKUP.findConstructor(impl,
                        MethodType.methodType(void.class, Object.class)).asType(site);
                } catch (Throwable ignore) {
                }
                return MethodHandles.dropArguments(
                    MethodHandles.throwException(site.returnType(), ClassCastException.class)
                        .bindTo(new ClassCastException(rc.getName())),
                    0, site.parameterList());
            }
        };
        return new ConstantCallSite(DispatchRuntime.invokePic(pic, site));
    }

    /**
     * Instance-stub baseline — delegates to the real
     * {@link DispatchRuntime#bootstrap} so today's indy dispatch is measured
     * on the same terms.
     */
    public static CallSite member(MethodHandles.Lookup caller, String name, MethodType site,
                                  MethodType member, Class<?> implOwner, String implSig)
            throws Exception {
        return DispatchRuntime.bootstrap(caller, name, site, member, implOwner, implSig);
    }

    // ---------- {@code jvmdg$indy$} lowering's reflective bsm call ----------

    public static CallSite lowerGained(String iface, String member, int ceiling) {
        try {
            return gained(MethodHandles.lookup(), "op",
                MethodType.methodType(Object.class, Object.class, int.class),
                iface, member, ceiling);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static CallSite lowerRetroIs() {
        try {
            return retroIsInstance(MethodHandles.lookup(), "isInstance",
                MethodType.methodType(boolean.class, Object.class), Weave.RETRO_SRC,
                Fix.Adapter.class, Fix.Marker.class, "bench/R0;bench/R1;bench/R2;bench/R3");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static CallSite lowerMember() {
        try {
            return member(MethodHandles.lookup(), "op",
                MethodType.methodType(int.class, Object.class, int.class),
                MethodType.methodType(int.class, int.class),
                Fix.Impl.class, "op (Ljava/lang/Object;I)I");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
