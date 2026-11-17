package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicit container for repeatable {@link InitRewrite} — what javac emits
 * for {@code @Repeatable} sugar, and the only way to declare several
 * rewrites on source levels below Java 8. Read by the scanner via
 * {@code getAnnotationsByType}, so both forms register identically.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InitRewrites {

    InitRewrite[] value();

}
