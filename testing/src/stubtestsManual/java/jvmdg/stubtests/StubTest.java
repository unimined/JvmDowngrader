package jvmdg.stubtests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a hand-written stub-parity test. The generator reflects over compiled
 * classes under {@code jvmdg.stubtests.manual}, assigns each annotated method
 * an {@code x####} id, and emits a call into the generated {@code Index}.
 *
 * <p>Contract: the method must be {@code public static void m(String id)} and
 * report results through {@link Support#print}/{@link Support#exc}. A single
 * method may emit several result lines by suffixing the id with {@code b}, {@code c}
 * (e.g. {@code Support.print(id + "b", r)}). Wrap the exercised calls in
 * {@code try { ... } catch (Throwable t) { Support.exc(id, t); } }.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StubTest {

    /**
     * Registered stub-member key this test covers, in {@code index.txt} format:
     * {@code <internal-owner>.<name><desc>}, e.g.
     * {@code java/lang/Runtime.version()Ljava/lang/Runtime$Version;}.
     * Empty when the test covers cross-cutting behavior rather than one member.
     */
    String member() default "";
}
