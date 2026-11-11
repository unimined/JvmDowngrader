package xyz.wagyourtail.jvmdg.compile.shade;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.asm.ASMUtils;
import xyz.wagyourtail.jvmdg.util.AsyncUtils;
import xyz.wagyourtail.jvmdg.util.IOConsumer;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * Entry-stream feeder for {@link ReferenceIndex}: walks a filesystem root
 * (directory or zip filesystem), parses each {@code .class}, and adds it at
 * its multi-release slot ({@code META-INF/versions/<n>/} → {@code n},
 * everything else → {@code 0}). {@code module-info.class} is skipped — it
 * carries no references the index tracks.
 * <p>
 * Two passes, mirroring the old {@code ReferenceGraph} flow: a path walk
 * derives (type, slot) targets, then a parallel pass parses + adds — the
 * add dedups duplicate (type, slot) pairs itself.
 */
public final class IndexScanner {

    private IndexScanner() {
    }

    /** Checks whether a root-relative path is inside {@code META-INF/versions/<n>/}. */
    public static boolean isMultiRelease(Path rel) {
        return rel.getNameCount() > 3
            && rel.getName(0).toString().equals("META-INF")
            && rel.getName(1).toString().equals("versions");
    }

    /** The multi-release slot for a root-relative path: {@code 0} outside the versions tree. */
    public static int slotOf(Path rel) {
        return isMultiRelease(rel) ? Integer.parseInt(rel.getName(2).toString()) : 0;
    }

    /** Joins path components with forward slashes, for a class-file resource path. */
    public static String internalPath(Path rel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rel.getNameCount(); i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(rel.getName(i));
        }
        return sb.toString();
    }

    /**
     * Scans {@code root} into {@code index} — every class file at its
     * multi-release slot.
     */
    public static void scanRoot(final ReferenceIndex index, final Path root, final RefExtractor.Filter filter) throws IOException, ExecutionException, InterruptedException {
        // pass 1: derive (type, slot) targets from paths; dedup by (type, slot)
        final ConcurrentMap<Path, Type> targets = new ConcurrentHashMap<>();
        AsyncUtils.visitPathsAsync(root, null, new IOConsumer<Path>() {
            @Override
            public void accept(Path path) throws IOException {
                if (path.getFileName().toString().equals("module-info.class")) {
                    return;
                }
                Path rel = root.relativize(path);
                int slot = slotOf(rel);
                if (slot != 0) {
                    rel = rel.subpath(3, rel.getNameCount());
                }
                String pathStr = internalPath(rel);
                if (pathStr.endsWith(".class")) {
                    pathStr = pathStr.substring(0, pathStr.length() - 6);
                    targets.put(path, Type.getObjectType(pathStr));
                }
            }
        }).get();

        // pass 2: parse + add each target
        AsyncUtils.forEachAsync(targets.keySet(), new IOConsumer<Path>() {
            @Override
            public void accept(Path path) throws IOException {
                int slot = slotOf(root.relativize(path));
                try (InputStream stream = Files.newInputStream(path)) {
                    ClassNode node = ASMUtils.bytesToClassNode(Utils.readAllBytes(stream));
                    Type type = Type.getObjectType(node.name);
                    if (!type.equals(targets.get(path))) {
                        throw new IllegalStateException("Expected path to match class name: " + path + " != " + type.getInternalName());
                    }
                    index.add(node, slot, filter);
                }
            }
        }).get();
    }

}
