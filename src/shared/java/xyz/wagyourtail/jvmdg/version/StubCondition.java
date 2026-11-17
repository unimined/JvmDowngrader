package xyz.wagyourtail.jvmdg.version;

/**
 * Activation condition of a registered stub entry — generalizes the legacy
 * {@code requiresRuntime} boolean so a member can gate on more than one
 * runtime prerequisite. The entry still carries {@code requiresRuntime} as
 * the boolean view of {@link #REQUIRES_RUNTIME}; the condition is the
 * authoritative value serialized into {@code stubs.idx}.
 *
 * @since 3.1.0
 */
public enum StubCondition {
    /** active whenever the member resolves — the default */
    ALWAYS,
    /** active only when the runtime transform (agent/classloader path) is available */
    REQUIRES_RUNTIME,
    /**
     * reserved for preview-only stubs: active only when the run opted in via
     * {@code Flags.allowPreview} — such entries are registered but never fire
     * on ordinary inputs.
     */
    PREVIEW;

    /**
     * Resolves the annotation pair {@code (requiresRuntime, condition)} to a
     * single condition: an explicit condition wins; the legacy boolean maps
     * to {@link #REQUIRES_RUNTIME}. Declaring both a non-{@link #ALWAYS}
     * condition and the flag is a registration error.
     */
    public static StubCondition of(boolean requiresRuntime, StubCondition declared, String what) {
        if (declared != ALWAYS && requiresRuntime) {
            throw new IllegalArgumentException(what +
                " declares both requiresRuntime=true and condition=" + declared);
        }
        return declared != ALWAYS ? declared
            : (requiresRuntime ? REQUIRES_RUNTIME : ALWAYS);
    }

    /**
     * Whether this entry may fire under the given run conditions.
     *
     * @param runtimeAvailable {@code enableRuntime} of the run (agent/classloader transform available)
     * @param allowPreview     {@code Flags.allowPreview} of the run
     */
    public boolean active(boolean runtimeAvailable, boolean allowPreview) {
        switch (this) {
            case REQUIRES_RUNTIME:
                return runtimeAvailable;
            case PREVIEW:
                return allowPreview;
            default:
                return true;
        }
    }
}
