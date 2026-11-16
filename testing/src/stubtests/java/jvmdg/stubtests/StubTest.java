package jvmdg.stubtests;

import xyz.wagyourtail.jvmdg.version.Ref;

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
     * Registered stub member this test covers, as a {@link Ref} triple —
     * {@code value} is the internal owner name, {@code member} the member
     * name, {@code desc} the descriptor — e.g.
     * {@code @Ref(value = "java/lang/Runtime", member = "version", desc = "()Ljava/lang/Runtime$Version;")}.
     * This is the same shape {@code @Stub}/{@code @Adapter} use and parses
     * through {@code FullyQualifiedMemberNameAndDesc.of(Ref)}. The default
     * (all-empty) {@code @Ref} means the test covers cross-cutting behavior
     * rather than one member.
     */
    Ref member() default @Ref;
}
