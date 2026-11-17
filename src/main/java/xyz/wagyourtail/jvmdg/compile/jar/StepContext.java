package xyz.wagyourtail.jvmdg.compile.jar;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Function;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared state for one {@link FusedJarRun} — the jar-level analogue of
 * {@code TransformContext}. Steps reach the sink via {@link #sink} for side
 * emissions, and {@link #data} is the registrar/step-private channel (same
 * convention as {@code TransformContext.data}).
 * <p>
 * {@link #downgrader} is null for runs that never downgrade classes (e.g.
 * pure shading of an already-downgraded jar); {@link #extraRead} resolves
 * class bytes by internal name across the input roots and the run classpath —
 * steps pass it to transforms that query surrounding classes.
 */
public final class StepContext {

    public final Flags flags;
    public final Logger logger;
    public final ClassDowngrader downgrader;
    /** Every input root feeding this run — the resolver looks across all of them. */
    public final List<Path> inputRoots;
    public final Function<String, byte[]> extraRead;
    public final JarEntrySink sink = new JarEntrySink();
    public final Map<String, Object> data = new ConcurrentHashMap<>();

    public StepContext(Flags flags, Logger logger, ClassDowngrader downgrader,
                       List<Path> inputRoots, Function<String, byte[]> extraRead) {
        this.flags = flags;
        this.logger = logger;
        this.downgrader = downgrader;
        this.inputRoots = inputRoots;
        this.extraRead = extraRead;
    }

}
