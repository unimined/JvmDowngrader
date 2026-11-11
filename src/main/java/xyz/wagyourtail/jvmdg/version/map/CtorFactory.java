package xyz.wagyourtail.jvmdg.version.map;

import java.lang.reflect.Method;

/**
 * {@code @InitFactory} registration: a verified {@code NEW}/{@code DUP}
 * construction site is replaced by {@code INVOKESTATIC} of
 * {@link #method}, whose return supplies the object.
 */
public class CtorFactory extends CtorEntry {

    /** the {@code public static} factory returning the constructed object */
    public final Method method;
    /** pass the input class-file version as a trailing int arg */
    public final boolean downgradeVersion;

    public CtorFactory(Method method, boolean downgradeVersion, boolean requiresRuntime) {
        super(requiresRuntime);
        this.method = method;
        this.downgradeVersion = downgradeVersion;
    }

    @Override
    public Method[] methods() {
        return new Method[]{method};
    }
}
