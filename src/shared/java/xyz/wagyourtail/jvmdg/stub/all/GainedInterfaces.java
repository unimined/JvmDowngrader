package xyz.wagyourtail.jvmdg.stub.all;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Records interfaces the downgrade transform injected into a class's
 * {@code interfaces} list to restore gained-supertype membership (e.g. a
 * supertype that became {@code AutoCloseable} at a later release). The
 * source never declared them, so {@link Class#getInterfaces}/
 * {@link Class#getGenericInterfaces} stubs filter them back out; values
 * are binary class names ({@code java.lang.AutoCloseable}) matching
 * {@link Class#getName}. Sibling of {@link RemovedInterfaces} — that one
 * records what the transform took away, this records what it added.
 * Generated {@code $Marker}/{@code $Ov$} interfaces are filtered by name
 * pattern instead and are NOT listed here.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface GainedInterfaces {
    String[] value();
}
