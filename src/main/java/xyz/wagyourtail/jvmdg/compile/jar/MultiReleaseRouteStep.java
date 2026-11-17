package xyz.wagyourtail.jvmdg.compile.jar;

import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-release input routing for class entries — reproduces the legacy
 * {@code PathDowngrader} behavior exactly:
 * <ul>
 *   <li>{@code META-INF/versions/<n>/**} classes with
 *       {@code downgradeFromMultiReleases}: re-anchored to their stripped path
 *       at slot {@code n} and compete with the root entry of the same name —
 *       the first (stripped-name, slot) pair seen wins the dedup, then the
 *       prefer-highest scan below resolves which bytes actually feed it.</li>
 *   <li>Without that flag but with {@code multiReleaseOriginal}: passed
 *       through verbatim (still under {@code versions/<n>/}).</li>
 *   <li>Neither flag: dropped.</li>
 * </ul>
 * For the winner, {@code prefer-highest} replaces the entry's source with the
 * highest {@code versions/<n>/} file carrying the same name — today this scan
 * runs for whichever entry wins the race, so the result is order-stable.
 */
public class MultiReleaseRouteStep extends JarStep {

    /**
     * When true, {@code multiReleaseOriginal} versioned entries pass through
     * to downstream steps tagged at their slot (a variant-capable class step
     * collects them for in-place shading) instead of being consumed here as
     * verbatim copies.
     */
    private final boolean passthroughVariants;

    private Path inputRoot;
    private final List<String> multiReleasePaths = new ArrayList<>();
    private final Set<String> visitedClasses = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public MultiReleaseRouteStep() {
        this(false);
    }

    public MultiReleaseRouteStep(boolean passthroughVariants) {
        this.passthroughVariants = passthroughVariants;
    }

    @Override
    public void prepareFused(Path inputRoot, StepContext ctx) throws IOException {
        this.inputRoot = inputRoot;
        for (int v = ctx.downgrader.maxVersion(); v >= 52; v--) {
            multiReleasePaths.add("META-INF/versions/" + Utils.classVersionToMajorVersion(v));
        }
    }

    @Override
    public JarEntry visitEntry(JarEntry entry, StepContext ctx) throws IOException {
        if (!entry.name.endsWith(".class")) {
            return entry;
        }
        String rel = entry.name;
        int slot = 0;
        if (rel.startsWith("META-INF/versions/")) {
            int slotEnd = rel.indexOf('/', "META-INF/versions/".length());
            if (slotEnd > 0) {
                try {
                    slot = Integer.parseInt(rel.substring("META-INF/versions/".length(), slotEnd));
                } catch (NumberFormatException e) {
                    // non-numeric version dir — malformed jar; treat as root
                    // slot rather than crash (legacy routing ignored slot numbers)
                    slot = 0;
                }
            }
            if (ctx.flags.downgradeFromMultiReleases) {
                rel = rel.substring(slotEnd + 1);
            } else if (ctx.flags.multiReleaseOriginal) {
                if (passthroughVariants) {
                    // let a variant-capable class step collect it — the
                    // fused shade path rewrites its api refs in place
                    return entry.atSlot(slot);
                }
                // verbatim copy of the versioned entry — legacy emitted the
                // raw bytes untouched, so downstream class steps never see it
                if (entry.data != null) {
                    ctx.sink.put(entry.name, entry.data);
                } else {
                    ctx.sink.copy(entry.name, entry.source);
                }
                return null;
            } else {
                return null;
            }
        }
        if (!visitedClasses.add(rel)) {
            return null;
        }
        if (ctx.flags.downgradeFromMultiReleases) {
            for (String pth : multiReleasePaths) {
                Path best = inputRoot.resolve(pth).resolve(rel);
                if (Files.exists(best)) {
                    return entry.renamed(rel).withSource(best).atSlot(slot);
                }
            }
        }
        return entry.renamed(rel).atSlot(slot);
    }

}
