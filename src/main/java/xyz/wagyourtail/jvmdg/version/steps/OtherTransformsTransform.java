package xyz.wagyourtail.jvmdg.version.steps;

import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.DowngradeContext;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;
import java.util.Set;

/**
 * {@link VersionProvider#otherTransforms} (the full 7-arg overload) as a
 * stage transform — provider-specific fixups (nest fixes, indy desugars,
 * record/switch helpers). Wrap in {@code ExtrasApplyTransform} for the
 * legacy {@code stubWithExtras} semantics.
 */
public class OtherTransformsTransform extends StepTransform {

    public OtherTransformsTransform(VersionProvider provider) {
        super(provider);
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) throws IOException {
        DowngradeContext dc = DowngradeContext.get(ctx);
        Set<ClassNode> extra = ctx.extras.asSet();
        Set<String> warnings = dc.warnings(node.name);
        return provider.otherTransforms(
            node, extra, dc.getReadOnly, warnings, dc.enableRuntime,
            provider.memberResolver(extra, dc.getReadOnly, warnings),
            provider.superTypeResolver(extra, dc.getReadOnly, warnings)
        );
    }

}
