package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Naming contract for generated gained-supertype dispatchers —
 * {@code xyz/wagyourtail/jvmdg/gen/gained/<mangled-I>} plus its {@code $Wrap}
 * sibling — shared by the api-jar generator (emit side) and the transforms
 * (site side) without either depending on the other. Member call sites are
 * {@code invokedynamic} bootstrapped by {@link #GAINED_BSM} — the row-keyed
 * {@code "name desc"} rides the bootstrap args, so erased-shape overloads
 * ({@code forEachRemaining(Consumer)} vs
 * {@code forEachRemaining(DoubleConsumer)}) stay distinct without a
 * name-mangle.
 */
public final class GainedSites {

    public static final String PKG = "xyz/wagyourtail/jvmdg/gen/gained/";

    /** the api-side dispatcher owner for a gained interface's emitted sites */
    public static String dispatchOwner(String ifaceInternalName) {
        return PKG + mangleType(ifaceInternalName);
    }

    /** {@code java/lang/AutoCloseable} → {@code J_L_AutoCloseable} — the api's segment-initial convention */
    private static String mangleType(String internalName) {
        StringBuilder sb = new StringBuilder();
        int last = 0;
        for (int i = 0; i < internalName.length(); i++) {
            if (internalName.charAt(i) == '/') {
                sb.append(Character.toUpperCase(internalName.charAt(last))).append('_');
                last = i + 1;
            }
        }
        return sb.append(internalName.substring(last)).toString();
    }

    public static boolean isRef(Type t) {
        return t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY;
    }

    /** internal name of the shared (ASM-free) gained-dispatch runtime */
    public static final String RUNTIME = "xyz/wagyourtail/jvmdg/stub/runtime/GainedRuntime";

    /**
     * Whether {@code owner} (the containing class) is dispatch machinery —
     * the shared {@code xyz/wagyourtail/jvmdg/stub/} runtime classes and the
     * generated siblings {@link StructuralRemapTransform#isGeneratedDispatch}
     * already covers. An indy emitted inside them bootstraps through the
     * very code path executing it — {@code GainedRuntime.bootstrap} calling
     * {@code List.subList} re-linked through {@code bootstrap} and recursed
     * to {@link BootstrapMethodError} — so machinery classes keep the
     * native site (all their members resolve natively anyway).
     */
    public static boolean isDispatchMachinery(String owner) {
        return owner.startsWith("xyz/wagyourtail/jvmdg/stub/")
            || StructuralRemapTransform.isGeneratedDispatch(owner);
    }

    /**
     * The bootstrap an indy-mode gained callsite names:
     * {@code (Lookup, String, MethodType, String, String, int)CallSite} on
     * {@link #RUNTIME} — args are {@code iName}, the row-keyed
     * {@code "name desc"} member, and the calling class's authored version
     * (the membership ceiling, moved out of the callsite signature).
     */
    public static final org.objectweb.asm.Handle GAINED_BSM = new org.objectweb.asm.Handle(
        Opcodes.H_INVOKESTATIC, RUNTIME, "bootstrap",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
            "Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/invoke/CallSite;",
        false);

    /**
     * The erased-shape descriptor an indy gained callsite emits:
     * {@code (Ljava/lang/Object;<args>)<ret>} with reference args erased
     * to {@code Object} (site arg types are post-remap, so only the sort
     * survives contract), primitives raw, and reference returns as
     * {@code Object} (the site still checkcasts) — the membership
     * ceiling rides the bootstrap args instead of the signature.
     */
    public static String indyDesc(Type siteDesc) {
        StringBuilder sb = new StringBuilder("(Ljava/lang/Object;");
        for (Type a : siteDesc.getArgumentTypes()) {
            sb.append(isRef(a) ? "Ljava/lang/Object;" : a.getDescriptor());
        }
        sb.append(')');
        Type ret = siteDesc.getReturnType();
        return sb.append(isRef(ret) ? "Ljava/lang/Object;" : ret.getDescriptor()).toString();
    }

    private GainedSites() {
    }
}
