package xyz.wagyourtail.jvmdg.config;

/**
 * Which {@code reflection.idx} entries a shaded output ships for dynamic
 * (non-constant-folded) reflection sites:
 *
 * <ul>
 *   <li>{@code NONE} — ship no entries; every dynamic lookup misses and
 *       answers the native JVM behavior ({@code NoSuchMethodException}/
 *       {@code ClassNotFoundException} exactly as the old runtime gives).</li>
 *   <li>{@code USED_OWNERS} — keep entries whose stubbed JDK owner the input
 *       references; those entries' impl classes become closure roots so the
 *       reflective call can actually reach them. The default.</li>
 *   <li>{@code ALL} — keep every entry; every impl class is a closure root.
 *       Largest output, least pruning.</li>
 * </ul>
 *
 * <p>Explicit keeps ({@code Shade-Keep-Reflection} / {@code --shadeKeepReflection})
 * union additional owner-member patterns into the {@code USED_OWNERS} root
 * set.</p>
 */
public enum ShadeReflection {
    NONE,
    USED_OWNERS,
    ALL
}
