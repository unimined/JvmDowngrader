package xyz.wagyourtail.jvmdg.version.scan;

import xyz.wagyourtail.jvmdg.version.InitPost;
import xyz.wagyourtail.jvmdg.version.InitPosts;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * {@code @InitPosts} container → unboxes each {@link InitPost} — repeatable
 * sugar and the explicit container register identically.
 */
public class InitPostsHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return InitPosts.class;
    }

    @Override
    public void onMethod(Method m, Annotation a, ScanContext ctx) {
        for (InitPost p : ((InitPosts) a).value()) {
            ctx.ctorCollector().addPost(m, p);
        }
    }
}
