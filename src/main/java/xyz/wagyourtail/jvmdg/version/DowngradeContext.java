package xyz.wagyourtail.jvmdg.version;

import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.pipeline.DiagnosticSink;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.util.Function;

import java.util.Map;
import java.util.Set;

/**
 * Registrar-private state carried in {@link TransformContext#data} — the
 * downgrade-specific surface the sub-transforms share: the downgrader (for
 * ensureInit and version-position queries), the runtime flag, and the
 * original-bytes view.
 * <p>
 * Warning collection delegates to {@link TransformContext#diagnostics},
 * scoped by the class being transformed (an emitted extra attributes its
 * own warnings). {@link #warnings(String)} is the write path; the stage's
 * flush step drains each class's set through
 * {@link VersionProvider#printWarnings}, and dropped classes' leftovers are
 * drained at stage exit by {@link #flushAll(VersionProvider)}.
 */
public class DowngradeContext {

    public static final String KEY = "jvmdg.downgrade";

    public final ClassDowngrader downgrader;
    public final boolean enableRuntime;
    public final Function<String, ClassNode> getReadOnly;

    private DiagnosticSink sink;

    public DowngradeContext(ClassDowngrader downgrader, boolean enableRuntime, Function<String, ClassNode> getReadOnly) {
        this.downgrader = downgrader;
        this.enableRuntime = enableRuntime;
        this.getReadOnly = getReadOnly;
    }

    public static DowngradeContext get(TransformContext ctx) {
        return (DowngradeContext) ctx.data.get(KEY);
    }

    /** Binds this context's warning channel to the run's sink. */
    public DowngradeContext install(TransformContext ctx) {
        this.sink = ctx.diagnostics;
        ctx.data.put(KEY, this);
        return this;
    }

    /** The warning set for {@code className} — created on first use. */
    public Set<String> warnings(String className) {
        return sink.scope(className);
    }

    /**
     * Prints and drops {@code className}'s accumulated warnings — the flush
     * at the end of a class's stage traversal.
     */
    public void flushWarnings(VersionProvider provider, String className) {
        provider.printWarnings(sink.drain(className), className);
    }

    /**
     * Prints and drains every remaining class's warnings — stage-exit sweep
     * for classes dropped mid-stage (whose flush step never ran). Legacy
     * printed at drop time; this preserves the content, at stage-end timing.
     */
    public void flushAll(VersionProvider provider) {
        for (Map.Entry<String, Set<String>> e : sink.drainAll().entrySet()) {
            provider.printWarnings(e.getValue(), e.getKey());
        }
    }

}
