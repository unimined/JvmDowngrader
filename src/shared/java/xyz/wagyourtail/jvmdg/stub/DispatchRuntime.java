package xyz.wagyourtail.jvmdg.stub;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import xyz.wagyourtail.jvmdg.exc.MissingStubError;

/**
 * Runtime side of override-preserving instance-stub dispatch.
 *
 * <p>Every {@code INVOKEVIRTUAL}/{@code INVOKEINTERFACE} site resolved to a
 * dispatchable instance stub is emitted as an {@code invokedynamic} whose
 * bootstrap ({@link #bootstrap}) builds a constant callsite. The callsite's
 * dispatch chain resolves the receiver's own member per receiver class via a
 * {@link ClassValue} polymorphic inline cache — {@code findVirtual} finds the
 * receiver's declared or inherited override exactly like the authored JDK's
 * virtual dispatch — and falls back to the registered self-first impl handle
 * when the member is unresolvable on that class. This replaces the generated
 * {@code $jvmdg$dispatch} siblings + {@code $Ov$} marker interfaces: no
 * interface is injected into user classes, so {@code getInterfaces()} shows
 * only what the source declared.
 *
 * <p>The impl is <em>not</em> a constant: the transform emits its owner as a
 * {@code Class} constant plus its {@code "name desc"} as a string, and the
 * PIC resolves it lazily — literal descriptor first, stand-in-mapped second —
 * because the api jar's declared signature differs per packaging (a raw
 * shaded jar keeps the source's JDK types; a downgraded jar rewrites types
 * missing on the running JVM to stand-ins). A lazy string also keeps a bad
 * impl from poisoning {@code <clinit>} in {@code --noIndy} output, where an
 * eagerly linked {@code Handle} constant would fail the whole class.
 *
 * <p>The same resolution is exposed reflectively as {@link #dispatchStub} —
 * the {@code D:}-prefixed site the {@code gained-calls} fallback rows name —
 * so a stub fallback reached through {@link RetroTypes#gainedCall} keeps the
 * override-wins contract too.
 */
public final class DispatchRuntime {

    private DispatchRuntime() {
    }

    private static final MethodHandles.Lookup IMPL_LOOKUP = MethodHandles.lookup();

    private static final MethodHandle GET_CLASS;
    private static final MethodHandle PIC_GET;

    private static final MethodType PIC_GET_MT =
        MethodType.methodType(MethodHandle.class, Class.class);

