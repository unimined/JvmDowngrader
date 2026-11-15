package xyz.wagyourtail.jvmdg.stub;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Probe point for capabilities that live outside the api jar. Stub impls
 * that optionally need the heavy downgrader core ask here instead of
 * hand-probing {@code Class.forName("xyz.wagyourtail.jvmdg.ClassDowngrader")};
 * discovery is {@link ServiceLoader}-driven, so presence of the core jar's
 * {@code META-INF/services} registration is what makes the backend visible —
 * no constant-pool edge from api classes to core types.
 * <p>
 * When the backend is absent the caller falls back to the same-JVM behavior
 * (e.g. defining the bytes as-is), which is correct whenever the input bytes
 * are already at or below the target class version.
 */
public final class OptionalDependency {

    private static volatile Optional<RuntimeDowngrader> downgrader;

    private OptionalDependency() {
    }

    /**
     * @return the core-jar downgrade backend when the core is on the same
     *         classpath as this class, or empty otherwise
     */
    public static Optional<RuntimeDowngrader> downgrader() {
        Optional<RuntimeDowngrader> d = downgrader;
        if (d == null) {
            synchronized (OptionalDependency.class) {
                d = downgrader;
                if (d == null) {
                    downgrader = d = resolve();
                }
            }
        }
        return d;
    }

    private static Optional<RuntimeDowngrader> resolve() {
        try {
            for (RuntimeDowngrader impl : ServiceLoader.load(
                    RuntimeDowngrader.class, OptionalDependency.class.getClassLoader())) {
                return Optional.of(impl);
            }
        } catch (Throwable ignored) {
            // no/!broken provider — treat as absent
        }
        return Optional.empty();
    }

}
