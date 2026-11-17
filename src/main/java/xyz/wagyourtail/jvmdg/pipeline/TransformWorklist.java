package xyz.wagyourtail.jvmdg.pipeline;

import org.objectweb.asm.tree.ClassNode;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The accumulating set of classes at the current stage position: every
 * primary output plus every class a transform {@link #enqueue(ClassNode)
 * enqueued} mid-run. Name-keyed and name-ordered — drain order must not
 * depend on insertion order, so output stays deterministic.
 * <p>
 * Transforms read it to fan a stage's own treatment over emitted classes
 * (the {@code stubWithExtras} semantic): {@link #snapshot()} iterates a
 * stable copy so classes enqueued during that pass are invisible until the
 * next transform's pass; {@link #replace(ClassNode, ClassNode)} swaps an
 * emitted class for its transformed form (or drops it on null).
 * <p>
 * The whole set drains into the next stage's input when the stage ends.
 */
public final class TransformWorklist {

    private final Map<String, ClassNode> nodes = new TreeMap<String, ClassNode>();
    /** Append-only enqueue log — lets extras passes drain only what was
     * enqueued *during* their window instead of re-walking the whole
     * accumulated set. Entries carry whether they were transform-emitted
     * extras or pipeline outputs so drains can skip outputs. */
    private final List<ClassNode> journal = new ArrayList<ClassNode>();
    private final List<Boolean> journalIsOutput = new ArrayList<Boolean>();
    /** Journal position the next extras drain resumes from — entries
     * enqueued during a drain land past it and defer to the next pass. */
    private int extrasCursor;

    /** Adds {@code node} to the set as a transform-emitted extra, replacing any same-named entry. */
    public void enqueue(ClassNode node) {
        nodes.put(node.name, node);
        journal.add(node);
        journalIsOutput.add(false);
    }

    /** Adds {@code node} to the set as a pipeline output — extras drains skip it. */
    public void enqueueOutput(ClassNode node) {
        nodes.put(node.name, node);
        journal.add(node);
        journalIsOutput.add(true);
    }

    /**
     * Monotonic enqueue count — pair with {@link #entryAt} to iterate what
     * was enqueued after a point. Includes entries later replaced or
     * re-enqueued; callers re-check {@link #get(String)} for the live form.
     */
    public int mark() {
        return journal.size();
    }

    /** The {@code i}'th enqueued node — possibly stale if it was since
     * replaced or re-enqueued; compare against {@link #get(String)}. */
    public ClassNode entryAt(int i) {
        return journal.get(i);
    }

    /** Whether the {@code i}'th journal entry was a pipeline output. */
    public boolean isOutputAt(int i) {
        return journalIsOutput.get(i);
    }

    /** Journal position the next extras drain starts from. */
    public int extrasCursor() {
        return extrasCursor;
    }

    /** Advances the extras drain cursor — call before draining so entries
     * enqueued by the drain defer to the next pass. */
    public void extrasCursor(int mark) {
        this.extrasCursor = mark;
    }

    /** The class currently in the set under {@code name}, or null. */
    public ClassNode get(String name) {
        return nodes.get(name);
    }

    public boolean contains(String name) {
        return nodes.containsKey(name);
    }

    /**
     * Stable copy in name order — the iterate-while-mutating idiom for
     * extras passes: classes enqueued during the pass don't appear in it.
     */
    public List<ClassNode> snapshot() {
        return new ArrayList<ClassNode>(nodes.values());
    }

    /**
     * Replaces {@code original} with {@code transformed} — removing the entry
     * when {@code transformed} is null. Renames are honored: a transform that
     * changes the class name re-keys the entry.
     */
    public void replace(ClassNode original, ClassNode transformed) {
        nodes.remove(original.name);
        if (transformed != null) {
            nodes.put(transformed.name, transformed);
        }
    }

    /** Removes the entry keyed by {@code node.name}; true if one existed. */
    public boolean remove(ClassNode node) {
        return nodes.remove(node.name) != null;
    }

    /** All classes in name order — the next stage's input. */
    public List<ClassNode> drain() {
        return snapshot();
    }

    public int size() {
        return nodes.size();
    }

    /**
     * A live {@code Set<ClassNode>} view for legacy transform APIs that take
     * an {@code extra} set — adds enqueue, removes/contains are name-keyed
     * (matching the original name-comparator TreeSet semantics), iteration
     * yields a name-ordered snapshot (callers that mutate mid-iteration copy
     * first, same as they did on the set).
     */
    public Set<ClassNode> asSet() {
        return new AbstractSet<ClassNode>() {
            @Override
            public Iterator<ClassNode> iterator() {
                return snapshot().iterator();
            }

            @Override
            public int size() {
                return TransformWorklist.this.size();
            }

            @Override
            public boolean contains(Object o) {
                return o instanceof ClassNode && TransformWorklist.this.contains(((ClassNode) o).name);
            }

            @Override
            public boolean add(ClassNode node) {
                boolean had = TransformWorklist.this.contains(node.name);
                TransformWorklist.this.enqueue(node);
                return !had;
            }

            @Override
            public boolean remove(Object o) {
                return o instanceof ClassNode && TransformWorklist.this.remove((ClassNode) o);
            }
        };
    }

}
