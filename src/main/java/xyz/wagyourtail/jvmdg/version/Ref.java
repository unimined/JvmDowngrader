package xyz.wagyourtail.jvmdg.version;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Ref {

    String value();

    String member() default "";

    String desc() default "";

}
