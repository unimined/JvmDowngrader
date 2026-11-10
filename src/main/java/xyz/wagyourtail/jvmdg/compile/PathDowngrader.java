package xyz.wagyourtail.jvmdg.compile;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.classloader.ResourceClassLoader;
import xyz.wagyourtail.jvmdg.util.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Manifest;

public class PathDowngrader {

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        int target = Integer.parseInt(args[0]);
        Set<Path> input = new HashSet<>();
        Set<Path> output = new HashSet<>();
        for (String s : args[1].split(File.pathSeparator)) {
            input.add(new File(s).toPath());
        }
        for (String s : args[2].split(File.pathSeparator)) {
            output.add(new File(s).toPath());
        }
        if (input.size() != output.size()) {
            throw new IllegalArgumentException("Input and output paths must be the same size");
        }
        Set<File> classpath = new HashSet<>();
        if (args.length > 3) {
            for (String s : args[3].split(File.pathSeparator)) {
                classpath.add(new File(s));
            }
        }
        downgradePaths(target, new ArrayList<>(input), new ArrayList<>(output), classpath);
        System.out.println("Downgraded in " + (System.currentTimeMillis() - start) + "ms");
    }

    public static void downgradePaths(int opcVersion, List<Path> inputRoots, List<Path> outputRoots, Set<File> classpath) throws IOException {
        Set<URL> classpathPaths = new HashSet<>();
        for (File file : classpath) {
            classpathPaths.add(file.toURI().toURL());
        }
        try (ClassDowngrader downgrader = ClassDowngrader.downgradeTo(opcVersion)) {
            downgradePaths(downgrader, inputRoots, outputRoots, classpathPaths);
        }
    }

    public static void downgradePaths(final ClassDowngrader downgrader, final List<Path> inputRoots, List<Path> outputRoots, Set<URL> classpath) throws IOException {
        try (final ResourceClassLoader extraClasspath = new ResourceClassLoader(classpath, PathDowngrader.class.getClassLoader()) {

            @Override
            protected int maxClassVersionSupported() {
                return downgrader.maxVersion();
            }

            @Override
            protected List<String> multiVersionPrefixes() {
                if (downgrader.flags.downgradeFromMultiReleases) {
                    return super.multiVersionPrefixes();
                } else {
                    return Collections.singletonList("");
                }
            }

        }) {
            // zip input and output
            for (int i = 0; i < inputRoots.size(); i++) {
                final Path in = inputRoots.get(i);
                final Path out = outputRoots.get(i);

                final List<String> multiReleasePaths = new ArrayList<>();
                for (int j = downgrader.maxVersion(); j >= 52; j--) {
                    multiReleasePaths.add("META-INF/versions/" + Utils.classVersionToMajorVersion(j));
                }
                final Set<Path> visitedClasses = Collections.newSetFromMap(new ConcurrentHashMap<Path, Boolean>());
                // deterministic output: async consumers defer into these maps,
                // then a single sorted write pass produces reproducible order
                final Set<String> mkdirs = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
                final ConcurrentMap<String, byte[]> writes = new ConcurrentHashMap<>();
                final ConcurrentMap<String, Path> copies = new ConcurrentHashMap<>();

                AsyncUtils.visitPathsAsync(in, new IOFunction<Path, Boolean>() {

                    @Override
                    public Boolean apply(Path path) throws IOException {
                        mkdirs.add(in.relativize(path).toString());
                        return true;
                    }

                }, new IOConsumer<Path>() {

                    @Override
                    public void accept(Path path) throws IOException {
                        Path relativized = in.relativize(path);
                        if (path.getFileName().toString().endsWith(".class")) {

                            if (relativized.startsWith("META-INF/versions")) {
                                if (downgrader.flags.downgradeFromMultiReleases) {
                                    relativized = relativized.subpath(3, relativized.getNameCount());
                                } else if (downgrader.flags.multiReleaseOriginal) {
                                    // copy original
                                    copies.put(relativized.toString(), path);
                                    return;
                                } else {
                                    return;
                                }
                            }

                            // dont if already visited
                            if (!visitedClasses.add(out.resolve(relativized.toString()))) {
                                return;
                            }

                            if (downgrader.flags.downgradeFromMultiReleases) {
                                // prefer highest multi-release
                                for (String pth : multiReleasePaths) {
                                    if (Files.exists(in.resolve(pth).resolve(relativized))) {
                                        path = in.resolve(pth).resolve(relativized);
                                        break;
                                    }
                                }
                            }

                            try {
                                String relativizedName = relativized.toString();
                                if (relativized.getFileSystem().getSeparator().equals("\\")) {
                                    relativizedName = relativizedName.replace('\\', '/');
                                }
                                String internalName = relativizedName.substring(0, relativizedName.length() - 6);
                                byte[] bytes = Files.readAllBytes(path);
                                Map<String, byte[]> outputs = downgrader.downgrade(new AtomicReference<>(internalName), bytes, false, new Function<String, byte[]>() {
                                    @Override
                                    public byte[] apply(String s) {
                                        try {

                                            // multi-version
                                            if (downgrader.flags.downgradeFromMultiReleases) {
                                                for (String pth : multiReleasePaths) {
                                                    for (Path in : inputRoots) {
                                                        Path p = in.resolve(pth).resolve(s + ".class");
                                                        if (Files.exists(p)) {
                                                            return Files.readAllBytes(p);
                                                        }
                                                    }
                                                }
                                            }

                                            // main version
                                            for (Path in : inputRoots) {
                                                Path p = in.resolve(s + ".class");
                                                if (Files.exists(p)) {
                                                    return Files.readAllBytes(p);
                                                }
                                            }

                                            URL url = extraClasspath.getResource(s + ".class");
                                            if (url == null) return null;
                                            try (InputStream is = url.openStream()) {
                                                return Utils.readAllBytes(is);
                                            }
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                                if (outputs == null) {
                                    copies.put(relativized.toString(), path);
                                } else {
                                    for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                                        writes.put(entry.getKey() + ".class", entry.getValue());
                                    }
                                }
                            } catch (IllegalClassFormatException e) {
                                throw new IOException("Failed to downgrade " + path, e);
                            }
                        } else if (ManifestUtils.MANIFEST_PATH.equals(
                                relativized.toString().replace('\\', '/'))) {
                            // add version number to manifest
                            try (InputStream is = Files.newInputStream(path)) {
                                Manifest manifest = new Manifest(is);
                                ManifestUtils.applyDowngradeAttributes(manifest, downgrader.flags);
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                manifest.write(bos);
                                writes.put(ManifestUtils.MANIFEST_PATH, bos.toByteArray());
                            }
                        } else {
                            // strip signatures since downgraded classes will not have the same signatures
                            if (ManifestUtils.isSignatureFile(relativized.toString().replace('\\', '/'))) {
                                return;
                            }
                            copies.put(relativized.toString(), path);
                        }
                    }

                }).get();

                // single sorted write pass: deterministic entry order
                List<String> dirs = new ArrayList<>(mkdirs);
                Collections.sort(dirs);
                for (String d : dirs) {
                    Files.createDirectories(out.resolve(d));
                }
                List<String> order = new ArrayList<>(writes.size() + copies.size());
                order.addAll(writes.keySet());
                order.addAll(copies.keySet());
                Collections.sort(order);
                for (String rel : order) {
                    Path p = out.resolve(rel);
                    Path parent = p.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    byte[] data = writes.get(rel);
                    if (data != null) {
                        Files.write(p, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    } else {
                        Files.copy(copies.get(rel), p, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                OutputUtils.pinTimestamps(out);

                // inputs without a manifest still need one when multi-release
                // output was requested, or the JVM ignores META-INF/versions
                if (ManifestUtils.needsMultiRelease(downgrader.flags)) {
                    Path mfPath = out.resolve(ManifestUtils.MANIFEST_PATH);
                    if (!Files.exists(mfPath)) {
                        Path parent = mfPath.getParent();
                        if (parent != null) {
                            Files.createDirectories(parent);
                        }
                        Manifest manifest = new Manifest();
                        ManifestUtils.applyDowngradeAttributes(manifest, downgrader.flags);
                        try (OutputStream os = Files.newOutputStream(mfPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                            manifest.write(os);
                        }
                        Files.setLastModifiedTime(mfPath, OutputUtils.FIXED_TIME);
                    }
                }
            }
        } catch (ExecutionException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
