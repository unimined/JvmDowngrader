package xyz.wagyourtail.jvmdg.j8.intl;

/**
 * Internal runtime helpers for bytecode sequences emitted by jvmdg {@code @Modify}
 * handlers. Transformed user code invokes these methods directly, so they must
 * remain {@code public} and must not be renamed.
 */
public class ExceptionHelper {
    private ExceptionHelper() {
    }

    /**
     * Replicates the {@code detailMessage} computation of
     * {@link Throwable#Throwable(Throwable)}: the cause's {@code toString()},
     * or {@code null} when the cause is {@code null}.
     *
     * @param cause the throwable passed to the emulated constructor
     * @return the detail message for the emulated constructor
     */
    public static String causeMessage(Throwable cause) {
        return cause == null ? null : cause.toString();
    }
}
