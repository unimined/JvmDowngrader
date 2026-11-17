package xyz.wagyourtail.jvmdg.version.scan;

import xyz.wagyourtail.jvmdg.version.InitArg;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * {@code @InitArg} converter method → staged onto the class's
 * {@link CtorRewriteCollector}; binding (which rewrite, which {@code to}
 * slot, which original args feed its params) resolves in
 * {@link InitRewriteHandler#finishClass} once every member has been seen.
 */
public class InitArgHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return InitArg.class;
    }

    @Override
    public void onMethod(Method m, Annotation a, ScanContext ctx) {
        ctx.ctorCollector().addArg(m, (InitArg) a);
    }
}
