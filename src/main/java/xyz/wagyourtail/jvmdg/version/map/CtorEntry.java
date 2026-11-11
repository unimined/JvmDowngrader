package xyz.wagyourtail.jvmdg.version.map;

/**
 * Base for registered constructor callsite rewrites. Ctor entries live in the
 * owner's {@code ctor} map, separate from member redirects: an
 * {@code <init>} call is keyed to its exact declared owner — a
 * {@code new Sub(...)} must never resolve {@code Super}'s ctor rewrite — so
 * they skip the parent-walking member resolution entirely.
 */
public abstract class CtorEntry {

    /** call site may only rewrite when the runtime transform is available */
    public final boolean requiresRuntime;

    protected CtorEntry(boolean requiresRuntime) {
        this.requiresRuntime = requiresRuntime;
    }

    /**
     * the handler methods this entry invokes at call sites — the factory
     * method for {@link CtorFactory}, the bound {@code @InitArg}/
     * {@code @InitPre}/{@code @InitPost} methods for {@link CtorRewrite}.
     * Coverage/corpus consumers use this to mark handler methods exercised.
     */
    public abstract java.lang.reflect.Method[] methods();
}
