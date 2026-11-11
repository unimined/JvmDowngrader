package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.Adapter;

import java.util.Set;

/**
 * The forward type-remap surface a structural stage consumes: which fake
 * (adapter) type a real JDK type maps to, plus the coverage hook for types
 * that have no mapping. Implemented by {@code VersionProvider} today; in the
 * pipeline engine this is the narrow contract a remap stage is handed rather
 * than the whole registrar/provider.
 */
public interface TypeRemap {

    /**
     * Remaps {@code type} through this remap's class stubs — object types
     * become their adapter types, method/array types remap recursively.
     * Identity for types with no stub. Does not warn; reporting an unstubbed
     * type is the caller's choice via {@link #warnUnstubbed}.
     */
    Type remap(Type type, Set<String> warnings);

    /**
     * The registered stub for {@code type} — adapter type plus backing stub
     * class — or null when the type has no stub.
     */
    Pair<Type, Pair<Class<?>, Adapter>> stubFor(Type type);

    /**
     * Reports {@code type} as landing in transformed output with no stub, so
     * the coverage report and warning set record it.
     */
    void warnUnstubbed(Type type, Set<String> warnings);
}
