package xyz.wagyourtail.jvmdg.version.map;

import java.lang.reflect.Method;

/**
 * {@code @InitFactory} registration: a verified {@code NEW}/{@code DUP}
 * construction site is replaced by {@code INVOKESTATIC} of
 * {@link #handler}, whose return supplies the object.
 */
public class CtorFactory extends CtorEntry {

    /** the {@code public static} factory returning the constructed object */
    public final MethodRef handler;
    /** pass the input class-file version as a trailing int arg */
    public final boolean downgradeVersion;

    public CtorFactory(Method method, boolean downgradeVersion, boolean requiresRuntime) {
        this(new MethodRef(method), downgradeVersion, requiresRuntime);
    }

    /** index-loading form — no reflective object needed */
    public CtorFactory(MethodRef handler, boolean downgradeVersion, boolean requiresRuntime) {
        super(requiresRuntime);
        this.handler = handler;
        this.downgradeVersion = downgradeVersion;
    }

    @Override
    public MethodRef[] methods() {
        return new MethodRef[]{handler};
    }
}
