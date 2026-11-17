package xyz.wagyourtail.jvmdg.version;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a constructor/method/field on a stub or {@link Adapter} class as
 * jvmdg-internal API surface — it does not mirror a JDK member, so the
 * coverage checker doesn't expect a JDK counterpart (not listed in
 * unmatched.txt).
 * <p>
 * Not valid on members carrying a primary stub annotation
 * ({@link Stub}, {@link Modify}, ctor annotations) — those are classified
 * by {@link StubKind} instead.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface Internal {
}
