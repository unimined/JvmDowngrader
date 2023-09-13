package xyz.wagyourtail.jvmdg.compile;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.util.Function;
import xyz.wagyourtail.jvmdg.util.Utils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipDowngrader {

    public static void main(String[] args) throws IOException {
        int target = Integer.parseInt(args[0]);
        File input = new File(args[1]);
        File output = new File(args[2]);
        Set<File> classpath = new HashSet<>();
        for (String s : args[3].split(File.pathSeparator)) {
            classpath.add(new File(s));
        }
        downgradeZip(target, input, classpath, output);
    }

    public static void downgradeZip(int opcVersion, File input, Set<File> classpath, File output) throws IOException {
        Set<URL> classpathPaths = new HashSet<>();
        for (File file : classpath) {
            classpathPaths.add(file.toURI().toURL());
        }
        downgradeZip(ClassDowngrader.downgradeTo(opcVersion), input.toPath(), classpathPaths, output.toPath());
    }

    public static void downgradeZip(int opcVersion, Path input, Set<URL> classpath, Path output) throws IOException {
        downgradeZip(ClassDowngrader.downgradeTo(opcVersion), input, classpath, output);
    }

    public static void downgradeZip(final ClassDowngrader downgrader, Path zip, Set<URL> classpath, final Path output) throws IOException {
        final URLClassLoader extraClasspath = new URLClassLoader(classpath.toArray(new URL[0]), ZipDowngrader.class.getClassLoader());
        try (final FileSystem zipfs = Utils.openZipFileSystem(zip, new HashMap<String, Object>())) {
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(output))) {
                Files.walkFileTree(zipfs.getPath("/"), new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (dir.startsWith("META-INF"))
                            return FileVisitResult.SKIP_SUBTREE;
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        byte[] bytes = Utils.readAllBytes(Files.newInputStream(file));
                        if (file.getFileName().toString().endsWith(".class")) {
                            try {
                                String internalName = file.toString().substring(1, file.toString().length() - 6);
                                Map<String, byte[]> outputs = downgrader.downgrade(new AtomicReference<>(internalName), bytes, new Function<String, byte[]>() {
                                    @Override
                                    public byte[] apply(String s) {
                                        try {
                                            Path path = zipfs.getPath(s + ".class");
                                            if (Files.exists(path)) {
                                                return Utils.readAllBytes(Files.newInputStream(path));
                                            } else {
                                                URL url = extraClasspath.getResource(s + ".class");
                                                if (url != null) {
                                                    return Utils.readAllBytes(url.openStream());
                                                }
                                            }
                                            return null;
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                                if (outputs == null || outputs.isEmpty()) {
                                    ZipEntry newEntry = new ZipEntry(internalName + ".class");
                                    newEntry.setTime(attrs.lastModifiedTime().toMillis());
                                    zos.putNextEntry(newEntry);
                                    zos.write(bytes);
                                    zos.flush();
                                    zos.closeEntry();
                                    return FileVisitResult.CONTINUE;
                                }
                                for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                                    ZipEntry newEntry = new ZipEntry(entry.getKey() + ".class");
                                    newEntry.setTime(attrs.lastModifiedTime().toMillis());
                                    zos.putNextEntry(newEntry);
                                    zos.write(entry.getValue());
                                    zos.flush();
                                    zos.closeEntry();
                                }
                            } catch (IllegalClassFormatException e) {
                                throw new IOException("Failed to downgrade class", e);
                            }
                        } else {
                            String fileName = file.getFileName().toString();
                            // remove leading /
                            if (fileName.startsWith("/")) {
                                fileName = fileName.substring(1);
                            }
                            ZipEntry newEntry = new ZipEntry(fileName);
                            newEntry.setTime(attrs.lastModifiedTime().toMillis());
                            zos.putNextEntry(newEntry);
                            zos.write(bytes);
                            zos.flush();
                            zos.closeEntry();
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        throw new IOException("Failed to visit file", exc);
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

}
