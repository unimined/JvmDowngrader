package xyz.wagyourtail.jvmdg.version.map;

import org.objectweb.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.pipeline.StageScoped;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.version.map.StructuralRemapTransform.MethodAnalysis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The shared per-run method-analysis channel: {@link MethodAnalysis}
 * holders memoized by {@link MethodNode} identity in
 * {@link TransformContext#data} so every consumer of a method shares one
 * fused frames computation. {@link StageScoped} — the engine clears it at
 * each stage boundary, so frames always describe the method as the current
 * stage found it; consumers must still capture what they need before
 * mutating within their own window.
 */
public final class MethodAnalyses implements StageScoped {

    private static final String KEY = MethodAnalyses.class.getName();

    // keyed on MethodNode identity — MethodNode doesn't override equals;
    // concurrent because a stage's class loop may apply in parallel
    private final Map<MethodNode, MethodAnalysis> byMethod = new ConcurrentHashMap<MethodNode, MethodAnalysis>();

    private MethodAnalyses() {
    }

    /** The shared holder for {@code method} on this store. */
    public MethodAnalysis getOrCreate(String owner, MethodNode method) {
        return byMethod.computeIfAbsent(method, k -> new MethodAnalysis(owner, k));
    }

    /** The run's store, or {@code null} when nothing has been analyzed yet. */
    public static MethodAnalyses get(TransformContext ctx) {
        return (MethodAnalyses) ctx.data.get(KEY);
    }

    /**
     * The run's store, created on first use. Class application may run in
     * parallel and {@code ctx.data} is a plain map, so creation serializes
     * on it — publication to consumers goes through the caller's volatile
     * field afterwards.
     */
    public static MethodAnalyses store(TransformContext ctx) {
        synchronized (ctx.data) {
            MethodAnalyses store = get(ctx);
            if (store == null) {
                store = new MethodAnalyses();
                ctx.data.put(KEY, store);
            }
            return store;
        }
    }

    @Override
    public void stageBoundary() {
        byMethod.clear();
    }

}
