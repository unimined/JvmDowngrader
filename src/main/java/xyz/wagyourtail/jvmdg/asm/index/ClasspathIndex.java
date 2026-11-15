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
 * <p>Thread-safe; misses are cached so repeated queries for absent classes
 * don't re-read resources.
 */
public class ClasspathIndex {

    /** negative-cache sentinel: ConcurrentHashMap cannot store null */
    private static final SoftReference<ClassInfo> MISS = new SoftReference<>(null);

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
     * or null. Results (including misses) are memoized for the lifetime of
     * this index; hits are soft so they can be re-parsed after GC pressure.
     */
    public ClassInfo loaderInfo(String internalName) throws IOException {
        SoftReference<ClassInfo> cached = loaderInfos.get(internalName);
        ClassInfo info = cached == null ? null : cached.get();
        if (info == null && cached != MISS) {
            try (InputStream is = Utils.openResource(classLoader, internalName + ".class")) {
                cached = is == null ? MISS : new SoftReference<>(ClassInfo.of(new ClassReader(is)));
            }
            SoftReference<ClassInfo> prev = loaderInfos.putIfAbsent(internalName, cached);
            if (prev != null) {
                cached = prev;
            }
            info = cached.get();
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
