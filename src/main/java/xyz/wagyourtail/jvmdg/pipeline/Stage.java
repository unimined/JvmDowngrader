package xyz.wagyourtail.jvmdg.pipeline;

import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.util.Function;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An ordered slice of a pipeline: the transforms that run at one position,
 * plus optional lifecycle hooks and variant captures.
 * <p>
 * A stage's transforms run per class, in list order — each class flows
 * through all of them before the next class starts (sequential semantics;
 * emitted classes accumulate in the shared worklist).
 * <p>
 * {@link #onEnter}/{@link #onExit} run once per stage, around the class loop.
 * A transform-less stage whose hooks compute shared state (e.g. a shading
 * closure) is a <b>barrier</b>: it runs once when reached, before the next
 * stage's class loop.
 * <p>
 * {@link #captureLabels} request a deep-copied snapshot of every class at
 * stage entry, stored under that label in {@link TransformPipeline#variants()}
 * — the multi-release mechanism: a label becomes a {@code META-INF/versions}
 * slot downstream.
 */
public final class Stage {

    /** Diagnostics only — appears in transform-failure provenance. */
    public final String name;

    public final List<ClassTransform> transforms = new ArrayList<ClassTransform>();

    /** Optional per-class guard at stage entry; nullable. */
    public ClassPrecondition precondition;

    /**
     * Optional per-class gate; nullable. A class the gate rejects flows
     * through untransformed (it still reaches {@code onExit} and later
     * stages) — the mixed-input-version mechanism: each provider stage
     * filters to the class-file version it consumes.
     */
    public Function<ClassNode, Boolean> filter;

    /** Variant labels to snapshot under at stage entry; ordered, dedup'd. */
    public final Set<Object> captureLabels = new LinkedHashSet<Object>();

    /**
     * Once-per-stage hooks around the class loop; nullable. {@code onEnter}
     * runs with this stage's (empty) worklist already bound — a barrier hook
     * that emits classes into {@code ctx.extras} joins them to the
     * <i>remaining</i> stages.
     */
    public StageHook onEnter;
    public StageHook onExit;

    public Stage(String name) {
        this.name = name;
    }

    public Stage transform(ClassTransform t) {
        transforms.add(t);
        return this;
    }

    public Stage precondition(ClassPrecondition p) {
        this.precondition = p;
        return this;
    }

    public Stage capture(Object label) {
        this.captureLabels.add(label);
        return this;
    }

    public Stage filter(Function<ClassNode, Boolean> f) {
        this.filter = f;
        return this;
    }

    public Stage onEnter(StageHook h) {
        this.onEnter = h;
        return this;
    }

    public Stage onExit(StageHook h) {
        this.onExit = h;
        return this;
    }

}
