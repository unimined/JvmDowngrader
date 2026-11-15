package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Naming contract for override-preserving dispatch: every non-{@code
 * abstractDefault} instance-method stub {@code S.m(Owner, args...)} routes
 * its {@code INVOKEVIRTUAL}/{@code INVOKEINTERFACE} call sites through an
 * {@code invokedynamic} bootstrapped by {@link #DISPATCH_BSM} —
 * {@code DispatchRuntime.bootstrap} builds a per-callsite
 * {@link java.lang.ClassValue} polymorphic inline cache that resolves the
 * receiver's own member via {@code findVirtual} (inherited overrides
 * included) and falls back to the impl handle when the member is
 * unresolvable on that class:
 * <pre>{@code
 * invokedynamic m(Owner, args...)ret
 *   bsm = DispatchRuntime.bootstrap
 *   args = member MethodType, impl-owner Class, impl "name desc" String
 * }
 * </pre>
 * The impl rides as a class + string rather than a {@code Handle} constant
 * because its declared signature differs per packaging — a downgraded api
 * jar rewrites missing JDK types to stand-ins while a raw shaded jar keeps
 * them — so the runtime resolves it lazily, and a type that can't load on
 * the running JVM stays a per-callsite failure instead of an eagerly-linked
 * {@code <clinit>} poison under {@code --noIndy} lowering.
 * No marker interface is injected into user classes — dispatch sees the
 * member the class actually declares — and {@code INVOKESPECIAL} sites keep
 * the impl ({@code super.m()} must not bounce back to {@code this}). The
 * {@code gained-calls} resource's stub column names the same contract
 * reflectively: a {@code D:} prefix on the {@code owner.name(desc)} site
 * tells {@code RetroTypes.invokeStub} to run the override-then-impl
 * resolution via {@code DispatchRuntime.dispatchStub}.
 */
public final class DispatchSites {

    /** internal name of the shared (ASM-free) dispatch runtime */
    public static final String RUNTIME = "xyz/wagyourtail/jvmdg/stub/DispatchRuntime";

    /**
     * The bootstrap every instance-stub {@code invokedynamic} site names:
     * {@code (Lookup, String, MethodType, MethodType, Class, String)CallSite}
     * on {@link #RUNTIME}.
     */
    public static final Handle DISPATCH_BSM = new Handle(Opcodes.H_INVOKESTATIC,
        RUNTIME, "bootstrap",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
            "Ljava/lang/invoke/MethodType;Ljava/lang/Class;Ljava/lang/String;)" +
            "Ljava/lang/invoke/CallSite;",
        false);

    /**
     * Whether a member-stub entry is instance-shaped — the handler takes the
     * receiver as arg 0, followed by the member's args and a trailing input
     * {@code int} when {@link StubEntry#downgradeVersion} — and so gets a
     * dispatcher. Constructor stubs live in a separate map, static stubs have
     * matching arg counts, and {@code abstractDefault} members are the
     * {@code @DefaultImpl}/insert-abstracts machinery, not callsite stubs.
     */
    public static boolean isInstanceStub(StubEntry entry, MemberNameAndDesc member) {
        if (entry.abstractDefault || !member.isMethod()) {
            return false;
        }
        Type[] handler = entry.handler.member.getDesc().getArgumentTypes();
        if (handler.length == 0 || !GainedSites.isRef(handler[0])) {
            return false;
        }
        // arity-only discrimination: a static member whose handler is
        // (ref-arg0, caller-Class) with callerClass alone satisfies the
        // count — harmless: INVOKESTATIC sites never take the dispatch route
        // (the opcode gates it), so the worst case is an unused dispatcher
        return handler.length == member.getDesc().getArgumentTypes().length + 1
            + (entry.callerClass ? 1 : 0) + (entry.downgradeVersion ? 1 : 0);
    }

    /**
     * The {@code invokedynamic} an instance-stub call site emits:
     * {@code indy member-name (handler-desc)ret} with {@code member}'s own
     * MethodType, the impl's owner class, and the impl's {@code "name desc"}
     * in registered vocabulary. The class args are remapped to adapter
     * vocabulary at emission — the emitted node may sit at the instruction
     * loop's already-consumed index and skip the bsmArg remap pass — and
     * still ride the ordinary constant-pool remap so prefix shading keeps
     * them pointing at the jar's own copies. The sig string deliberately
     * stays in source vocabulary: the runtime tries the literal signature
     * first (shaded api jars keep it) then the stand-in-mapped one
     * (downgraded api jars hold that).
     */
    public static org.objectweb.asm.tree.InvokeDynamicInsnNode insn(MemberNameAndDesc member,
                                                                    MethodRef handler,
                                                                    TypeRemap remap,
                                                                    java.util.Set<String> warnings) {
        return new org.objectweb.asm.tree.InvokeDynamicInsnNode(
            member.getName(),
            handler.member.getDesc().getDescriptor(),
            DISPATCH_BSM,
            remap.remap(member.getDesc(), warnings),
            remap.remap(handler.member.getOwner(), warnings),
            handler.member.getName() + " " + handler.member.getDesc().getDescriptor());
    }

    /**
     * The {@code invokedynamic} a signature-polymorphic callsite emits when
     * the member-stub lookup missed and the owner's {@code @Adapter} lists
     * the site name in {@code signaturePolymorphic}:
     * {@code indy site-name (stub-type, site-args...)site-ret} with the
     * adapter's {@code jvmdg$bootstrap} hook ({@link ClassStubEntry#polymorphic})
     * as the bsm and no bootstrap args — the callsite name+desc carry
     * everything the hook needs to derive the impl signature at link time
     * (VarHandle's {@code get*}→typecode mangle, reference-arg erasure,
     * CAE/getAndUpdate primitive returns; {@code asType} performs the
     * casts/boxes a {@code @Modify} splice used to emit inline). Site
     * arg/ret types are remapped here like {@link #insn}'s bsmArgs — the
     * node lands at the consumed index and the loop's remap pass skips it.
     */
    public static org.objectweb.asm.tree.InvokeDynamicInsnNode polymorphicInsn(
            ClassStubEntry stub, org.objectweb.asm.tree.MethodInsnNode site,
            TypeRemap remap, java.util.Set<String> warnings) {
        Type siteType = Type.getMethodType(site.desc);
        Type[] args = siteType.getArgumentTypes();
        Type[] indyArgs = new Type[args.length + 1];
        indyArgs[0] = stub.stubType;
        for (int i = 0; i < args.length; i++) {
            indyArgs[i + 1] = remap.remap(args[i], warnings);
        }
        return new org.objectweb.asm.tree.InvokeDynamicInsnNode(
            site.name,
            Type.getMethodDescriptor(remap.remap(siteType.getReturnType(), warnings), indyArgs),
            new Handle(Opcodes.H_INVOKESTATIC,
                stub.polymorphic.getOwner().getInternalName(),
                stub.polymorphic.getName(),
                stub.polymorphic.getDesc().getDescriptor(),
                false));
    }

    private DispatchSites() {
    }
}
