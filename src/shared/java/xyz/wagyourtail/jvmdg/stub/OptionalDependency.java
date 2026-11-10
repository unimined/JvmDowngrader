package xyz.wagyourtail.jvmdg.stub;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Flags a stub-support delegate as requiring an optional third-party library
 * on the consumer's classpath ("when present" stubs). The delegate class is
 * compiled against the library ({@code compileOnly}) so its body is typed code;
 * {@link OptionalDelegate#get(Class)} reads this annotation, probes the listed
 * classes, and only instantiates the delegate when all resolve — keeping the
 * api jar free of hard references to absent libraries.
 * <p>
 * Example: {@code @OptionalDependency(value = "org.newsclub.net.unix.AFUNIXSelectorProvider",
 * name = "junixsocket", maven = "com.kohlschutter.junixsocket:junixsocket-core")}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OptionalDependency {

    /**
     * Class names probed with {@code Class.forName} (using the delegate's own
     * classloader) to decide availability. All must resolve.
     */
    String[] value();

    /**
     * Human-readable library name for error messages.
     */
    String name() default "";

    /**
     * Maven coordinate suggested to consumers when the delegate is absent.
     */
    String maven() default "";

}
