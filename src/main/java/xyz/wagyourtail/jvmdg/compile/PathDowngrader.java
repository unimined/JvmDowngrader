package xyz.wagyourtail.jvmdg.compile;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

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
        downgradePaths(ClassDowngrader.downgradeTo(opcVersion), inputRoots, outputRoots, classpathPaths);
    }

    public static void downgradePaths(final ClassDowngrader downgrader, final List<Path> inputRoots, List<Path> outputRoots, Set<URL> classpath) throws IOException {
        try (final URLClassLoader extraClasspath = new URLClassLoader(classpath.toArray(new URL[0]), PathDowngrader.class.getClassLoader())) {
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
                                Files.copy(path, outFile, StandardCopyOption.REPLACE_EXISTING);
                            } else {
                                try {
                                    String relativizedName = relativized.toString();
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
                                            Files.createDirectories(p.getParent());
                                            Files.write(p, entry.getValue(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                                        }
                                    }
                                } catch (IllegalClassFormatException e) {
                                    throw new IOException("Failed to downgrade " + path, e);
                                }
                            }
                        } else {
                            Files.copy(path, outFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }

                }).get();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
