package xyz.wagyourtail.jvmdg.version.steps;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleRequireNode;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.stub.all.JvmDowngraded;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.DowngradeContext;
import xyz.wagyourtail.jvmdg.version.VersionProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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
        if (!hasProvenance(node)) {
            // first boundary — node.version is still the authored major
            // (masked: a preview class carries minor 0xFFFF, and `from` is
            // compared against plain majors by the reflection runtime);
            // record it before the rewrite below so reflection stubs can
            // answer in the callsite's emitted vocabulary at runtime
            if (node.visibleAnnotations == null) {
                node.visibleAnnotations = new ArrayList<>();
            }
            AnnotationNode provenance = new AnnotationNode(Type.getDescriptor(JvmDowngraded.class));
            provenance.values = Arrays.asList(
                "from", Utils.majorClassVersion(node.version),
                "to", dc.downgrader.flags.classVersion);
            node.visibleAnnotations.add(provenance);
        }
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

    /** already stamped (a later boundary, or an upstream transformer) — keep the first-seen {@code from} */
    private static boolean hasProvenance(ClassNode node) {
        if (node.visibleAnnotations == null) {
            return false;
        }
        String desc = Type.getDescriptor(JvmDowngraded.class);
        for (AnnotationNode an : node.visibleAnnotations) {
            if (desc.equals(an.desc)) {
                return true;
            }
        }
        return false;
    }

}
