package xyz.wagyourtail.jvmdg.version.steps;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleRequireNode;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.DowngradeContext;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Stage-entry per-class setup, lifted from {@link VersionProvider#downgrade}:
 * provider init (idempotent — the legacy call ran it per class), the class
 * version stamp to this boundary's output version, and the
 * {@code module-info} requires injection for the java-api module.
 */
public class ProviderInitTransform extends StepTransform {

    public ProviderInitTransform(VersionProvider provider) {
        super(provider);
    }

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) throws IOException {
        DowngradeContext dc = DowngradeContext.get(ctx);
        provider.ensureInit(dc.downgrader);
        node.version = provider.outputVersion;
        if (node.name.equals("module-info")) {
            if (node.module.requires == null) {
                node.module.requires = new ArrayList<ModuleRequireNode>();
            } else {
                boolean contains = true;
                for (ModuleRequireNode req : node.module.requires) {
                    if (req.module.equals("xyz.wagyourtail.jvmdg.java_api")) {
                        contains = false;
                        break;
                    }
                }
                // else
                if (contains) {
                    node.module.requires.add(new ModuleRequireNode("xyz.wagyourtail.jvmdg.java_api", 0, Flags.jvmdgVersion));
                }
            }
        }
        return node;
    }

}
