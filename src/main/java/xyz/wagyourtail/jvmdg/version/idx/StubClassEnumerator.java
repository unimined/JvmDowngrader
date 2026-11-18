package xyz.wagyourtail.jvmdg.version.idx;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Gen-time classpath enumeration for the generated provider config — the
 * "scan the version package" half of {@link StubIndexWriter}'s emit path.
 * Walks every directory/jar on {@code java.class.path} for class files
 * under {@code xyz/wagyourtail/jvmdg/j<N>/} and classifies each without
 * loading it:
 * <ul>
 *   <li><b>stub class</b> — the class or any of its methods carries one of
 *   the {@link xyz.wagyourtail.jvmdg.version.StubScanner} marker
 *   annotations (RUNTIME-retained, so {@code visibleAnnotations} matches
 *   {@code getAnnotation} exactly)</li>
 *   <li><b>boundary transform</b> — directly implements {@code
 *   BoundaryTransform}</li>
 * </ul>
 * Nested marker classes are reported individually — registering each
 * directly via {@code stub()} is equivalent to {@code init()}'s old
 * anchor + {@code getDeclaredClasses()} recursion, minus the marker-less
 * anchor entries that contributed nothing.
 * <p>
 * The {@code j<N>} path segment is the provider tag ({@link
 * xyz.wagyourtail.jvmdg.version.VersionProvider#versionTag}); providers
 * never registered cross-package classes, so the tag mapping is
 * complete. Shared {@code jvmdg.stub.all} contributor classes live
 * outside {@code j<N>} and are intentionally not tagged — {@code preInit}
 * still owns them on both load paths.
 */
public class StubClassEnumerator {

    private static final String JVMDG_PREFIX = "xyz/wagyourtail/jvmdg/";
    private static final String TRANSFORM = "xyz/wagyourtail/jvmdg/version/BoundaryTransform";

    public static class Result {
        /** tag ({@code j<N>}) -&gt; FQCNs of marker-bearing stub classes */
        public final Map<String, Set<String>> stubClasses = new TreeMap<>();
        /** tag ({@code j<N>}) -&gt; FQCNs of BoundaryTransform impls */
        public final Map<String, Set<String>> transforms = new TreeMap<>();
    }

    public static Result enumerate(Set<String> markerDescriptors) throws IOException {
        Result result = new Result();
        String[] entries = System.getProperty("java.class.path", "").split(java.util.regex.Pattern.quote(File.pathSeparator));
        for (String entry : entries) {
            if (entry.isEmpty()) continue;
            Path path = Paths.get(entry);
            if (!Files.exists(path)) continue;
            if (Files.isDirectory(path)) {
                scanDir(path, markerDescriptors, result);
            } else if (entry.endsWith(".jar")) {
                scanJar(path, markerDescriptors, result);
            }
        }
        return result;
    }

    private static void scanDir(Path root, final Set<String> markerDescriptors, final Result result) throws IOException {
        Path jvmdg = root.resolve(JVMDG_PREFIX);
        if (!Files.isDirectory(jvmdg)) return;
        Files.walkFileTree(jvmdg, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class") && !file.toString().endsWith("module-info.class") && !file.toString().endsWith("package-info.class")) {
                    String internal = jvmdg.relativize(file).toString().replace(File.separatorChar, '/');
                    internal = JVMDG_PREFIX + internal.substring(0, internal.length() - ".class".length());
                    try (InputStream in = Files.newInputStream(file)) {
                        classify(internal, in, markerDescriptors, result);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void scanJar(Path jar, Set<String> markerDescriptors, Result result) throws IOException {
        try (JarFile jf = new JarFile(jar.toFile())) {
            for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();
                String name = entry.getName();
                if (!name.startsWith(JVMDG_PREFIX) || !name.endsWith(".class") ||
                    name.endsWith("module-info.class") || name.endsWith("package-info.class")) {
                    continue;
                }
                try (InputStream in = jf.getInputStream(entry)) {
                    classify(name.substring(0, name.length() - ".class".length()), in, markerDescriptors, result);
                }
            }
        }
    }

    private static void classify(String internalName, InputStream in, Set<String> markerDescriptors, Result result) throws IOException {
        String tag = tagOf(internalName);
        if (tag == null) return;
        ClassNode node = new ClassNode();
        new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        String fqcn = internalName.replace('/', '.');
        if (node.interfaces != null && node.interfaces.contains(TRANSFORM)) {
            result.transforms.computeIfAbsent(tag, k -> new TreeSet<String>()).add(fqcn);
            return;
        }
        if (hasMarker(node.visibleAnnotations, markerDescriptors)) {
            result.stubClasses.computeIfAbsent(tag, k -> new TreeSet<String>()).add(fqcn);
            return;
        }
        if (node.methods != null) {
            for (MethodNode method : node.methods) {
                if (hasMarker(method.visibleAnnotations, markerDescriptors)) {
                    result.stubClasses.computeIfAbsent(tag, k -> new TreeSet<String>()).add(fqcn);
                    return;
                }
            }
        }
    }

    /** {@code j<N>} for classes under {@code xyz/wagyourtail/jvmdg/j<N>/}; null elsewhere */
    private static String tagOf(String internalName) {
        String rel = internalName.substring(JVMDG_PREFIX.length());
        int slash = rel.indexOf('/');
        if (slash < 0) return null;
        String seg = rel.substring(0, slash);
        if (seg.length() < 2 || seg.charAt(0) != 'j') return null;
        for (int i = 1; i < seg.length(); i++) {
            if (!Character.isDigit(seg.charAt(i))) return null;
        }
        return seg;
    }

    private static boolean hasMarker(List<AnnotationNode> annotations, Set<String> markerDescriptors) {
        if (annotations == null) return false;
        for (AnnotationNode a : annotations) {
            if (markerDescriptors.contains(a.desc)) {
                return true;
            }
        }
        return false;
    }

}
