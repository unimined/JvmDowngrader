package xyz.wagyourtail.jvmdg.version.map;

import xyz.wagyourtail.jvmdg.version.StubCondition;

/**
 * Base for registered constructor callsite rewrites. Ctor entries live in the
 * owner's {@code ctor} map, separate from member redirects: an
 * {@code <init>} call is keyed to its exact declared owner — a
 * {@code new Sub(...)} must never resolve {@code Super}'s ctor rewrite — so
 * they skip the parent-walking member resolution entirely.
 */
public abstract class CtorEntry {

    /** when this entry may fire — {@link StubCondition} */
    public final StubCondition condition;
    /** call site may only rewrite when the runtime transform is available — boolean view of {@code condition == REQUIRES_RUNTIME} */
    public final boolean requiresRuntime;

    protected CtorEntry(boolean requiresRuntime) {
        this(requiresRuntime ? StubCondition.REQUIRES_RUNTIME : StubCondition.ALWAYS);
    }

    protected CtorEntry(StubCondition condition) {
        this.condition = condition;
        this.requiresRuntime = condition == StubCondition.REQUIRES_RUNTIME;
    }

    /**
     * the handler methods this entry invokes at call sites — the factory
     * method for {@link CtorFactory}, the bound {@code @InitArg}/
     * {@code @InitPre}/{@code @InitPost} methods for {@link CtorRewrite}.
     * Coverage/corpus consumers use this to mark handler methods exercised.
     */
    public abstract MethodRef[] methods();
}
