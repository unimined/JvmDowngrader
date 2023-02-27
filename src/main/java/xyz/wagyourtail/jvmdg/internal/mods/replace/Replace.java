package xyz.wagyourtail.jvmdg.internal.downgraders.replace;

import org.gradle.api.JavaVersion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Replace {
    JavaVersion value();
    String desc() default "";
    boolean idBSM() default false;
}