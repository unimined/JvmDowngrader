package xyz.wagyourtail.jvmdg.config;

/**
 * What {@code Stub-Policy: mechanical} does with a {@code MISSING_API}
 * callsite — error aborts the transform naming the site and the Java
 * version the member needs; warn keeps building and reports it.
 */
public enum OnMissingApi {
    ERROR,
    WARN
}
