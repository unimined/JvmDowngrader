package xyz.wagyourtail.jvmdg.version;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.version.map.ClassStubEntry;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MethodRef;
import xyz.wagyourtail.jvmdg.version.map.StubEntry;
import xyz.wagyourtail.jvmdg.version.map.StubRegistry;
import xyz.wagyourtail.jvmdg.version.scan.StubHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The OWN_INTERFACE-adapter member-forwarder contract, shared by both
 * producers so the registrations and the emitted bytecode can't drift:
 * {@link #registerAll} — run post-scan from {@link
 * VersionProvider#ensureInit} once every adapter in the provider has
 * registered — records a member stub per forwardable method pointing at
 * {@code <adapter>$jvmdg$fwd.<name>}, and {@link
 * xyz.wagyourtail.jvmdg.version.gen.MemberForwarderGenerator} emits that
 * class — each forwarder wraps the receiver ({@code $jvmdg$retro.wrap}) and
 * invokeinterfaces the member on the stand-in, which carries the real
 * default body / dispatches to the impl's override.
 * <p>
 * Hand-declared {@code @Stub}s for the same member win (they registered
 * earlier in the class scan) — the generated shape is only the default for
 * the mechanical {@code wrap(x).m(args)} boilerplate.
 */
public final class AdapterForwarders {

    /** sibling-class suffix hosting an adapter's generated member forwarders */
    public static final String SUFFIX = "$jvmdg$fwd";

    private AdapterForwarders() {
    }

    /** generated forwarder host for an adapter's stand-in type */
    public static Type fwdOwner(Type standIn) {
        return Type.getObjectType(standIn.getInternalName() + SUFFIX);
    }

    /**
     * Register {@code <adapter>$jvmdg$fwd.<name>(Object self, args…) ret}
     * member stubs for every {@link #adaptable adaptable} OWN_INTERFACE
     * adapter in the provider. Runs once the scan has seen every adapter —
     * impls pull upward through the adapter-interface tree (a descendant's
     * impls serve ancestor members, mirroring {@code GenContext}'s
     * flattened-wraps contributors), so adaptability isn't decidable while
     * a class's own {@code finishClass} runs: the contributing descendants
     * may not have scanned yet.
     */
    public static void registerAll(VersionProvider vp) {
        for (Map.Entry<Type, ClassStubEntry> e : vp.classStubs.entrySet()) {
            ClassStubEntry stub = e.getValue();
            if (stub.mode != Adapter.Mode.OWN_INTERFACE || !adaptable(stub, vp)) {
                continue;
            }
            List<Method> forwardable = forwardableMethods(stub.clazz());
            if (forwardable.isEmpty()) {
                continue;
            }
            Type fwd = fwdOwner(stub.stubType);
            StubRegistry.Owner stubs = vp.stubRegistry.forOwner(stub.stubType);
            for (Method m : forwardable) {
                MemberNameAndDesc member = member(m);
                if (stubs.methodStub.containsKey(member)) {
                    continue;
                }
                MethodRef handler = new MethodRef(
                    new FullyQualifiedMemberNameAndDesc(fwd, m.getName(), handlerDesc(m)),
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, false, new int[0]);
                if (vp.shouldSkip(handler)) {
                    continue;
                }
                stubs.methodStub.put(member,
                    new StubEntry(handler, false, false, StubCondition.ALWAYS, false, false, new String[0]));
            }
        }
    }

    /**
     * Whether the adapter's wrap path can adapt a foreign receiver at all —
     * an {@code @Adapter(wrap=…)} factory on it, or {@code impls} anywhere
     * in its adapter subtree (its own or a descendant stand-in's — child
     * impls pull up to ancestor membership rows). Without either, {@code
     * wrap} can only pass through adapter-typed objects, so a generated
     * forwarder would do nothing the plain invokeinterface it replaces
     * doesn't: receivers must already be stand-in-typed (nothing can
     * produce a real-member receiver that adapts), and a foreign receiver
     * fails either way.
     * <p>
     * Provider-scoped like the rest of the registry — a descendant adapter
     * authored in a newer provider contributes to its own provider's
     * rows.
     */
    public static boolean adaptable(ClassStubEntry stub, VersionProvider vp) {
        if (stub.wrap != null) {
            return true;
        }
        Class<?> standIn = stub.clazz();
        for (Map.Entry<Type, ClassStubEntry> e : vp.classStubs.entrySet()) {
            List<String[]> impls = vp.adapterImpls.get(e.getKey().getInternalName());
            if (impls == null || impls.isEmpty()) {
                continue;
            }
            if (standIn.isAssignableFrom(e.getValue().clazz())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The adapter interface's forwardable instance methods, sorted by
     * name+desc — the deterministic set both producers enumerate. Static
     * members ({@code jvmdg$wrap} helpers, factories) stay authored, private
     * interface members can't be invokeinterface'd from a sibling class, and
     * synthetic bridges would double-register their erased targets. Only
     * interfaces produce forwarders — a class adapter's declared instance
     * methods aren't invokeinterface'd dispatch targets.
     */
    public static List<Method> forwardableMethods(Class<?> adapter) {
        List<Method> out = new ArrayList<>();
        if (!adapter.isInterface()) {
            return out;
        }
        for (Method m : adapter.getDeclaredMethods()) {
            int mod = m.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isPrivate(mod) || m.isSynthetic()) {
                continue;
            }
            out.add(m);
        }
        out.sort(Comparator.comparing(Method::getName).thenComparing(AdapterForwarders::desc));
        return out;
    }

    private static String desc(Method m) {
        return Type.getMethodDescriptor(m);
    }

    /**
     * The generated handler's signature: {@code (Object self,
     * declared-args…) declared-ret} — declared (stand-in) vocabulary, the
     * same shape a hand-written {@code @Coerce(Adapter) Object self}
     * forwarder carries.
     */
    public static Type handlerDesc(Method m) {
        Type[] declared = Type.getArgumentTypes(m);
        Type[] args = new Type[declared.length + 1];
        args[0] = Type.getType(Object.class);
        System.arraycopy(declared, 0, args, 1, declared.length);
        return Type.getMethodType(Type.getReturnType(m), args);
    }

    /**
     * The registered member key: the source-vocab member signature —
     * adapter-typed params/return coerce to the real JDK types so
     * pre-transform call sites match, exactly as a hand-written
     * {@code @Stub}'s {@code resolveStubTarget} would derive it.
     */
    public static MemberNameAndDesc member(Method m) {
        return new MemberNameAndDesc(m.getName(),
            Type.getMethodType(StubHandler.coercedReturnType(m), StubHandler.coercedParamTypes(m)));
    }

}
