package xyz.wagyourtail.jvmdg.j16;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RecordComponents {

    RecordComponents.Value[] value();

    @interface Value {
        String name();
        Class<?> type();
    }

}
