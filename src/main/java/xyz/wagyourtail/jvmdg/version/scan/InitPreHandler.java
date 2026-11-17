package xyz.wagyourtail.jvmdg.version.scan;

import xyz.wagyourtail.jvmdg.version.InitPre;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * {@code @InitPre} hook method → staged onto the class's
 * {@link CtorRewriteCollector} for linking in
 * {@link InitRewriteHandler#finishClass}.
 */
public class InitPreHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return InitPre.class;
    }

    @Override
    public void onMethod(Method m, Annotation a, ScanContext ctx) {
        ctx.ctorCollector().addPre(m, (InitPre) a);
    }
}
