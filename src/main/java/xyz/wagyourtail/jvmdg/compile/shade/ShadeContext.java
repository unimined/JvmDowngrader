package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.pipeline.TransformContext;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.util.Utils;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Registrar-private state for in-pipeline shading — installed into
 * {@link TransformContext#data} by {@link ShadeRegistrar#install}. Holds the
 * input {@link ReferenceIndex} populated by the scan stage, the api index
 * classes are matched against, and the per-position products of the closure
 * barrier: inline targets, prefix remappers, emitted api classes.
 * <p>
 * Positions are multi-release slots ({@code 0} = jar root, {@code n} =
 * {@code META-INF/versions/<n>/}), matching the jar-level flow. For the
 * downgrade chain a node's slot derives from its class-file version
 * (final output lands at root, variants at their captured major); a runner
 * that knows entry positions directly (e.g. copied un-downgraded entries)
 * can pin them via {@link #pinPosition}.
 */
public class ShadeContext {

    public static final String KEY = "jvmdg:shade";

    public final String prefix;
    public final ReferenceIndex apiIndex;
    public final Set<Type> apiClasses;
    public final List<Path> apiRoots;
    /** Post-transform input references — populated by the scan stage. */
    public final ReferenceIndex inputIndex;

    /** Explicit slot overrides, identity-keyed; consulted before the version heuristic. */
    private final IdentityHashMap<ClassNode, Integer> positions = new IdentityHashMap<ClassNode, Integer>();

    /** position → input owner type → api members to inline into it. */
    public final Map<Integer, Map<Type, Set<FullyQualifiedMemberNameAndDesc>>> inlineTargets =
        new HashMap<Integer, Map<Type, Set<FullyQualifiedMemberNameAndDesc>>>();

    /** position → prefix remap for that position's required api types. */
    public final Map<Integer, SimpleRemapper> remappers = new HashMap<Integer, SimpleRemapper>();

    /**
     * Prefixed api classes ready for output — jar-relative path → node.
     * TreeMap keeps drain order deterministic; first write wins like the
     * jar flow's {@code writes.containsKey} check.
     */
    public final Map<String, ClassNode> emitted = new TreeMap<String, ClassNode>();

    public ShadeContext(Flags flags, String prefix, ReferenceIndex apiIndex, Set<Type> apiClasses, List<Path> apiRoots, ReferenceIndex inputIndex) {
        this.prefix = prefix;
        this.apiIndex = apiIndex;
        this.apiClasses = apiClasses;
        this.apiRoots = apiRoots;
        this.inputIndex = inputIndex;
    }

    /** Fetches this run's state; null when no shade registrar is installed. */
    public static ShadeContext get(TransformContext ctx) {
        return (ShadeContext) ctx.data.get(KEY);
    }

    /** Pins {@code node} to an explicit jar slot instead of the version heuristic. */
    public void pinPosition(ClassNode node, int position) {
        positions.put(node, position);
    }

    /**
     * The jar slot for {@code node}: a pinned position when set, otherwise
     * {@code META-INF/versions/<major>/} for nodes above the target class
     * version (multi-release variants) and {@code 0} for everything at or
     * below it — the same routing the class writer applies.
     */
    public int positionOf(ClassNode node, Flags flags) {
        Integer pinned = positions.get(node);
        if (pinned != null) {
            return pinned;
        }
        return node.version > flags.classVersion
            ? Utils.classVersionToMajorVersion(node.version) : 0;
    }

    /** The jar-relative output path for an emitted (already prefixed) api class. */
    public String relPath(ClassNode node, Flags flags) {
        String base = node.name + ".class";
        return node.version > flags.classVersion
            ? "META-INF/versions/" + Utils.classVersionToMajorVersion(node.version) + "/" + base
            : base;
    }

    /** Records an emitted api class — first write at a path wins. */
    public void emit(ClassNode node, Flags flags) {
        String rel = relPath(node, flags);
        if (!emitted.containsKey(rel)) {
            emitted.put(rel, node);
        }
    }

    /** A sorted snapshot of the emitted classes, for deterministic drains. */
    public List<ClassNode> emittedClasses() {
        return new ArrayList<ClassNode>(emitted.values());
    }

    /** The usage/closure product for a position, exposed for tests and debug. */
    public Map<Integer, Pair<Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>, Set<String>>> closures =
        new HashMap<Integer, Pair<Map<FullyQualifiedMemberNameAndDesc, Set<FullyQualifiedMemberNameAndDesc>>, Set<String>>>();

}
