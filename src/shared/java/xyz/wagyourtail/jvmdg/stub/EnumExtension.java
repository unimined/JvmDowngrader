package xyz.wagyourtail.jvmdg.stub;

/**
 * ServiceLoader SPI for eager enum-constant injection.
 * <p>
 * Implementations live in the api jar (one per versioned stub set, e.g.
 * {@code j16.impl.StandardProtocolFamilyExtension}) and are listed in
 * {@code META-INF/services/xyz.wagyourtail.jvmdg.stub.EnumExtension}. The agent
 * {@code Bootstrap} loads them at premain so the universe is extended before
 * any consumer bytecode runs; stub impls may also call {@link #extend()}
 * lazily as an ordering guard on the non-agent path.
 * <p>
 * Implementations must be idempotent ({@link EnumExtender#extend} already is)
 * and side-effect free beyond the injection itself.
 */
public interface EnumExtension {

    /**
     * Perform this extension's enum injections via {@link EnumExtender}.
     */
    void extend();

}
