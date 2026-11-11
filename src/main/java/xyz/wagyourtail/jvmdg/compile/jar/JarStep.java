package xyz.wagyourtail.jvmdg.compile.jar;

import java.io.IOException;
import java.nio.file.Path;

/**
 * One step in a {@link FusedJarRun} — the jar-level analogue of a pipeline
 * transform. Steps decompose into an optional jar-level preparation plus a
 * per-entry transformation, so a whole composed operation (signature strip,
 * manifest rewrite, multi-release routing, class downgrade/shade) shares one
 * input walk and one sorted write instead of a jar round-trip each.
 * <p>
 * Abstract class rather than interface: jvmdg main targets Java 7, so the
 * no-op hooks can't be interface default methods.
 */
public abstract class JarStep {

    /**
     * Jar-level preparation over the run's input root — read manifests, build
     * route tables, run analysis. Called once before any {@link #visitEntry};
     * state captured here is what the visitor uses.
     */
    public void prepareFused(Path inputRoot, StepContext ctx) throws IOException {
    }

    /**
     * Transform one file entry. {@code entry} carries the bytes the previous
     * step emitted (input bytes for the first); return the replacement —
     * possibly renamed via {@link JarEntry#renamed} — or {@code null} to drop
     * it from the output. Side emissions (a class that downgrades into several
     * outputs) go through {@code ctx.sink}.
     */
    public abstract JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException;

    /**
     * Directory-entry hook — called once per directory in the walk; returning
     * {@code false} drops it from the output. Default {@code true} (keep).
     */
    public boolean visitDirectory(JarEntry entry, StepContext ctx) throws IOException {
        return true;
    }

    /**
     * Called once after the last entry (successful run only) — the point to
     * flush buffered state or emit whole-run artifacts (synthesized manifest,
     * transformed class sets, shaded api classes) through {@code ctx.sink}.
     */
    public void endFused(StepContext ctx) throws IOException {
    }

}
