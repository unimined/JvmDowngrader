package xyz.wagyourtail.jvmdg.version.steps;

import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.DowngradeContext;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;
import java.util.Set;

/**
 * {@link VersionProvider#insertAbstractMethods} as a stage transform —
 * fills abstract stubs the class doesn't implement. Wrap in
 * {@code ExtrasApplyTransform} for the legacy {@code stubWithExtras}
 * semantics.
 */
public class InsertAbstractsTransform extends StepTransform {

    public InsertAbstractsTransform(VersionProvider provider) {
        super(provider);
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) throws IOException {
        DowngradeContext dc = DowngradeContext.get(ctx);
        Set<ClassNode> extra = ctx.extras.asSet();
        Set<String> warnings = dc.warnings(node.name);
        return provider.insertAbstractMethods(
            node, extra, dc.enableRuntime, warnings,
            provider.memberResolver(extra, dc.getReadOnly, warnings),
            provider.superTypeResolver(extra, dc.getReadOnly, warnings)
        );
    }

}
