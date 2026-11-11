package xyz.wagyourtail.jvmdg.version.scan;

import xyz.wagyourtail.jvmdg.version.InitArg;
import xyz.wagyourtail.jvmdg.version.InitArgs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * {@code @InitArgs} container → unboxes each {@link InitArg} into the same
 * collector path as the lone-annotation form — repeatable sugar and the
 * explicit pre-Java-8 container register identically.
 */
public class InitArgsHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return InitArgs.class;
    }

    @Override
    public void onMethod(Method m, Annotation a, ScanContext ctx) {
        for (InitArg arg : ((InitArgs) a).value()) {
            ctx.ctorCollector().addArg(m, arg);
        }
    }
}
