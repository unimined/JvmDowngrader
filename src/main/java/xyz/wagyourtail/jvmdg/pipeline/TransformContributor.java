package xyz.wagyourtail.jvmdg.pipeline;

import xyz.wagyourtail.jvmdg.ClassDowngrader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * ServiceLoader SPI ({@code META-INF/services/...TransformContributor})
 * letting jar-side extensions contribute {@link Stage}s at fixed points of
 * the assembled pipeline instead of hardcoding transform order inside the
 * registrars. Implementations are discovered fresh per stage-list build,
 * sorted by class name for deterministic order, and invoked once per
 * {@link Anchor} they could meaningfully attach to: {@code BEFORE} and
 * {@code AFTER} are asked while {@code DowngradeRegistrar.stages} is
 * constructing the version chain, {@code POST_EMIT} after the last stage
 * of the run (currently appended by {@code PipelineJarStep} once shading
 * has added its stages — a pure downgrade path never reaches it).
 * <p>
 * {@code out} is the mutable stage list at that anchor — add {@link Stage}
 * instances to run work there, or leave it untouched. Contributors must be
 * side-effect free w.r.t. provider registries (member-stub registration is
 * {@code ReflectionContributor}'s job, which runs at provider init).
 */
public interface TransformContributor {

    enum Anchor {
        /** Before the first version-boundary stage. */
        BEFORE_VERSION_CHAIN,
        /** After the last version-boundary stage, before shade stages. */
        AFTER_VERSION_CHAIN,
        /** After every stage of the assembled run (post-shade). */
        POST_EMIT
    }

    /**
     * @param anchor     the position this call appends at — ignore anchors
     *                   the contributor doesn't target
     * @param downgrader flags/logger/classpath access for the run; null on
     *                   pure-shade runs (POST_EMIT with no downgrade stages)
     * @param out        the stage list under construction; append Stages
     */
    void contribute(Anchor anchor, ClassDowngrader downgrader, List<Stage> out);

    /**
     * Discover contributors via {@link ServiceLoader} (TCCL, falling back
     * to this interface's loader), sorted by class name for deterministic
     * order regardless of classpath order, and invoke each at
     * {@code anchor}. Called once per stage-list build.
     */
    static void invoke(Anchor anchor, ClassDowngrader downgrader, List<Stage> out) {
        List<TransformContributor> contributors = new ArrayList<TransformContributor>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = TransformContributor.class.getClassLoader();
        }
        for (TransformContributor c : ServiceLoader.load(TransformContributor.class, loader)) {
            contributors.add(c);
        }
        contributors.sort(Comparator.comparing(c -> c.getClass().getName()));
        for (TransformContributor c : contributors) {
            c.contribute(anchor, downgrader, out);
        }
    }
}
