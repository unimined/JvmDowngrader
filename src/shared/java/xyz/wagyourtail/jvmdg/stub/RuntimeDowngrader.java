package xyz.wagyourtail.jvmdg.stub;

import xyz.wagyourtail.jvmdg.util.Function;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Backend capability for stub impls that genuinely need runtime bytecode
 * transformation (e.g. {@code Lookup.defineClass}/{@code defineHiddenClass},
 * which receive bytes that may be above the running JVM's class version).
 * <p>
 * Implemented by the core jar when present ({@code CoreRuntimeDowngrader},
 * delegated to {@code ClassDowngrader}); resolved through
 * {@link OptionalDependency#downgrader()} so api-jar classes never carry a
 * hard reference to core types — the api jar stays ASM-free and the lookup
 * is a {@link java.util.ServiceLoader} probe, not a constant-pool edge.
 */
public interface RuntimeDowngrader {

    /**
     * Downgrades {@code bytes} like
     * {@code ClassDowngrader#downgrade(AtomicReference, byte[], boolean, Function)}.
     *
     * @param name         out-param receiving the class's internal name
     * @param bytes        raw class bytes
     * @param enableRuntime whether the runtime transformer is present
     * @param getExtraRead reads classpath resources ({@code name.class}) the
     *                     transform may need to inspect
     * @return all output entries ({@code <name>.class → bytes})
     */
    Map<String, byte[]> downgrade(AtomicReference<String> name, byte[] bytes,
                                  boolean enableRuntime, Function<String, byte[]> getExtraRead)
        throws IllegalClassFormatException;

}
