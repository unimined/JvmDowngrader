package xyz.wagyourtail.jvmdg.version.scan;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.ClassMapping;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Per-class scan state handed to {@link AnnotationHandler}s: the provider
 * being registered into, the stub-provider class under scan, and the
 * warning/failure accumulators the scanner reports once per class.
 */
public class ScanContext {

    public final VersionProvider vp;
    public final Class<?> clazz;
    public final Set<String> warnings;
    public final List<String> failures;
    private CtorRewriteCollector ctorCollector;

    public ScanContext(VersionProvider vp, Class<?> clazz, Set<String> warnings, List<String> failures) {
        this.vp = vp;
        this.clazz = clazz;
        this.warnings = warnings;
        this.failures = failures;
    }

    /** the registry write slice for a stubbed JDK type (creates the owner mapping lazily, like the scanner's old direct call) */
    public ClassMapping stubMapper(Type owner) throws IOException {
        return vp.getStubMapper(owner, warnings);
    }

    /**
     * two-phase state for {@code @InitRewrite}/{@code @InitArg}/{@code @InitPre}/
     * {@code @InitPost}: class-level rewrites and method-level handlers stage
     * here during the scan; {@code InitRewriteHandler.finishClass} links,
     * validates, and registers once every member ran.
     */
    public CtorRewriteCollector ctorCollector() {
        if (ctorCollector == null) {
            ctorCollector = new CtorRewriteCollector();
        }
        return ctorCollector;
    }
}
