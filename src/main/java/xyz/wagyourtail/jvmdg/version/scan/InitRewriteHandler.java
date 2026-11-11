package xyz.wagyourtail.jvmdg.version.scan;

import xyz.wagyourtail.jvmdg.version.InitRewrite;

import java.lang.annotation.Annotation;

/**
 * {@code @InitRewrite} (a single repeat, or one element unboxed from
 * {@link InitRewritesHandler}'s container) → a staged ctor rewrite. The
 * declared rewrite is parked on {@link ScanContext#ctorCollector()} during
 * the class scan; {@link #finishClass} links staged {@code @InitArg}/
 * {@code @InitPre}/{@code @InitPost} handlers, validates the bound shape,
 * and registers the result — it runs once per class for every staged form
 * (the scanner calls it unconditionally).
 */
public class InitRewriteHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return InitRewrite.class;
    }

    @Override
    public void onClass(Class<?> c, Annotation a, ScanContext ctx) {
        ctx.ctorCollector().add((InitRewrite) a);
    }

    @Override
    public void finishClass(ScanContext ctx) {
        ctx.ctorCollector().finish(ctx);
    }
}
