package xyz.wagyourtail.jvmdg.version;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a constructor/method/field as ignored inside an {@link Adapter} class, so it isn't included in unmatched.txt
 *
 * @deprecated renamed to {@link Internal}; kept as an alias for one release.
 */
@Deprecated
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface CoverageIgnore {
}
