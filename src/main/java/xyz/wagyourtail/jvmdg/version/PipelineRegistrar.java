package xyz.wagyourtail.jvmdg.version;

import xyz.wagyourtail.jvmdg.pipeline.Stage;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;

import java.util.List;
import java.util.Set;

/**
 * A registrar that emits a stage list onto a transform pipeline — the
 * composition point every version-chain direction plugs into.
 * {@link DowngradeRegistrar} is the first implementation; an
 * {@code UpgradeRegistrar} (ascending boundaries: stubs redirect removed
 * calls to shims, {@code TypeRemap} redirects removed packages) implements
 * the same contract and can run on the same {@code TransformPipeline} —
 * {@code TransformContributor} anchors order the chains relative to each
 * other. Dual-run (downgrade + upgrade on one class set) is therefore a
 * matter of calling {@link #stages} on both registrars and concatenating
 * the lists.
 *
 * @since 3.1.0
 */
public interface PipelineRegistrar {

    /** binds this registrar's run state into the pipeline context */
    PipelineRegistrar install(TransformContext ctx);

    /** the stage list for a class at {@code inputVersion} */
    List<Stage> stages(int inputVersion);

    /**
     * the stage list for a mixed-version class set — each stage carries a
     * {@link Stage#filter} gating to the class-file version it consumes
     */
    List<Stage> stages(Set<Integer> inputVersions);
}
