package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Type;

/**
 * Naming contract for generated gained-supertype dispatchers —
 * {@code xyz/wagyourtail/jvmdg/gen/gained/<mangled-I>} plus its {@code $Wrap}
 * sibling — shared by the api-jar generator (emit side) and the transforms
 * (site side) without either depending on the other. Member dispatch
 * methods are {@code <name>$<mangled-desc>} where the mangle is an
 * injective encoding of the member's real descriptor: erased-shape
 * overloads ({@code forEachRemaining(Consumer)} vs
 * {@code forEachRemaining(DoubleConsumer)}) are common on gained
 * interfaces, so the name has to carry the desc to stay unique. Sites
 * compute the same name from their own (post-remap) desc — an adapter
 * remapping inside the member's signature makes the names diverge, which
 * is honest: the member can't be invoked with adapter-typed args anyway.
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

    /**
     * The dispatcher's dispatch-method name for a member site:
     * {@code <name>$<mangled-desc>}. {@code desc} is whatever the site
     * carries — the generator passes the real member descriptor.
     */
    public static String dispatchName(String name, String desc) {
        return name + "$" + mangleDesc(desc);
    }

    /**
     * Injective descriptor → legal-method-name encoding. JVM unqualified
     * names forbid {@code . ; [ / < >}; descriptors only contain {@code ;}
     * {@code /} {@code [} of those, and {@code $} is the escape leader.
     * {@code (} {@code )} are legal and left raw for readability.
     */
    public static String mangleDesc(String desc) {
        StringBuilder sb = new StringBuilder(desc.length() + 8);
        for (int i = 0; i < desc.length(); i++) {
            char c = desc.charAt(i);
            switch (c) {
                case '$': sb.append("$$"); break;
                case ';': sb.append("$S"); break;
                case '/': sb.append("$P"); break;
                case '[': sb.append("$A"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * The erased-shape descriptor a site emits and the dispatcher declares:
     * {@code (Ljava/lang/Object;<args>I)<ret>} where every reference arg
     * becomes {@code Object} (site arg types are post-remap, so only the
     * sort survives contract), primitives stay primitive, and the return
     * is {@code Object} for reference returns (the site checkcasts),
     * primitive for primitives, {@code V} for void.
     */
    public static String dispatchDesc(Type siteDesc) {
        StringBuilder sb = new StringBuilder("(Ljava/lang/Object;");
        for (Type a : siteDesc.getArgumentTypes()) {
            sb.append(isRef(a) ? "Ljava/lang/Object;" : a.getDescriptor());
        }
        sb.append("I)");
        Type ret = siteDesc.getReturnType();
        return sb.append(isRef(ret) ? "Ljava/lang/Object;" : ret.getDescriptor()).toString();
    }

    public static boolean isRef(Type t) {
        return t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY;
    }

    private GainedSites() {
    }
}
