package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Type;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.pipeline.Stage;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Emits the shading stage list onto a pipeline, to be appended after the
 * version chain: {@code shade:scan} anchors the post-transform reference
 * index, {@code shade:closure} is the global barrier computing per-slot
 * closure + inline targets + prefix remaps + emitted api classes + resource
 * copies (and shading captured variants in place), then {@code shade:inline},
 * {@code shade:prefix}, and {@code shade:module-info} apply to the primary
 * class flow.
 * <p>
 * Equivalent in stages to the jar-level {@code ApiShader.shadeApis} body —
 * same closure math, same inline/prefix recipe — but operating on the
 * in-memory pipeline classes instead of re-scanning a written jar.
 * <p>
 * The api side ({@link ReferenceIndex}, class set, roots) is built by the
 * caller — {@code ApiShader.scanApis} for jar flows today, direct entry
 * feeds for a fused jar run later. Products land on {@link ShadeContext} in
 * {@link TransformContext#data}: {@code emitted} api classes and
 * {@code ctx.resources} for the writer to drain.
 */
public class ShadeRegistrar {

    private final ShadeContext context;

    /**
     * @param inputIndex the index post-transform classes are scanned into;
     *                   callers control retention — {@code retainClassNodes}
     *                   and {@code retainInsns} are required (the barrier
     *                   resolves input nodes and inline insns through it)
     */
    public ShadeRegistrar(Flags flags, Logger logger, String prefix, ReferenceIndex apiIndex, Set<Type> apiClasses, List<Path> apiRoots, ReferenceIndex inputIndex) {
        if (!prefix.endsWith("/")) {
            throw new IllegalArgumentException("prefix must end with /");
        }
        this.context = new ShadeContext(flags, prefix, apiIndex, apiClasses, apiRoots, inputIndex);
    }

    /** The products of this run — also reachable via {@code ctx.data}. */
    public ShadeContext context() {
        return context;
    }

    /** Binds this registrar's run state into the pipeline context. */
    public ShadeRegistrar install(TransformContext ctx) {
        ctx.data.put(ShadeContext.KEY, context);
        return this;
    }

    /**
     * The stage list to append after the version chain. Stage boundaries
     * matter: the scan must fully drain before the closure barrier, and the
     * barrier before inline/prefix — these cannot fuse into earlier stages.
     */
    public List<Stage> stages() {
        List<Stage> stages = new ArrayList<Stage>();
        stages.add(new Stage("shade:scan").transform(new ReferenceScanTransform(context)));
        stages.add(new Stage("shade:closure").onEnter(new ShadeClosureBarrier(context)));
        stages.add(new Stage("shade:inline").transform(new ShadeInlineTransform(context)));
        stages.add(new Stage("shade:prefix").transform(new ShadePrefixTransform(context)));
        stages.add(new Stage("shade:module-info").transform(new ShadeModuleInfoTransform()));
        return stages;
    }

}
