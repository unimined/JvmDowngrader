package xyz.wagyourtail.jvmdg.pipeline;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The pipeline's diagnostics channel: scoped warning messages, deduplicated
 * on write and sorted on drain so emitted output is deterministic regardless
 * of processing order.
 * <p>
 * A scope is a free-form attribution key — the downgrade registrar uses the
 * class name being transformed; other registrars can scope by stage, jar
 * entry, or not at all ({@link #warn(String)}).
 * <p>
 * {@link #scope} returns a live mutable set so bulk collectors (e.g. the
 * coverage {@code warnClass} path that appends directly to a {@code Set})
 * keep working without an API change.
 */
public final class DiagnosticSink {

    private final Map<String, Set<String>> scoped = new LinkedHashMap<String, Set<String>>();
    private final Set<String> unscoped = new LinkedHashSet<String>();

    /** Live mutable set for {@code key} — writes dedup automatically. */
    public Set<String> scope(String key) {
        Set<String> set = scoped.get(key);
        if (set == null) {
            set = new LinkedHashSet<String>();
            scoped.put(key, set);
        }
        return set;
    }

    public void warn(String key, String message) {
        scope(key).add(message);
    }

    public void warn(String message) {
        unscoped.add(message);
    }

    /**
     * Sorted copy of {@code key}'s messages, removing them from the sink.
     * An empty (not null) set when the scope collected nothing.
     */
    public Set<String> drain(String key) {
        Set<String> set = scoped.remove(key);
        Set<String> out = new TreeSet<String>();
        if (set != null) {
            out.addAll(set);
        }
        return out;
    }

    /**
     * Every remaining scope's messages — scope keys and their contents both
     * sorted — removing them from the sink.
     */
    public Map<String, Set<String>> drainAll() {
        Map<String, Set<String>> out = new TreeMap<String, Set<String>>();
        for (Map.Entry<String, Set<String>> e : scoped.entrySet()) {
            out.put(e.getKey(), new TreeSet<String>(e.getValue()));
        }
        scoped.clear();
        return out;
    }

    /** Sorted unscoped messages, removing them. */
    public Set<String> drainUnscoped() {
        Set<String> out = new TreeSet<String>(unscoped);
        unscoped.clear();
        return out;
    }

}
