package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleRequireNode;
import xyz.wagyourtail.jvmdg.pipeline.ClassTransform;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;

import java.util.ArrayList;

/**
 * Strips {@code requires xyz.wagyourtail.jvmdg.java_api} from module-info —
 * the shaded api classes live inside the output jar, so the module dependency
 * is gone (the jar flow's module-info rewrite). Applies to any slot the
 * module-info flows through; the jar flow only rewrites the root copy.
 */
public class ShadeModuleInfoTransform implements ClassTransform {

    public static final String API_MODULE = "xyz.wagyourtail.jvmdg.java_api";

    @Override
    public ClassNode apply(ClassNode node, TransformContext ctx) {
        if (!"module-info".equals(node.name) || node.module == null || node.module.requires == null) {
            return node;
        }
        for (ModuleRequireNode require : new ArrayList<ModuleRequireNode>(node.module.requires)) {
            if (API_MODULE.equals(require.module)) {
                node.module.requires.remove(require);
            }
        }
        return node;
    }

    @Override
    public String toString() {
        return "shade-module-info";
    }

}
