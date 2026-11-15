package xyz.wagyourtail.jvmdg.version.map;

import xyz.wagyourtail.jvmdg.version.Stub;
import xyz.wagyourtail.jvmdg.version.StubCondition;

import java.lang.reflect.Method;

/**
 * Registered stub entry: the handler method reference plus the redirect
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
    public final MethodRef handler;
    /** pass the input class-file version as a trailing int arg */
    public final boolean downgradeVersion;
    /** pass an {@code ldc} of the callsite's containing class as a {@code Class} arg, before {@link #downgradeVersion}'s int */
    public final boolean callerClass;
    /** when this entry may fire — {@link StubCondition} */
    public final StubCondition condition;
    /** call site may only rewrite when the runtime transform is available — boolean view of {@code condition == REQUIRES_RUNTIME} */
    public final boolean requiresRuntime;
    /** insert a forwarding body into implementors lacking the method ({@code @DefaultImpl}) */
    public final boolean abstractDefault;
    /** never rewrite {@code INVOKESPECIAL}/{@code NEWINVOKESPECIAL} sites */
    public final boolean noSpecial;
    /** subtype patterns excluded from a supertype-inherited stub */
    public final String[] excludeChild;

    public StubEntry(Method method, Stub stub) {
        this(method, stub.downgradeVersion(), stub.callerClass(),
            StubCondition.of(stub.requiresRuntime(), stub.condition(), "@Stub " + method),
            false, stub.noSpecial(), stub.excludeChild());
    }

    public StubEntry(Method method, boolean downgradeVersion, boolean requiresRuntime,
                     boolean abstractDefault, boolean noSpecial, String[] excludeChild) {
        this(method, downgradeVersion, false, requiresRuntime, abstractDefault, noSpecial, excludeChild);
    }

    public StubEntry(Method method, boolean downgradeVersion, boolean callerClass, boolean requiresRuntime,
                     boolean abstractDefault, boolean noSpecial, String[] excludeChild) {
        this(new MethodRef(method), downgradeVersion, callerClass,
            requiresRuntime ? StubCondition.REQUIRES_RUNTIME : StubCondition.ALWAYS,
            abstractDefault, noSpecial, excludeChild);
    }

    /** the entry form a {@code @DefaultImpl} handler registers */
    public static StubEntry defaultImpl(Method method) {
        return new StubEntry(method, false, false, StubCondition.ALWAYS, true, false, new String[0]);
    }

    /** index-loading form — no reflective object needed */
    public StubEntry(MethodRef handler, boolean downgradeVersion, boolean requiresRuntime,
                     boolean abstractDefault, boolean noSpecial, String[] excludeChild) {
        this(handler, downgradeVersion, false,
            requiresRuntime ? StubCondition.REQUIRES_RUNTIME : StubCondition.ALWAYS,
            abstractDefault, noSpecial, excludeChild);
    }

    /** index-loading form — no reflective object needed */
    public StubEntry(MethodRef handler, boolean downgradeVersion, boolean callerClass, boolean requiresRuntime,
                     boolean abstractDefault, boolean noSpecial, String[] excludeChild) {
        this(handler, downgradeVersion, callerClass,
            requiresRuntime ? StubCondition.REQUIRES_RUNTIME : StubCondition.ALWAYS,
            abstractDefault, noSpecial, excludeChild);
    }

    /** condition-carrying form — scan handlers and the index reader */
    public StubEntry(Method method, boolean downgradeVersion, boolean callerClass, StubCondition condition,
                     boolean abstractDefault, boolean noSpecial, String[] excludeChild) {
        this(new MethodRef(method), downgradeVersion, callerClass, condition, abstractDefault, noSpecial, excludeChild);
    }

    /** condition-carrying form — scan handlers and the index reader */
    public StubEntry(MethodRef handler, boolean downgradeVersion, boolean callerClass, StubCondition condition,
                     boolean abstractDefault, boolean noSpecial, String[] excludeChild) {
        this.handler = handler;
        this.downgradeVersion = downgradeVersion;
        this.callerClass = callerClass;
        this.condition = condition;
        this.requiresRuntime = condition == StubCondition.REQUIRES_RUNTIME;
        this.abstractDefault = abstractDefault;
        this.noSpecial = noSpecial;
        this.excludeChild = excludeChild;
    }

    /** copy with a different {@link #excludeChild} — the build-time derivation unions onto the declared array */
    public StubEntry withExcludeChild(String[] excludeChild) {
        return new StubEntry(handler, downgradeVersion, callerClass, condition, abstractDefault, noSpecial, excludeChild);
    }
}
