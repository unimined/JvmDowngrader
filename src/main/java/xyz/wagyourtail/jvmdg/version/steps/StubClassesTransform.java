package xyz.wagyourtail.jvmdg.version.steps;

import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.DowngradeContext;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;
import java.util.Set;

/**
 * {@link VersionProvider#stubClasses} as a stage transform — the class-level
 * stub pass (supertypes, annotations, signatures, {@code $Marker}/{@code Ov}
 * interface injection). Not extras-wrapped, per the legacy sequencing; the
 * resolver pair comes from the context the same way
 * {@link InsertAbstractsTransform} obtains it.
 */
public class StubClassesTransform extends StepTransform {

    public StubClassesTransform(VersionProvider provider) {
        super(provider);
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) throws IOException {
        DowngradeContext dc = DowngradeContext.get(ctx);
        Set<ClassNode> extra = ctx.extras.asSet();
        Set<String> warnings = dc.warnings(node.name);
        return provider.stubClasses(
            node, dc.enableRuntime,
            provider.memberResolver(extra, dc.getReadOnly, warnings),
            provider.superTypeResolver(extra, dc.getReadOnly, warnings),
            warnings
        );
    }

}
