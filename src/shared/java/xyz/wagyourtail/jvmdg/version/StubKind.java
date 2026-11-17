package xyz.wagyourtail.jvmdg.version;

/**
 * What a registered stub entry emulates, classified per downgrade run —
 * {@code MISSING_API} vs {@code BEHAVIOR} depends on whether the member
 * exists on the <em>target</em> JDK, so entries carry their introduction
 * version ({@code since}, from the build-time {@code ct.sym} index) and the
 * kind resolves at emission.
 * <p>
 * {@link #AUTO} is the registration-time default: memberModify entries
 * classify {@link #LANGUAGE}; member stubs classify by {@code since}.
 */
public enum StubKind {
    /** derive from entry type + {@code since} at classification time */
    AUTO,
    /** member absent on the target JDK — the stub supplies it */
    MISSING_API,
    /** member exists on the target JDK — the stub intercepts for newer semantics */
    BEHAVIOR,
    /** language-feature desugar ({@code @Modify}, indy/condy rewrites) — not an API mirror */
    LANGUAGE
}
