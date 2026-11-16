package xyz.wagyourtail.jvmdg.compile.jar;

import xyz.wagyourtail.jvmdg.compile.shade.IndexScanner;
import xyz.wagyourtail.jvmdg.util.AsyncUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Executes a composed list of {@link JarStep}s over one input root as a single
 * traversal: every step's {@link JarStep#prepareFused} sees the input once,
 * each entry flows through the visitors in declaration order, then
 * {@link JarStep#endFused} flushes whole-run state — one walk, one sorted
 * write. Port of unimined-remap's {@code FusedJarRun}; unlike that model the
 * sink is reachable through {@link StepContext} during the whole run because
 * jvmdg steps emit whole-run artifacts (transformed class sets, shaded api
 * classes, synthesized manifests), not just per-entry results.
 */
public final class FusedJarRun {

    private FusedJarRun() {
    }

    /**
     * Runs {@code steps} over {@code inputRoot}, writing to {@code outputRoot}
     * through the run's {@link JarEntrySink}. Directory entries pass through
     * the {@link JarStep#visitDirectory} chain; file entries through
     * {@link JarStep#visitEntry} — {@code null} drops, a surviving entry lands
     * as a byte write or a lazy copy depending on whether it still references
     * a source file.
     */
    public static void run(final Path inputRoot, final List<JarStep> steps, final StepContext ctx, Path outputRoot) throws IOException {
        for (JarStep step : steps) {
            step.prepareFused(inputRoot, ctx);
        }
        try {
            AsyncUtils.visitPathsAsync(inputRoot, path -> {
                JarEntry dir = new JarEntry(IndexScanner.internalPath(inputRoot.relativize(path)), path);
                for (JarStep step : steps) {
                    if (!step.visitDirectory(dir, ctx)) {
                        return true;
                    }
                }
                ctx.sink.mkdir(dir.name);
                return true;
            }, path -> {
                JarEntry cur = new JarEntry(IndexScanner.internalPath(inputRoot.relativize(path)), path);
                for (JarStep step : steps) {
                    if (cur == null) {
                        break;
                    }
                    cur = step.visitEntry(cur, ctx);
                }
                if (cur != null) {
                    if (cur.data != null) {
                        ctx.sink.put(cur.name, cur.data);
                    } else {
                        ctx.sink.copy(cur.name, cur.source);
                    }
                }
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (JarStep step : steps) {
            step.endFused(ctx);
        }
        ctx.sink.flush(outputRoot);
    }

}
