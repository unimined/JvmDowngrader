package xyz.wagyourtail.jvmdg.version;

import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.pipeline.ClassTransform;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.util.Function;

import java.io.IOException;

/**
 * One provider boundary as a pipeline transform: delegates to
 * {@link VersionProvider#downgrade} verbatim — stubClasses/stubMethods/
 * insertAbstracts/otherTransforms, extras emission, warnings — so the
 * sequential (unfused) pipeline reproduces the legacy per-provider pass
 * exactly. This is the oracle step: fusion decomposes it later.
 */
public class ProviderStep implements ClassTransform {

    private final ClassDowngrader downgrader;
    private final VersionProvider provider;
    private final boolean enableRuntime;
    private final Function<String, ClassNode> getReadOnly;

    public ProviderStep(ClassDowngrader downgrader, VersionProvider provider, boolean enableRuntime, Function<String, ClassNode> getReadOnly) {
        this.downgrader = downgrader;
        this.provider = provider;
        this.enableRuntime = enableRuntime;
        this.getReadOnly = getReadOnly;
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) throws IOException {
        return provider.downgrade(downgrader, node, ctx.extras.asSet(), enableRuntime, getReadOnly);
    }

}
