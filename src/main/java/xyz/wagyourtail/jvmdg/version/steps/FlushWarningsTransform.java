package xyz.wagyourtail.jvmdg.version.steps;

import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.DowngradeContext;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

/**
 * End-of-class warning flush — {@link VersionProvider#printWarnings} for the
 * primary class's accumulated set, matching the legacy per-{@code downgrade()}
 * print. Classes dropped mid-stage never reach this transform; the stage's
 * {@code onExit} sweep ({@link DowngradeContext#flushAll}) covers them.
 */
public class FlushWarningsTransform extends StepTransform {

    public FlushWarningsTransform(VersionProvider provider) {
        super(provider);
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) {
        DowngradeContext.get(ctx).flushWarnings(provider, node.name);
        return node;
    }

}
