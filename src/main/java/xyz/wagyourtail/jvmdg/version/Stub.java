package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Stub {

    int opcVers();

    Ref ref() default @Ref("");

}