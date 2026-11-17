package xyz.wagyourtail.jvmdg.pipeline;

import xyz.wagyourtail.jvmdg.asm.index.ClasspathIndex;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.logging.Logger;

import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Everything a {@link ClassTransform} can see besides the class itself.
 * One context per pipeline run; the engine swaps {@link #extras} and
 * {@link #current} as stages advance.
 */
public final class TransformContext {

    /** Classpath lookup — original + loader + working layers. */
    public final ClasspathIndex index;

    /**
     * Classes bound for the next stage position — primaries are enqueued by
     * the engine, classes emitted by transforms are enqueued via this field.
     */
    public TransformWorklist extras;

    public final Flags flags;
    public final Logger logger;

    /** The stage currently executing; set by the engine. */
    public Stage current;

    /**
     * Live view into the running pipeline's multi-release captures —
     * label → deep-copied class set at that stage boundary. Bound by the
     * engine at run start; barrier hooks may index or rewrite the lists
     * (e.g. shading each captured variant in place).
     */
    public Map<Object, List<ClassNode>> variants;

    /**
     * Emitted resources channel — jar-relative path → bytes. Barriers
     * (shading resource copies, generated data files) write here; the jar
     * writer drains it. TreeMap keeps the drain order deterministic.
     */
    public final Map<String, byte[]> resources = new TreeMap<String, byte[]>();

    /** Diagnostics channel — scoped, deduplicated, sorted on drain. */
    public final DiagnosticSink diagnostics = new DiagnosticSink();

    /**
     * Registrar-private state (e.g. a {@code DowngradeContext} carrying the
     * provider boundary chain). Keyed by string so different registrars on
     * one pipeline can't collide.
     */
    public final Map<String, Object> data = new HashMap<String, Object>();

    /** Emits a resource entry — first write wins, matching jar-write dedup. */
    public void emitResource(String path, byte[] data) {
        synchronized (resources) {
            if (!resources.containsKey(path)) {
                resources.put(path, data);
            }
        }
    }

    /** The resource entries emitted so far, in sorted order. */
    public Map<String, byte[]> resources() {
        return resources;
    }

    public TransformContext(ClasspathIndex index, Flags flags, Logger logger) {
        this.index = index;
        this.flags = flags;
        this.logger = logger;
    }

}
