package xyz.wagyourtail.jvmdg.version.steps;

import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.DowngradeContext;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;

/**
 * {@link VersionProvider#stubClasses} as a stage transform — the class-level
 * stub pass (supertypes, annotations, signatures). Not extras-wrapped, per
 * the legacy sequencing.
 */
public class StubClassesTransform extends StepTransform {

    public StubClassesTransform(VersionProvider provider) {
        super(provider);
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) throws IOException {
        DowngradeContext dc = DowngradeContext.get(ctx);
        return provider.stubClasses(node, dc.enableRuntime, dc.warnings(node.name));
    }

}
