package jvmdg.featuretests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a probe method in a {@code jvmdg.featuretests.manual.*} corpus class.
 * Each corpus class's {@code main} calls {@link Support#run(Class)}, which
 * reflects over its declared methods and invokes every {@code @FeatureTest}
 * method in name order, passing the stable id {@code <SimpleName>.<method>}.
 *
 * <p>Contract: {@code static void m(String id)} (or zero-arg). Report results
 * through {@link Support#print}/{@link Support#exc} or plain stdout — frames
 * diff whole-class output between native and downgraded/shaded runs. Uncaught
 * throwables are caught by the runner and reported as an {@code EXC} line so a
 * failing probe doesn't crash the rest of the class.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FeatureTest {
}
