package xyz.wagyourtail.jvmdg.version.scan;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * One primary annotation → one registration operation, dispatched by
 * {@link xyz.wagyourtail.jvmdg.version.StubScanner} (Mixin
 * {@code InjectionInfo} / {@code ClassTransform} handler shape). The scanner
 * owns skip filters and per-class warning/failure collection; a handler owns
 * its annotation's validation and the registry writes it performs.
 * <p>
 * A method may carry at most one primary annotation across all handlers —
 * the scanner reports more than one as a registration error before any
 * handler runs.
 */
public interface AnnotationHandler {

    /** the primary annotation this handler processes */
    Class<? extends Annotation> annotation();

    /** class-level form of the annotation (e.g. {@code @Adapter}, {@code @InitRewrites}) */
    default void onClass(Class<?> c, Annotation a, ScanContext ctx) throws Exception {
    }

    /** method-level form — validate and stage the registration into {@code ctx} */
    default void onMethod(Method m, Annotation a, ScanContext ctx) throws Exception {
    }

    /** cross-member link/validation after every method of the class ran (e.g. ctor-rewrite handlers resolving against class-level {@code @InitRewrite}s) */
    default void finishClass(ScanContext ctx) throws Exception {
    }
}
