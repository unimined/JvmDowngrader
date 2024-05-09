package xyz.wagyourtail.jvmdg.compile;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class PathDowngrader {

    public static void main(String[] args) throws IOException {
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
    }

    public static void downgradePaths(int opcVersion, List<Path> input, List<Path> output, Set<File> classpath) throws IOException {
        Set<URL> classpathPaths = new HashSet<>();
        for (File file : classpath) {
            classpathPaths.add(file.toURI().toURL());
        }
        downgradePaths(ClassDowngrader.downgradeTo(opcVersion), input, output, classpathPaths);
    }

    public static void downgradePaths(final ClassDowngrader downgrader, final List<Path> input, List<Path> output, Set<URL> classpath) throws IOException {
        final URLClassLoader extraClasspath = new URLClassLoader(classpath.toArray(new URL[0]), PathDowngrader.class.getClassLoader());
        for (int i = 0; i < input.size(); i++) {
            final Path in = input.get(i);
            final Path out = output.get(i);
            Files.walkFileTree(in, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativized = in.relativize(file);
                    Path outFile = out.resolve(relativized);
                    Files.createDirectories(outFile.getParent());
                    if (file.getFileName().toString().endsWith(".class")) {
                        if (relativized.startsWith("META-INF/versions")) {
                            Files.copy(file, outFile, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            try {
                                String relativizedName = relativized.toString();
                                String internalName = relativizedName.substring(0, relativizedName.length() - 6);
                                byte[] bytes = Files.readAllBytes(file);
                                Map<String, byte[]> outputs = downgrader.downgrade(new AtomicReference<>(internalName), bytes, false, new Function<String, byte[]>() {
                                    @Override
                                    public byte[] apply(String s) {
                                        try {
                                            for (Path in : input) {
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
                                if (outputs == null || outputs.isEmpty()) {
                                    Files.copy(file, outFile, StandardCopyOption.REPLACE_EXISTING);
                                } else {
                                    for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                                        String internal = entry.getKey();
                                        Path p = out.resolve(internal + ".class");
                                        Files.createDirectories(p.getParent());
                                        Files.write(p, entry.getValue(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                                    }
                                }
                            } catch (IllegalClassFormatException e) {
                                throw new IOException("Failed to downgrade " + file, e);
                            }
                        }
                    } else {
                        Files.copy(file, outFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

}
