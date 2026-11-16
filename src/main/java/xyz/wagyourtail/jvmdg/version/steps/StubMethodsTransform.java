package xyz.wagyourtail.jvmdg.version.steps;

import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.DowngradeContext;
import xyz.wagyourtail.jvmdg.version.VersionProvider;
import xyz.wagyourtail.jvmdg.version.map.MethodAnalyses;

import java.io.IOException;
import java.util.Set;

/**
 * {@link VersionProvider#stubMethods} as a stage transform — the member-insn
 * stub pass. Wrap in {@code ExtrasApplyTransform} for the legacy
 * {@code stubWithExtras} semantics.
 */
public class StubMethodsTransform extends StepTransform {

    public StubMethodsTransform(VersionProvider provider) {
        super(provider);
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) throws IOException {
        DowngradeContext dc = DowngradeContext.get(ctx);
        Set<ClassNode> extra = ctx.extras.asSet();
        Set<String> warnings = dc.warnings(node.name);
        // share the run's MethodAnalysis store with the provider — the store
        // is ctx-shared and MethodNode-identity-keyed, so assignment is
        // idempotent under concurrent applies and is deliberately NOT
        // cleared on exit (a finally-clear races a sibling thread's
        // stubMethod; a stale store still yields fresh per-node analyses)
        provider.methodAnalyses = MethodAnalyses.store(ctx);
        return provider.stubMethods(
            node, extra, dc.enableRuntime,
            provider.memberResolver(extra, dc.getReadOnly, warnings),
            provider.superTypeResolver(extra, dc.getReadOnly, warnings),
            dc.getReadOnly
        );
    }

}
