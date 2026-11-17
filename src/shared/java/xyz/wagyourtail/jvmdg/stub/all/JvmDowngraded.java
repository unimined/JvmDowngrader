package xyz.wagyourtail.jvmdg.stub.all;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Downgrade provenance stamped on every class that passes through the
 * version chain: {@code from} is the classfile major the class was authored
 * at (the version seen at the first provider boundary, before
 * {@code node.version} is rewritten), {@code to} is the downgrade target
 * ({@code flags.classVersion}). Reflection stubs read {@code to} off the
 * callsite's class to answer in the vocabulary its {@code ldc}s were left
 * in — e.g. {@code Class.getInterfaces} substitutes an api stand-in for a
 * natively-served interface exactly when the callsite's own
 * {@code X.class} literal was remapped to that stand-in
 * ({@code since ∈ (to, from]}). Untransformed classes carry no annotation
 * and get the runner's native view. Runtime-visible like
 * {@link GainedInterfaces}/{@link RemovedInterfaces}; shade-prefixing
 * rewrites the descriptor consistently on both sides.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JvmDowngraded {

    /** classfile major the class was authored at */
    int from();

    /** classfile major the downgrade wrote it out as */
    int to();

}
