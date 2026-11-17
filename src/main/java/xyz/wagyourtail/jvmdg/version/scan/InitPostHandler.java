package xyz.wagyourtail.jvmdg.version.scan;

import xyz.wagyourtail.jvmdg.version.InitPost;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * {@code @InitPost} tail method → staged onto the class's
 * {@link CtorRewriteCollector} for linking in
 * {@link InitRewriteHandler#finishClass}.
 */
public class InitPostHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return InitPost.class;
    }

    @Override
    public void onMethod(Method m, Annotation a, ScanContext ctx) {
        ctx.ctorCollector().addPost(m, (InitPost) a);
    }
}
