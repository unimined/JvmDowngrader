package xyz.wagyourtail.jvmdg.compile;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.classloader.ResourceClassLoader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.util.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
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
        try (final ResourceClassLoader extraClasspath = new ResourceClassLoader(classpath, PathDowngrader.class.getClassLoader())) {
            // zip input and output
            for (int i = 0; i < inputRoots.size(); i++) {
                final Path in = inputRoots.get(i);
                final Path out = outputRoots.get(i);
                AsyncUtils.visitPathsAsync(in, new IOFunction<Path, Boolean>() {

                    @Override
                    public Boolean apply(Path path) throws IOException {
                        Files.createDirectories(out.resolve(in.relativize(path).toString()));
                        return true;
                    }

                }, new IOConsumer<Path>() {

                    @Override
                    public void accept(Path path) throws IOException {
                        Path relativized = in.relativize(path);
                        Path outFile = out.resolve(relativized.toString());
                        if (path.getFileName().toString().endsWith(".class")) {
                            if (relativized.startsWith("META-INF/versions")) {
                                String version = relativized.getName(2).toString();
                                if (downgrader.flags.multiReleaseOriginal || downgrader.flags.multiReleaseVersions.contains(Utils.majorVersionToClassVersion(Integer.parseInt(version)))) {
                                    Files.copy(path, outFile, StandardCopyOption.REPLACE_EXISTING);
                                }
                            } else {
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
                                        Files.copy(path, outFile, StandardCopyOption.REPLACE_EXISTING);
                                    } else {
                                        for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                                            String internal = entry.getKey();
                                            Path p = out.resolve(internal + ".class");
                                            Path parent = p.getParent();
                                            if (parent != null) {
                                                Files.createDirectories(parent);
                                            }
                                            Files.write(p, entry.getValue(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                                        }
                                    }
                                } catch (IllegalClassFormatException e) {
                                    throw new IOException("Failed to downgrade " + path, e);
                                }
                            }
                        } else if (relativized.toString().equals("META-INF/MANIFEST.MF")) {
                            // add version number to manifest
                            try(InputStream is = Files.newInputStream(path)) {
                                Manifest manifest = new Manifest(is);
                                Attributes attr = manifest.getMainAttributes();
                                if (Flags.jvmdgVersion != null) {
                                    attr.putValue("JvmDowngrader-Version", Flags.jvmdgVersion);
                                }
                                if (downgrader.flags.multiReleaseOriginal || !downgrader.flags.multiReleaseVersions.isEmpty()) {
                                    attr.putValue("Multi-Release", "true");
                                }

                                try (OutputStream os = Files.newOutputStream(outFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                                    manifest.write(os);
                                }
                            }
                        } else {
                            // strip signatures since downgraded classes will not have the same signatures
                            if (relativized.toString().startsWith("META-INF/") && (path.toString().endsWith(".SF") || path.toString().endsWith(".DSA") || path.toString().endsWith(".RSA"))) {
                                return;
                            }
                            Files.copy(path, outFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }

                }).get();
            }
        } catch (ExecutionException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
