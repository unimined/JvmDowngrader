package xyz.wagyourtail.jvmdg.version.steps;

import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.DowngradeContext;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;

/**
 * Stage-entry per-class setup: provider init (idempotent — the legacy
 * call ran it per class), then the shared {@link VersionProvider#initClass}
 * prologue — provenance stamp, class version to this boundary's output,
 * and the java-api {@code module-info} requires injection — so this path
 * and {@link VersionProvider#downgrade} can't drift on either.
 */
public class ProviderInitTransform extends StepTransform {

    public ProviderInitTransform(VersionProvider provider) {
        super(provider);
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) throws IOException {
        provider.ensureInit(DowngradeContext.get(ctx).downgrader);
        return provider.initClass(node);
    }

}
