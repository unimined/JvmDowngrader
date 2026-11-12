package xyz.wagyourtail.jvmdg.config;

/**
 * Downgrade policy ({@code Stub-Policy}) — which stub kinds a callsite may
 * use. Resolved per transformed class: a {@code Name:}-scoped rule's
 * {@code Stub-Policy} wins over the global flag for classes in scope.
 */
public enum StubPolicy {
    /** apply every stub (default — today's behavior) */
    FULL,
    /** skip {@code BEHAVIOR} stubs — never wrap an API that exists on the target */
    NO_BEHAVIOR,
    /**
     * {@code LANGUAGE} desugars only — {@code MISSING_API}/{@code BEHAVIOR}
     * hits are vetoed and each {@code MISSING_API} callsite is reported
     * ({@code On-Missing-Api}: error aborts the transform, warn logs it),
     * animal-sniffer style.
     */
    MECHANICAL
}
