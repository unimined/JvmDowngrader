package xyz.wagyourtail.jvmdg.version;

import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.pipeline.ClassPrecondition;
import xyz.wagyourtail.jvmdg.pipeline.ExtrasApplyTransform;
import xyz.wagyourtail.jvmdg.pipeline.Stage;
import xyz.wagyourtail.jvmdg.pipeline.StageHook;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.version.steps.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Emits the downgrade stage list onto a pipeline: one {@link Stage} per
 * provider boundary in the chain from {@code inputVersion} down to the
 * downgrader's target, in order.
 * <p>
 * Re-homed checks (per the engine model): the class-file-version assertion
 * becomes the stage {@link Stage#precondition}, the multi-release snapshot
 * points become {@link Stage#captureLabels}, and each provider's
 * {@link VersionProvider#downgrade} sequence decomposes into the
 * {@code steps} transforms — {@link ProviderInitTransform},
 * {@link StubClassesTransform}, then the extras-wrapped
 * {@link StubMethodsTransform}, {@link InsertAbstractsTransform}, and
 * {@link OtherTransformsTransform}, closed by {@link FlushWarningsTransform}.
 * Sequential execution reproduces the legacy chain exactly.
 * <p>
 * A provider that overrides {@link VersionProvider#downgrade} wholesale
 * gets a {@link ProviderStep} instead — its custom orchestration can't be
 * decomposed.
 */
public class DowngradeRegistrar {

    private final ClassDowngrader downgrader;
    private final DowngradeContext context;

    public DowngradeRegistrar(ClassDowngrader downgrader, boolean enableRuntime, Function<String, ClassNode> getReadOnly) {
        this.downgrader = downgrader;
        this.context = new DowngradeContext(downgrader, enableRuntime, getReadOnly);
    }

    /** Binds this registrar's run state into the pipeline context. */
    public DowngradeRegistrar install(TransformContext ctx) {
        context.install(ctx);
        return this;
    }

    /**
     * The stage list for a class at {@code inputVersion}, ordered
     * input → target. Throws when no provider chain covers the input.
     */
    public List<Stage> stages(int inputVersion) {
        List<Stage> stages = new ArrayList<Stage>();
        int version = inputVersion;
        for (final VersionProvider provider : downgrader.versionProviders(inputVersion)) {
            stages.add(stageFor(provider, version, inputVersion, false));
            version = provider.outputVersion;
        }
        return stages;
    }

    /**
     * The stage list for a mixed-version class set: the union of every
     * input version's provider chain, ordered by descending input version
     * so a class flows through its own chain in order. Each stage carries
     * a {@link Stage#filter} gating to the class-file version it consumes —
     * classes at other versions pass through untouched (unlike the
     * single-version path, which asserts via {@code precondition}).
     * <p>
     * {@code multiReleaseVersions} captures still fire at their stage
     * boundaries; {@code multiReleaseOriginal} input-state captures are the
     * caller's job — seed pre-run nodes into {@code TransformPipeline#variants()}
     * at their class-file versions.
     */
    public List<Stage> stages(Set<Integer> inputVersions) {
        List<VersionProvider> providers = new ArrayList<VersionProvider>();
        for (Integer v : inputVersions) {
            for (VersionProvider p : downgrader.versionProviders(v)) {
                if (!providers.contains(p)) {
                    providers.add(p);
                }
            }
        }
        Collections.sort(providers, new Comparator<VersionProvider>() {
            @Override
            public int compare(VersionProvider a, VersionProvider b) {
                return Integer.compare(b.inputVersion, a.inputVersion);
            }
        });
        List<Stage> stages = new ArrayList<Stage>();
        for (VersionProvider provider : providers) {
            // -1 inputVersion: the multiReleaseOriginal input-boundary
            // capture can't run here — a class's own input version is lost
            // once earlier stages move it down the chain (an arrived-at
            // version is indistinguishable from a fresh input). Callers
            // seed input-state variants into the pipeline instead.
            stages.add(stageFor(provider, provider.inputVersion, -1, true));
        }
        return stages;
    }

    private Stage stageFor(final VersionProvider provider, int version, int inputVersion, boolean filtered) {
        Stage stage = new Stage(provider.getClass().getSimpleName() + "(" + provider.inputVersion + "->" + provider.outputVersion + ")");
        // multi-release snapshot: capture the class set at this version,
        // before this boundary's transforms — variant labels land in
        // META-INF/versions/<major>/ downstream via the node's version
        if (downgrader.flags.multiReleaseVersions.contains(version) ||
            (version == inputVersion && downgrader.flags.multiReleaseOriginal)) {
            stage.capture(version);
        }
        if (filtered) {
            stage.filter(new Function<ClassNode, Boolean>() {
                @Override
                public Boolean apply(ClassNode node) {
                    return node.version == provider.inputVersion;
                }
            });
        } else {
            stage.precondition(new ClassPrecondition() {
                @Override
                public void check(ClassNode node) {
                    if (node.version != provider.inputVersion) {
                        throw new IllegalArgumentException("Class " + node.name + " is not version " + provider.inputVersion);
                    }
                }
            });
        }
        if (overridesDowngrade(provider)) {
            stage.transform(new ProviderStep(downgrader, provider, context.enableRuntime, context.getReadOnly));
        } else {
            stage.transform(new ProviderInitTransform(provider))
                 .transform(new StubClassesTransform(provider))
                 .transform(new ExtrasApplyTransform(new StubMethodsTransform(provider)))
                 .transform(new ExtrasApplyTransform(new InsertAbstractsTransform(provider)))
                 .transform(new ExtrasApplyTransform(new OtherTransformsTransform(provider)))
                 .transform(new FlushWarningsTransform(provider));
            // classes dropped mid-stage never reach the flush transform —
            // sweep their leftovers at stage exit
            stage.onExit(new StageHook() {
                @Override
                public void run(TransformContext ctx) {
                    context.flushAll(provider);
                }
            });
        }
        return stage;
    }

    /**
     * SPI-compat check — a provider that overrides
     * {@link VersionProvider#downgrade} keeps its custom orchestration via
     * {@link ProviderStep}; the decomposed steps apply only to providers
     * using the stock sub-step sequence.
     */
    private static boolean overridesDowngrade(VersionProvider provider) {
        try {
            Method m = provider.getClass().getMethod("downgrade", ClassDowngrader.class, ClassNode.class, Set.class, boolean.class, Function.class);
            return m.getDeclaringClass() != VersionProvider.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

}
