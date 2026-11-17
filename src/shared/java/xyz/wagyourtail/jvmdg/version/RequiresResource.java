package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Resource paths a member (or class, at TYPE level) needs at runtime —
 * shading carries them into the output jar when the annotated element is
 * required by the api reference closure.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface RequiresResource {
    String[] value();

}
