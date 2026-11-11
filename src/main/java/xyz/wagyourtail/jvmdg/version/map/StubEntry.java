package xyz.wagyourtail.jvmdg.version.map;

import xyz.wagyourtail.jvmdg.version.Stub;

import java.lang.reflect.Method;

/**
 * Registered stub entry: the handler {@link Method} plus the redirect
 * modifiers copied off its annotation at registration. This is data, not the
 * annotation — handlers for every primary annotation ({@code @Stub},
 * {@code @DefaultImpl}) produce the same shape, which is what lets
 * {@code stubs.idx} feed the registry without reflecting on the annotation
 * types. Constructor callsite entries ({@code @InitFactory} /
 * {@code @InitRewrite}) are {@link CtorEntry}s in the owner's separate ctor
 * map.
 */
public class StubEntry {

    /** the {@code public static} implementation invoked in place of the target member */
    public final Method method;
    /** pass the input class-file version as a trailing int arg */
    public final boolean downgradeVersion;
    /** call site may only rewrite when the runtime transform is available */
    public final boolean requiresRuntime;
    /** insert a forwarding body into implementors lacking the method ({@code @DefaultImpl}) */
    public final boolean abstractDefault;
    /** never rewrite {@code INVOKESPECIAL}/{@code NEWINVOKESPECIAL} sites */
    public final boolean noSpecial;
    /** subtype patterns excluded from a supertype-inherited stub */
    public final String[] excludeChild;

    public StubEntry(Method method, Stub stub) {
        this(method, stub.downgradeVersion(), stub.requiresRuntime(), false,
            stub.noSpecial(), stub.excludeChild());
    }

    /** the entry form a {@code @DefaultImpl} handler registers */
    public static StubEntry defaultImpl(Method method) {
        return new StubEntry(method, false, false, true, false, new String[0]);
    }

    public StubEntry(Method method, boolean downgradeVersion, boolean requiresRuntime,
                     boolean abstractDefault, boolean noSpecial, String[] excludeChild) {
        this.method = method;
        this.downgradeVersion = downgradeVersion;
        this.requiresRuntime = requiresRuntime;
        this.abstractDefault = abstractDefault;
        this.noSpecial = noSpecial;
        this.excludeChild = excludeChild;
    }
}
