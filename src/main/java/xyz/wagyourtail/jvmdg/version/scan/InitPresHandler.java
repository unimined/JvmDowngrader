package xyz.wagyourtail.jvmdg.version.scan;

import xyz.wagyourtail.jvmdg.version.InitPre;
import xyz.wagyourtail.jvmdg.version.InitPres;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * {@code @InitPres} container → unboxes each {@link InitPre} — repeatable
 * sugar and the explicit container register identically.
 */
public class InitPresHandler implements AnnotationHandler {

    @Override
    public Class<? extends Annotation> annotation() {
        return InitPres.class;
    }

    @Override
    public void onMethod(Method m, Annotation a, ScanContext ctx) {
        for (InitPre p : ((InitPres) a).value()) {
            ctx.ctorCollector().addPre(m, p);
        }
    }
}
