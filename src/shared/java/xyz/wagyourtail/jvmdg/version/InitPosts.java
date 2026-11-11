package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicit container for repeatable {@link InitPost} — the pre-Java-8
 * spelling of several {@code @InitPost} annotations on one method, for tails
 * shared across constructor overloads (the {@code (String,Throwable)} and
 * {@code (Throwable)} exception ctors share one {@code initCause} tail).
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InitPosts {

    InitPost[] value();

}
