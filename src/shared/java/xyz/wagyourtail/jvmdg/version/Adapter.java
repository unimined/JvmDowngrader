package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Adapter {

    String value();

    String target() default "";

    /**
     * if false, will completely remove interface, rather than remapping.
     * if this is on an annotation, it will retain the original annotation.
     */
    boolean keepInterface() default true;

}