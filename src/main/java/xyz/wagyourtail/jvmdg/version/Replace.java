package xyz.wagyourtail.jvmdg.version;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Replace {

    int javaVersion();

    Ref ref() default @Ref("");

    boolean idBSM() default false;

}