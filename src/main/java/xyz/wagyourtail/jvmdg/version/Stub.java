package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Stub {

    /**
     * reference to the stub target, if it can't be auto-resolved.
     */
    Ref ref() default @Ref("");

    /**
     * if the input version in the VersionProvider should be pushed onto the stack before the stub is called.
     */
    boolean downgradeVersion() default false;

    /**
     * if the stub requires the runtime to be loaded to work properly.
     */
    boolean requiresRuntime() default false;

    /**
     * if was abstract/default and need to insert as default implementation while downgrading.
     */
    boolean abstractDefault() default false;

    boolean noSpecial() default false;

}