    static {
        try {
            MethodHandles.Lookup pub = MethodHandles.publicLookup();
            GET_CLASS = pub.findVirtual(Object.class, "getClass",
                MethodType.methodType(Class.class));
            PIC_GET = pub.findVirtual(ClassValue.class, "get",
                MethodType.methodType(Object.class, Class.class));
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    /**
     * Bootstrap for an emitted {@code invokedynamic} instance-stub callsite —
     * {@code indy <member-name> (handler-desc)ret} with bootstrap args
     * {@code (member MethodType, impl-owner Class, impl "name desc" String)}.
     * {@code handler-desc} is the self-first impl signature the transform
     * already emits (receiver + member args + any trailing ldc'd extras);
     * {@code member} is the registered member's own signature used for
     * {@code findVirtual}.
     */
    public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType site,
                                     MethodType member, Class<?> implOwner, String implSig) {
        return new ConstantCallSite(dispatchHandle(caller, name, member, site,
            null, implOwner, implSig));
    }

    /**
     * The dispatch chain as a bare receiver-first handle — same PIC the
     * callsites get, for reflective users ({@code Lookup.findVirtual} on a
     * stubbed member keeps virtual-dispatch semantics through this). A null
     * receiver NPEs in {@code getClass} — the same behavior a real
     * invokevirtual exhibits. This overload takes the pre-resolved impl
     * handle; the bsmArg form defers resolution to the PIC.
     */
    public static MethodHandle dispatchHandle(MethodHandles.Lookup caller, String name,
                                              MethodType member, MethodType site,
                                              MethodHandle impl) {
        return dispatchHandle(caller, name, member, site, impl, null, null);
    }

    /**
     * The dispatch chain as a bare receiver-first handle — the {@code indy}
     * callsite's own form: the impl arrives as an owner class plus a
     * {@code "name desc"} string, resolved lazily by the PIC.
     */
    public static MethodHandle dispatchHandle(MethodHandles.Lookup caller, String name,
                                              MethodType member, MethodType site,
                                              Class<?> implOwner, String implSig) {
        return dispatchHandle(caller, name, member, site, null, implOwner, implSig);
    }

    private static MethodHandle dispatchHandle(MethodHandles.Lookup caller, String name,
                                               MethodType member, MethodType site,
                                               MethodHandle impl, Class<?> implOwner,
                                               String implSig) {
        return invokePic(new Pic(caller, name, site, member, impl, implOwner, implSig), site);
    }

    /**
     * The dispatch chain for a callsite whose per-receiver-class handle a
     * {@link ClassValue} resolves — factored out of the instance-stub path so
     * the api jar's signature-polymorphic bootstrap (VarHandle's
     * {@code jvmdg$bootstrap}) shares the combinator:
     * {@code (self, args...) -> invoker(pic.get(self.getClass()), self, args...)}.
     * {@code getClass} feeds {@code pic.get} through a filter (combiner args
     * must match the target's own arg types), then fold feeds the resolved
     * handle into the invoker's leading MethodHandle slot.
     */
    public static MethodHandle invokePic(ClassValue<MethodHandle> pic, MethodType site) {
        MethodHandle getClass = GET_CLASS.asType(
            MethodType.methodType(Class.class, site.parameterType(0)));
        MethodHandle combiner = MethodHandles.filterArguments(
            PIC_GET.bindTo(pic).asType(PIC_GET_MT), 0, getClass);
        return MethodHandles.foldArguments(
            MethodHandles.invoker(site), combiner).asType(site);
    }

    /**
     * Reflective-path resolution caches — the {@code --noIndy}/gained-fallback
     * callers hit {@link #dispatchStub} per invocation, and uncached
     * {@code ownMember} (two {@code getMethod} probes with desc parsing) +
     * {@code resolveImpl} (up to three {@code getDeclaredMethod} probes plus an
     * access crack) cost 113ns–22µs per call megamorphic. Keyed per class so
     * entries die with their classloader — no leak — and misses cache as
     * {@code Optional.empty} so a memberless receiver or unresolvable impl
     * skips the reflective probes on repeat calls.
     */
    private static final ClassValue<java.util.concurrent.ConcurrentHashMap<String, java.util.Optional<java.lang.reflect.Method>>> MEMBER_CACHE =
        new ClassValue<java.util.concurrent.ConcurrentHashMap<String, java.util.Optional<java.lang.reflect.Method>>>() {
            @Override
            protected java.util.concurrent.ConcurrentHashMap<String, java.util.Optional<java.lang.reflect.Method>> computeValue(Class<?> type) {
                return new java.util.concurrent.ConcurrentHashMap<>();
            }
        };

    private static final ClassValue<java.util.concurrent.ConcurrentHashMap<String, java.util.Optional<java.lang.reflect.Method>>> IMPL_CACHE =
        new ClassValue<java.util.concurrent.ConcurrentHashMap<String, java.util.Optional<java.lang.reflect.Method>>>() {
            @Override
            protected java.util.concurrent.ConcurrentHashMap<String, java.util.Optional<java.lang.reflect.Method>> computeValue(Class<?> type) {
                return new java.util.concurrent.ConcurrentHashMap<>();
            }
        };

    /**
     * The reflective half of the dispatch — the {@code D:} stub site that
     * {@code gained-calls} fallback rows resolve to. The receiver's own
     * non-JDK member wins when the class declares one — the same contract
     * the {@code $Ov$} marker probe carried — otherwise the registered impl
     * site is invoked.
     *
     * @param member the gained member key, {@code "name desc"}
     * @param impl   the impl site in {@code owner.name(desc)} form
     */
    public static Object dispatchStub(String member, String impl, Object self, Object[] args,
                                      ClassLoader cl) throws Throwable {
        int p = impl.indexOf('(');
        int dot = impl.lastIndexOf('.', p);
        Class<?> implOwner = Class.forName(impl.substring(0, dot).replace('/', '.'), true, cl);
        return dispatchStub(member, implOwner,
            impl.substring(dot + 1, p) + " " + impl.substring(p), self, args, cl);
    }

    /**
     * The impl-owner-in-hand form of {@link #dispatchStub(String, String,
     * Object, Object[], ClassLoader)} — the {@code --noIndy} lowering of a
     * dispatch callsite invokes this directly, so no synthetic frame joins
     * the receiver's stack. {@code args} carries the member args plus any
     * trailing transform extras the impl signature wants — the member path
     * slices to what its signature declares.
     */
    public static Object dispatchStub(String member, Class<?> implOwner, String implSig,
                                      Object self, Object[] args, ClassLoader cl) throws Throwable {
        int sp = member.indexOf(' ');
        final Class<?> rc = self.getClass();
        final String name = member.substring(0, sp);
        final String desc = member.substring(sp + 1);
        java.util.Optional<java.lang.reflect.Method> cached = MEMBER_CACHE.get(rc).computeIfAbsent(member,
            k -> java.util.Optional.ofNullable(ownMember(rc, name, desc, cl)));
        java.lang.reflect.Method m = cached.orElse(null);
        if (m != null && !isJdk(m.getDeclaringClass())) {
            // a public member on a non-public class needs the access crack;
            // a refusal (e.g. a security-manager denial) is surfaced — the
            // receiver declared the member, so silently serving the impl
            // would mask an override it couldn't honor
            if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())
                || !java.lang.reflect.Modifier.isPublic(m.getDeclaringClass().getModifiers())) {
                m.setAccessible(true);
            }
            int n = m.getParameterCount();
            // slice only when the member takes fewer params — padding with
            // nulls on n > args.length would mask the arity mismatch the
            // invoke then reports
            return RetroTypes.invokeResolved(m, self,
                n < args.length ? java.util.Arrays.copyOf(args, n) : args);
        }
        java.lang.reflect.Method impl = IMPL_CACHE.get(implOwner).computeIfAbsent(implSig,
            k -> java.util.Optional.ofNullable(resolveImpl(implOwner, k))).orElse(null);
        if (impl == null) {
            throw MissingStubError.create(
                implOwner.getName().replace('.', '/') + "." + implSig);
        }
        Object[] callArgs = new Object[args.length + 1];
        callArgs[0] = self;
        System.arraycopy(args, 0, callArgs, 1, args.length);
        return RetroTypes.invokeResolved(impl, null, callArgs);
    }

    /**
     * The receiver's own public member — literal param types first, then
     * stand-in-mapped (the receiver's declared signature is corpus-vocabulary:
     * under shade, prefixed stand-ins; under downgrade, the stand-in names).
     */
    private static java.lang.reflect.Method ownMember(Class<?> rc, String name, String desc,
                                                      ClassLoader cl) {
        try {
            return rc.getMethod(name, RetroTypes.paramClasses(desc, cl));
        } catch (Throwable ignore) {
        }
        try {
            return rc.getMethod(name,
                ReflectionTable.paramClasses(desc, rc.getClassLoader()));
        } catch (Throwable ignore) {
        }
        return null;
    }

    /**
     * Whether {@code decl} is part of the JDK's own surface — the member the
     * stub was authored to replace. A declaration on any other class is an
     * override (user class) or the api's own stand-in answer (an api class
     * implementing the member), and both outrank the impl.
     */
    private static boolean isJdk(Class<?> decl) {
        String n = decl.getName();
        return n.startsWith("java.") || n.startsWith("javax.") || n.startsWith("jdk.")
            || n.startsWith("sun.") || n.startsWith("com.sun.")
            || n.startsWith("org.w3c.") || n.startsWith("org.xml.") || n.startsWith("org.ietf.jgss.");
    }

    /**
     * Resolves the impl method on {@code implOwner}. {@code implSig} is
     * {@code "name desc"} in registered vocabulary — the api jar's declared
     * signature differs per packaging, so three forms are tried in turn:
     * literal (an unshaded downgraded jar keeps api names verbatim), then
     * shade-prefixed (the closure relocates {@code xyz/wagyourtail/jvmdg/}
     * types under the run's prefix — derived from {@code implOwner}'s own
     * loaded name so the rewrite always matches), then stand-in-mapped (api
     * signatures written in source vocabulary that this runtime rewrites to
     * stand-ins).
     */
    private static java.lang.reflect.Method resolveImpl(Class<?> implOwner, String implSig) {
        int sp = implSig.indexOf(' ');
        String name = implSig.substring(0, sp);
        String desc = implSig.substring(sp + 1);
        java.lang.reflect.Method m = RetroTypes.resolveDeclared(implOwner, name, desc,
            implOwner.getClassLoader());
        if (m != null) {
            try {
                m.setAccessible(true);
            } catch (RuntimeException e) {
                m = null;
            }
        }
        return m;
    }

    /**
     * Per-callsite polymorphic inline cache — one resolved handle per
     * receiver class. {@code findVirtual} models real virtual dispatch
     * (inherited overrides included) through the <em>caller's</em> lookup, so
     * only members the site could legally invoke resolve; a member the JDK
     * itself serves ({@link #isJdk}) is the thing being stubbed and routes to
     * the impl, as does any resolution failure.
     */
    private static final class Pic extends ClassValue<MethodHandle> {

        private final MethodHandles.Lookup caller;
        private final String name;
        private final MethodType site;
        private final MethodType member;
        private final MethodHandle impl;
        private final Class<?> implOwner;
        private final String implSig;
        private volatile MethodHandle implResolved;

        Pic(MethodHandles.Lookup caller, String name, MethodType site, MethodType member,
            MethodHandle impl, Class<?> implOwner, String implSig) {
            this.caller = caller;
            this.name = name;
            this.site = site;
            this.member = member;
            this.impl = impl == null ? null : MethodHandles.explicitCastArguments(impl, site);
            this.implOwner = implOwner;
            this.implSig = implSig;
        }

        @Override
        protected MethodHandle computeValue(Class<?> rc) {
            try {
                MethodHandle raw = caller.findVirtual(rc, name, member);
                MethodHandle mh = adapt(raw);
                // classify: the JDK's own declaration is the member the stub
                // replaces — only a declaration elsewhere (user override, api
                // stand-in) wins the slot. A lookup that can resolve but not
                // reveal treats the member as the class's own — that's what
                // real dispatch would call.
                try {
                    if (!isJdk(caller.revealDirect(raw).getDeclaringClass())) {
                        return mh;
                    }
                } catch (RuntimeException e) {
                    return mh;
                }
            } catch (Throwable ignore) {
            }
            MethodHandle fb = impl();
            return fb != null ? fb : missingHandle();
        }

        /** drops the trailing transform extras the override's signature lacks, then casts to the site */
        private MethodHandle adapt(MethodHandle mh) {
            int extras = site.parameterCount() - member.parameterCount() - 1;
            if (extras > 0) {
                mh = MethodHandles.dropArguments(mh, member.parameterCount() + 1,
                    site.parameterList().subList(site.parameterCount() - extras,
                        site.parameterCount()));
            }
            // explicitCastArguments, not asType — asType on a varargs-declared
            // member collects a trailing Object site-arg into Object[]{arg}
            return MethodHandles.explicitCastArguments(mh, site);
        }

        /** the impl handle — lazily resolved when it arrived as owner+sig */
        private MethodHandle impl() {
            if (impl != null) {
                return impl;
            }
            if (implResolved == null && implOwner != null) {
                java.lang.reflect.Method m = resolveImpl(implOwner, implSig);
                if (m != null) {
                    try {
                        implResolved = MethodHandles.explicitCastArguments(IMPL_LOOKUP.unreflect(m), site);
                    } catch (Throwable ignore) {
                    }
                }
            }
            return implResolved;
        }

        /**
         * a site-shaped handle that throws {@link MissingStubError} on invoke —
         * built from the {@code throwException} combinator rather than a
         * {@code findStatic} handle so no reflectively-reached member sits in
         * this class for the shade closure to prune
         */
        private MethodHandle missingHandle() {
            return MethodHandles.dropArguments(
                MethodHandles.throwException(site.returnType(), MissingStubError.class)
                    .bindTo(MissingStubError.create(name + member.toMethodDescriptorString())),
                0, site.parameterList());
        }
    }
}
