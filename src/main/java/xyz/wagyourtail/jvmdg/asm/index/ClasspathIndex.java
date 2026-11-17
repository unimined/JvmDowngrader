package xyz.wagyourtail.jvmdg.asm.index;

import org.objectweb.asm.ClassReader;
import xyz.wagyourtail.jvmdg.util.Utils;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.asm.info.ClassInfo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Memoized view of the classes a downgrader can resolve through its own
 * classloader (JDK + api jar + compile classpath). Replaces the repeated
 * {@code getResourceAsStream + ClassReader} parsing that ran once per
 * hierarchy/member query — every stub resolution touched the same handful of
 * stub and JDK classes.
 *
 * <p>Scope: the index is downgrader-scoped, so entries are shared across
 * calls and input classes. This is only safe because the classloader is
 * fixed for the downgrader's lifetime and serves stable bytes. The
 * original-form bytes of *input* classes are deliberately NOT held here —
 * those differ per call (agent mode: different loaders can return different
 * bytes for the same name) and are cached per-call by the caller instead.
 *
 * <p>The {@link #working} layer indexes generated/transformed nodes by name
 * for the resolver's "check extras" lookups (replacing linear scans).
 *
 * <p>Thread-safe; hits are softly memoized, misses re-probe each call so a
 * transient resource failure can't permanently mark a live class absent.
 */
public class ClasspathIndex {

    private final ClassLoader classLoader;

    // SoftReference: in classloader mode the app classpath can be large;
    // let infos be dropped and re-parsed under heap pressure instead of
    // pinning the whole reachable classpath in the index
    private final ConcurrentMap<String, SoftReference<ClassInfo>> loaderInfos = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ClassNode> working = new ConcurrentHashMap<>();

    public ClasspathIndex(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * ClassInfo for a class resolvable through the downgrader's classloader,
     * or null. Hits are memoized (softly, so they can be re-parsed after GC
     * pressure); misses are deliberately NOT cached — a transient resource
     * miss (racing jar close, fs blip) must not poison the class for the
     * index's lifetime, and re-probing an absent class is a cheap map lookup.
     *
     * <p>Care is taken on the install path: a map slot holding a
     * GC-cleared {@link SoftReference} must be swapped out atomically —
     * {@code putIfAbsent} alone would keep returning the dead ref and
     * permanently report a live class as missing.
     */
    public ClassInfo loaderInfo(String internalName) throws IOException {
        SoftReference<ClassInfo> cached = loaderInfos.get(internalName);
        ClassInfo info = cached == null ? null : cached.get();
        if (info == null) {
            try (InputStream is = Utils.openResource(classLoader, internalName + ".class")) {
                if (is == null) return null;
                cached = new SoftReference<>(ClassInfo.of(new ClassReader(is)));
            }
            final SoftReference<ClassInfo> fresh = cached;
            info = loaderInfos.compute(internalName, (k, prev) ->
                prev != null && prev.get() != null ? prev : fresh).get();
        }
        return info;
    }

    /**
     * Registers a generated/transformed class node in the working layer.
     */
    public void registerWorking(ClassNode node) {
        working.put(node.name, node);
    }

    public ClassNode workingAt(String internalName) {
        return working.get(internalName);
    }

    public void clearWorking() {
        working.clear();
    }

}
