package xyz.wagyourtail.jvmdg.version.scan;

import xyz.wagyourtail.jvmdg.version.InitRewrite;
import xyz.wagyourtail.jvmdg.version.InitRewrites;

import java.lang.annotation.Annotation;

/**
 * {@code @InitRewrites} container → unboxes each {@link InitRewrite} into the
 * same collector the lone-annotation form uses — javac's repeatable sugar and
 * the explicit pre-Java-8 container register identically. Linking lives in
 * {@link InitRewriteHandler#finishClass}, which runs regardless of which
 * annotation forms were present.
 */
public class InitRewritesHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return InitRewrites.class;
    }

    @Override
    public void onClass(Class<?> c, Annotation a, ScanContext ctx) {
        for (InitRewrite rw : ((InitRewrites) a).value()) {
            ctx.ctorCollector().add(rw);
        }
    }
}
