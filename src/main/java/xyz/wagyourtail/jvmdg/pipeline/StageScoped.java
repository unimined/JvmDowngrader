package xyz.wagyourtail.jvmdg.pipeline;

/**
 * A {@link TransformContext#data} artifact whose contents must not cross a
 * stage boundary — the engine invokes {@link #stageBoundary()} before every
 * stage's class loop (ahead of {@code onEnter}). Method-derived artifacts
 * (frames, indices) go stale when earlier stages mutate the same
 * {@code MethodNode}s; clearing here makes stale hits impossible rather
 * than relying on consumer discipline.
 */
public interface StageScoped {

    /**
     * Invoked by the engine while it iterates {@code ctx.data.values()} —
     * implementations must clear their own contents only; mutating
     * {@code ctx.data} itself (e.g. removing this artifact) is a
     * concurrent-modification hazard.
     */
    void stageBoundary();

}
