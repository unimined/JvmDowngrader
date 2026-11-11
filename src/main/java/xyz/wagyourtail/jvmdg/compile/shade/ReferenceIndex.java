package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.logging.Logger;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * The reference index: extracted {@link ExtractedRefs} per
 * (class-type, multi-release slot) — entry-stream input ({@link #add} takes
 * a parsed {@link ClassNode} and its slot), floor version lookup, usage
 * inversion, and reachability closures.
 * <p>
 * Slots are multi-release directory numbers: {@code 0} for the jar root,
 * {@code 9}/{@code 11}/… for {@code META-INF/versions/<n>/} entries — the
 * caller decides the slot, the index knows only the ordering.
 * {@link #resolveVersioned} implements the floor rule: the highest slot
 * not above the requested one.
 * <p>
 * Replaces {@code ReferenceGraph} — the split is extractor
 * ({@link RefExtractor}) / index (this) / reachability
 * ({@link #closure}/{@link #closureWithUsages}). Thread-safe for concurrent
 * {@link #add}s of distinct (type, slot) pairs; queries run against a
 * fully-built index.
 */
public class ReferenceIndex {

    private final Logger logger;
    private final Set<Integer> versions = new ConcurrentSkipListSet<>();
    private final ConcurrentMap<Type, ConcurrentMap<Integer, ExtractedRefs>> refs = new ConcurrentHashMap<Type, ConcurrentMap<Integer, ExtractedRefs>>();
    private final ConcurrentMap<Type, ConcurrentMap<Integer, ClassNode>> classNodes = new ConcurrentHashMap<Type, ConcurrentMap<Integer, ClassNode>>();
    private final RefExtractor extractor;
    private final boolean retainClassNodes;
    private final boolean retainInsns;

    public ReferenceIndex(Logger logger) {
        this(logger, false);
    }

    public ReferenceIndex(Logger logger, boolean retainClassNodes) {
        this(logger, retainClassNodes, false);
    }

    public ReferenceIndex(Logger logger, boolean retainClassNodes, boolean retainInsns) {
        this(logger, new MemberRefExtractor(retainInsns), retainClassNodes, retainInsns);
    }

    public ReferenceIndex(Logger logger, RefExtractor extractor, boolean retainClassNodes, boolean retainInsns) {
        this.logger = logger.subLogger(ReferenceIndex.class);
        this.extractor = extractor;
        this.retainClassNodes = retainClassNodes;
        this.retainInsns = retainInsns;
        if (retainInsns && !retainClassNodes) {
            throw new IllegalArgumentException("Cannot retain instructions without retaining class nodes.");
        }
        versions.add(0);
    }

    /**
     * Indexes {@code node} at {@code version}: extracts its references with
     * the configured extractor, records the slot, and retains the node if
     * configured. A second add for the same (type, slot) keeps the first —
     * matching the old {@code putIfAbsent} dedup of duplicate class files.
     */
    public void add(ClassNode node, int version, RefExtractor.Filter filter) {
        Type type = Type.getObjectType(node.name);
        ConcurrentMap<Integer, ExtractedRefs> byVersion = refs.get(type);
        if (byVersion != null && byVersion.containsKey(version)) {
            return;
        }
        // extraction is the expensive part — run it outside the lock;
        // a losing concurrent add just discards its record
        ExtractedRefs extracted = extractor.extract(node, filter);
        synchronized (this) {
            byVersion = refs.get(type);
            if (byVersion == null) {
                byVersion = new ConcurrentHashMap<Integer, ExtractedRefs>();
                refs.put(type, byVersion);
            }
            if (byVersion.putIfAbsent(version, extracted) != null) {
                return;
            }
            versions.add(version);
            if (retainClassNodes) {
                ConcurrentMap<Integer, ClassNode> nodes = classNodes.get(type);
                if (nodes == null) {
                    nodes = new ConcurrentHashMap<Integer, ClassNode>();
                    classNodes.put(type, nodes);
                }
                nodes.put(version, node);
            }
        }
    }

    /** The retained node for {@code type} at floor({@code version}), or null. */
    public ClassNode nodeFor(Type type, int version) {
        if (!retainClassNodes) {
            throw new IllegalStateException("Class nodes are not retained.");
        }
        return resolveVersioned(classNodes.get(type), version);
    }

    /** Extracted refs for {@code type} at floor({@code version}), or null. */
    public ExtractedRefs refsFor(Type type, int version) {
        return resolveVersioned(refs.get(type), version);
    }

    /** All slots ever indexed (incl. {@code 0}). */
    public Set<Integer> versions() {
        return new TreeSet<>(versions);
    }

    /** Each indexed type → the slots it was seen at. */
    public Map<Type, Set<Integer>> keys() {
        Map<Type, Set<Integer>> keys = new HashMap<Type, Set<Integer>>();
        for (Map.Entry<Type, ConcurrentMap<Integer, ExtractedRefs>> e : refs.entrySet()) {
            keys.put(e.getKey(), new HashSet<Integer>(e.getValue().keySet()));
        }
        return keys;
    }

    /** Every retained reference at floor({@code version}). */
    public Set<FullyQualifiedMemberNameAndDesc> allRefs(int version) {
        Set<FullyQualifiedMemberNameAndDesc> out = new HashSet<FullyQualifiedMemberNameAndDesc>();
        for (Map.Entry<Type, ConcurrentMap<Integer, ExtractedRefs>> e : refs.entrySet()) {
            ExtractedRefs value = resolveVersioned(e.getValue(), version);
            if (value == null) continue;
            for (Type requiredInstance : value.requiredInstances) {
                out.add(FullyQualifiedMemberNameAndDesc.of(requiredInstance));
            }
            for (Set<FullyQualifiedMemberNameAndDesc> s : value.requiredForMembers.values()) {
                out.addAll(s);
            }
        }
        return out;
    }

    /**
     * Every retained reference at floor({@code version}), mapped to the
     * members that need it — the inverted form used to seed api closure.
     */
    public Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> usages(int version) {
        Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> out = new HashMap<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>();
        for (Map.Entry<Type, ConcurrentMap<Integer, ExtractedRefs>> e : refs.entrySet()) {
            Type owner = e.getKey();
            ExtractedRefs value = resolveVersioned(e.getValue(), version);
            if (value == null) continue;
            for (Type requiredInstance : value.requiredInstances) {
                FullyQualifiedMemberNameAndDesc ref = FullyQualifiedMemberNameAndDesc.of(requiredInstance);
                Set<FullyQualifiedMemberNameAndDesc> s = out.get(ref);
                if (s == null) {
                    s = new HashSet<FullyQualifiedMemberNameAndDesc>();
                    out.put(ref, s);
                }
                s.add(FullyQualifiedMemberNameAndDesc.of(owner));
            }
            for (Map.Entry<MemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> e2 : value.requiredForMembers.entrySet()) {
                FullyQualifiedMemberNameAndDesc target = e2.getKey().toFullyQualified(owner);
                for (FullyQualifiedMemberNameAndDesc ref : e2.getValue()) {
                    Set<FullyQualifiedMemberNameAndDesc> s = out.get(ref);
                    if (s == null) {
                        s = new HashSet<FullyQualifiedMemberNameAndDesc>();
                        out.put(ref, s);
                    }
                    s.add(target);
                }
            }
        }
        return out;
    }

    /**
     * BFS closure from {@code starts}: the refs reachable through the index
     * plus the {@code @RequiresResource} resources they need. Class refs
     * expand to the class's instance members and class-level requirements;
     * member refs expand to that member's requirements.
     */
    public Pair<Set<FullyQualifiedMemberNameAndDesc>, Set<String>> closure(Set<FullyQualifiedMemberNameAndDesc> starts, int version) {
        Set<FullyQualifiedMemberNameAndDesc> out = new HashSet<FullyQualifiedMemberNameAndDesc>(starts);
        Set<String> resources = new HashSet<String>();
        Queue<FullyQualifiedMemberNameAndDesc> toAdd = new ArrayDeque<FullyQualifiedMemberNameAndDesc>(starts);
        while (!toAdd.isEmpty()) {
            FullyQualifiedMemberNameAndDesc next = toAdd.poll();
            ExtractedRefs refer = refsFor(next.getOwner(), version);
            if (refer == null) continue;
            if (next.isClassRef()) {
                for (MemberNameAndDesc instanceMember : refer.instanceMembers) {
                    FullyQualifiedMemberNameAndDesc f = instanceMember.toFullyQualified(next.getOwner());
                    if (out.add(f)) {
                        toAdd.add(f);
                    }
                }
                for (Type t : refer.requiredInstances) {
                    FullyQualifiedMemberNameAndDesc f = FullyQualifiedMemberNameAndDesc.of(t);
                    if (out.add(f)) {
                        toAdd.add(f);
                    }
                }
            } else {
                Set<FullyQualifiedMemberNameAndDesc> nextRefs = refer.requiredForMembers.get(next.toMemberNameAndDesc());
                if (nextRefs != null) {
                    for (FullyQualifiedMemberNameAndDesc f : nextRefs) {
                        if (out.add(f)) {
                            toAdd.add(f);
                        }
                    }
                }
            }
        }
        collectResources(out, resources, version);
        return new Pair<Set<FullyQualifiedMemberNameAndDesc>, Set<String>>(out, resources);
    }

    /** Same closure as {@link #closure}, tracking which member pulled each ref. */
    public Pair<Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>, Set<String>> closureWithUsages(Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> starts, int version) {
        Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>> usages = new HashMap<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>(starts);
        Set<String> resources = new HashSet<String>();
        Queue<FullyQualifiedMemberNameAndDesc> toScan = new ArrayDeque<FullyQualifiedMemberNameAndDesc>(starts.keySet());
        while (!toScan.isEmpty()) {
            FullyQualifiedMemberNameAndDesc next = toScan.poll();
            ExtractedRefs refer = refsFor(next.getOwner(), version);
            if (refer == null) continue;
            if (next.isClassRef()) {
                for (MemberNameAndDesc instanceMember : refer.instanceMembers) {
                    FullyQualifiedMemberNameAndDesc f = instanceMember.toFullyQualified(next.getOwner());
                    if (!usages.containsKey(f)) {
                        usages.put(f, new HashSet<FullyQualifiedMemberNameAndDesc>());
                        toScan.add(f);
                    }
                    usages.get(f).add(next);
                }
                for (Type t : refer.requiredInstances) {
                    FullyQualifiedMemberNameAndDesc f = FullyQualifiedMemberNameAndDesc.of(t);
                    if (!usages.containsKey(f)) {
                        usages.put(f, new HashSet<FullyQualifiedMemberNameAndDesc>());
                        toScan.add(f);
                    }
                    usages.get(f).add(next);
                }
            } else {
                Set<FullyQualifiedMemberNameAndDesc> nextRefs = refer.requiredForMembers.get(next.toMemberNameAndDesc());
                if (nextRefs != null) {
                    for (FullyQualifiedMemberNameAndDesc f : nextRefs) {
                        if (!usages.containsKey(f)) {
                            usages.put(f, new HashSet<FullyQualifiedMemberNameAndDesc>());
                            toScan.add(f);
                        }
                        usages.get(f).add(next);
                    }
                }
            }
        }
        collectResources(usages.keySet(), resources, version);
        return new Pair<Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>, Set<String>>(usages, resources);
    }

    private void collectResources(Set<FullyQualifiedMemberNameAndDesc> refs, Set<String> resources, int version) {
        for (FullyQualifiedMemberNameAndDesc ref : refs) {
            ExtractedRefs refer = refsFor(ref.getOwner(), version);
            if (refer == null) continue;
            MemberNameAndDesc member = ref.toMemberNameAndDesc();
            if (refer.resourceList.containsKey(member)) {
                resources.addAll(Arrays.asList(refer.resourceList.get(member)));
            }
        }
    }

    /**
     * Retained insns referencing {@code ref} at the EXACT slot (no floor —
     * inlined call sites are version-specific). Optionally scoped to one
     * owning class for per-class application.
     */
    public Set<AbstractInsnNode> insnsFor(FullyQualifiedMemberNameAndDesc ref, int version, Type onlyOwner) {
        if (!retainInsns) {
            throw new IllegalStateException("Insns are not retained.");
        }
        Set<AbstractInsnNode> insns = new HashSet<AbstractInsnNode>();
        for (Map.Entry<Type, ConcurrentMap<Integer, ExtractedRefs>> e : refs.entrySet()) {
            if (onlyOwner != null && !e.getKey().equals(onlyOwner)) continue;
            ExtractedRefs v = e.getValue().get(version);
            // unlike all the other methods, we only want the insns for the given version.
            if (v == null) continue;
            Set<AbstractInsnNode> ins = v.memberInsns.get(ref);
            if (ins != null) {
                insns.addAll(ins);
            }
        }
        return insns;
    }

    /** Floor lookup: the highest slot ≤ {@code version} present in {@code map}. */
    public static <T> T resolveVersioned(Map<Integer, T> map, int version) {
        if (map == null) return null;
        for (int i = version; i >= 0; i--) {
            T t = map.get(i);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    /** Retained insn nodes are only kept when the index was built to keep them. */
    public boolean retainsInsns() {
        return retainInsns;
    }

}
