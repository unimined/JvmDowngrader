package xyz.wagyourtail.jvmdg.pipeline;

import java.io.IOException;

/**
 * A transform failure with provenance: which stage, which transform, which
 * class. Extends {@link IOException} so the checked contract of
 * {@link TransformPipeline#run} is preserved for callers — an
 * {@code IOException} or {@code RuntimeException} inside a transform
 * surfaces as this type with the original as {@linkplain #getCause() cause}.
 * <p>
 * Stage {@link Stage#precondition} failures are deliberately not wrapped —
 * a rejected class (e.g. wrong file version) is a meaningful error of its
 * own, not a transform malfunction.
 */
public class TransformException extends IOException {

    /** Stage the failure occurred in. */
    public final String stageName;

    /** The transform that failed. */
    public final ClassTransform transform;

    /** Internal name of the class being transformed. */
    public final String className;

    public TransformException(String stageName, ClassTransform transform, String className, Throwable cause) {
        super("transform " + transform.getClass().getSimpleName() + " failed in stage " + stageName + " on class " + className + ": " + cause, cause);
        this.stageName = stageName;
        this.transform = transform;
        this.className = className;
    }

}
