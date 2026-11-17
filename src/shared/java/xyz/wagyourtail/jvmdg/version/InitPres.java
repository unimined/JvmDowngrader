package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicit container for repeatable {@link InitPre} — the pre-Java-8 spelling
 * of several {@code @InitPre} annotations on one method, for hooks shared
 * across constructor overloads.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InitPres {

    InitPre[] value();

}
