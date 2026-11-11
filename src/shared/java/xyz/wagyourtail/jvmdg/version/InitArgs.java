package xyz.wagyourtail.jvmdg.version;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicit container for repeatable {@link InitArg} — what javac emits for
 * {@code @Repeatable} sugar, and the only way to bind one converter to
 * several {@link InitRewrite}s on source levels below Java 8 (e.g. the same
 * {@code msg(Throwable)} filling a slot for both the
 * {@code (String,Throwable)} and {@code (Throwable)} ctor overloads). The
 * scanner reads both forms.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InitArgs {

    InitArg[] value();

}
