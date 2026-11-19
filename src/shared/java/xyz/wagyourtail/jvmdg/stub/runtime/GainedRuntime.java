package xyz.wagyourtail.jvmdg.stub.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Runtime side of gained-member call dispatch — the indy replacement for
 * the generated {@code gen/gained/<I>} dispatcher method tables.
 *
 * <p>A callsite that would have been {@code invokeinterface I.m} on a
 * receiver whose class predates {@code I} (a <em>gained member</em>) is
 * emitted as {@code invokedynamic <name> (self, args...)ret} bootstrapped
 * here with args {@code (iName, member "name desc", ceiling)}. The member
 * descriptor travels in the callsite signature itself — no trailing
 * {@code int ceiling} arg, no erased {@code Object[]} packing.
 *
 * <p>Three dispatch regimes, picked per runtime:
 * <ul>
 *   <li>{@code I} present on this JVM and the receiver serves it — a
 *   {@code guardWithTest} routes the site straight to
 *   {@code I.m}-equivalent dispatch ({@code findVirtual}), which is the
 *   generated dispatcher's {@code instanceof → invokeinterface} fast
 *   path minus the {@code LinkageError} catch region. Every call pays an
 *   {@code instanceof} check the JIT folds into the call.</li>
 *   <li>{@code I} absent, or the receiver's class doesn't serve it — a
 *   per-receiver-class {@link ClassValue} PIC resolves the gained-call
 *   row once: hierarchy member-T walk, ceiling row selection,
 *   declarer/stub resolution to a real {@link MethodHandle}. Old-target
 *   steady state is a hash lookup plus a direct handle invoke instead
 *   of an {@code NCDFE} + {@code catch(LinkageError)} + reflective tail
 *   per call (~3.6us → ~10ns on j27 measurement).</li>
 *   <li>Unresolvable — a site-typed handle that throws
 *   {@link AbstractMethodError}, matching the generated dispatcher's
 *   {@code gainedCall} tail.</li>
 * </ul>
 *
 * <p>The resolution contract mirrors
 * {@link RetroTypes#gainedCall(String, String, Object, Object[], int, Class)}:
 * superclass-chain-first then BFS-interface member-T walk via
 * {@code RetroTypes}' package-private helpers, {@code vers[k] <= ceiling}
 * descending row selection, declarer-then-stub resolution, and the
 * {@code D:}-prefixed dispatchable-stub form routing into
 * {@link DispatchRuntime}'s own PIC.
 */
public final class GainedRuntime {

    private GainedRuntime() {
    }

    private static final MethodHandles.Lookup IMPL_LOOKUP = MethodHandles.lookup();

    private static final MethodHandle IS_INSTANCE;

    static {
        try {
            IS_INSTANCE = MethodHandles.publicLookup().findVirtual(Class.class,
                "isInstance", MethodType.methodType(boolean.class, Object.class));
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    /**
     * Bootstrap for an emitted gained-member callsite —
     * {@code indy <member-name> (self, member-args...)ret} with bootstrap
     * args {@code (iName, member "name desc", ceiling)}. The callsite
     * signature is the member's own signature with the receiver prepended —
     * primitives stay unboxed end-to-end.
     */
    public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType site,
                                     String iName, String member, int ceiling) {
        Pic pic = new Pic(caller, iName, member, site, ceiling);
        Class<?> i = pic.iClass();
        if (i != null) {
            // I exists on this runtime — keep dispatcher-speed on receivers
            // that serve it: instanceof test → findVirtual(I), PIC tail for
            // the rest. No catch region: a resolution that would throw
            // LinkageError per call instead resolves once per receiver class.
            MethodHandle nativeCall = pic.nativeHandle(i);
            if (nativeCall != null) {
                MethodHandle test = IS_INSTANCE.bindTo(i)
                    .asType(MethodType.methodType(boolean.class, site.parameterType(0)));
                test = MethodHandles.dropArguments(test, 1,
                    site.parameterList().subList(1, site.parameterCount()));
                return new ConstantCallSite(
                    MethodHandles.guardWithTest(test, nativeCall, picChain(pic, site)));
            }
        }
        return new ConstantCallSite(picChain(pic, site));
    }

    private static MethodHandle picChain(Pic pic, MethodType site) {
        return DispatchRuntime.invokePic(pic, site);
    }

    /**
     * Per-callsite polymorphic inline cache — one resolved handle per
     * receiver class, replicating {@code gainedCall}'s resolution order.
     */
    private static final class Pic extends ClassValue<MethodHandle> {

        private final MethodHandles.Lookup caller;
        private final String iName;
        private final String member;
        private final String mName;
        private final String mDesc;
        private final MethodType site;
        private final int ceiling;
        private final ClassLoader cl;
        private volatile Class<?> iResolved = Void.class; // Void.class = unresolved sentinel

        Pic(MethodHandles.Lookup caller, String iName, String member, MethodType site,
            int ceiling) {
            this.caller = caller;
            this.iName = iName;
            this.member = member;
            int sp = member.indexOf(' ');
            this.mName = member.substring(0, sp);
            this.mDesc = member.substring(sp + 1);
            this.site = site;
            this.ceiling = ceiling;
            this.cl = caller.lookupClass().getClassLoader();
        }

        Class<?> iClass() {
            if (iResolved == Void.class) {
                try {
                    iResolved = Class.forName(iName.replace('/', '.'), false, cl);
                } catch (Throwable t) {
                    iResolved = null;
                }
            }
            return iResolved;
        }

        /** {@code findVirtual(I, name, mt)} as a site-typed handle, or null when unresolvable */
        MethodHandle nativeHandle(Class<?> i) {
            try {
                int sp = member.indexOf(' ');
                MethodType mt = MethodType.fromMethodDescriptorString(
                    member.substring(sp + 1), cl);
                // explicitCastArguments, not asType — asType on a
                // varargs-declared member treats Object→Object[] as varargs
                // collection and wraps the caller's array in Object[]{arg}
                return MethodHandles.explicitCastArguments(
                    caller.findVirtual(i, member.substring(0, sp), mt), site);
            } catch (Throwable t) {
                return null;
            }
        }

        @Override
        protected MethodHandle computeValue(Class<?> rc) {
            // native I path — receivers serving I directly
            Class<?> i = iClass();
            if (i != null && i.isAssignableFrom(rc)) {
                MethodHandle mh = nativeHandle(i);
                if (mh != null) {
                    return mh;
                }
            }
            // gained path — RetroTypes resolves the row to an
            // (Object self, Object[] args)Object handle; collect the site's
            // trailing args back into the array contract
            MethodHandle h = RetroTypes.gainedCallHandle(iName, member, rc, ceiling, cl);
            if (h != null) {
                return h.asCollector(Object[].class, site.parameterCount() - 1).asType(site);
            }
            return MethodHandles.dropArguments(
                MethodHandles.throwException(site.returnType(), AbstractMethodError.class)
                    .bindTo(new AbstractMethodError(iName.replace('/', '.') + "." + member
                        + " unresolved for " + rc.getName() + " at ceiling " + ceiling)),
                0, site.parameterList());
        }
    }
}
